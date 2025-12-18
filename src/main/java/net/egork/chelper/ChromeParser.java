package net.egork.chelper;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import net.egork.chelper.actions.NewTaskDefaultAction;
import net.egork.chelper.parser.*;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.Messenger;
import net.egork.chelper.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;

@Service(Service.Level.PROJECT)
public final class ChromeParser implements Disposable {
    private static final int PORT = 4243;

    private final Project project;
    ServerSocket serverSocket;

    public ChromeParser(Project project) {
        this.project = project;
    }

    public void startServer() {
        if (ProjectData.load(project) == null) {
            return;
        }
        try {
            serverSocket = new ServerSocket(PORT);
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            if (serverSocket.isClosed()) {
                                return;
                            }
                            try (Socket socket = serverSocket.accept()) {
                                BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                                // Read header
                                while (true) {
                                    String line = reader.readLine();
                                    if (line.isEmpty()) break;
                                }

                                final String type = reader.readLine();

                                // Read body
                                StringBuilder builder = new StringBuilder();
                                while (true) {
                                    String line = reader.readLine();
                                    if (line == null) break;
                                    builder.append(line).append('\n');
                                }
                                final String body = builder.toString();
                                TransactionGuard.getInstance().submitTransactionAndWait(new Runnable() {
                                    public void run() {
                                        if (type.equals("json")) {
                                            System.err.println(body);
                                            JSONParser parser = new JSONParser();
                                            Collection<Task> tasks = parser.parseTaskFromHTML(body);
                                            if (tasks.isEmpty()) {
                                                Messenger.publishMessage("Unable to parse task from " + type, NotificationType.WARNING);
                                                return;
                                            }
                                            JFrame projectFrame = WindowManager.getInstance().getFrame(project);
                                            if (projectFrame != null && projectFrame.getState() == JFrame.ICONIFIED) {
                                                projectFrame.setState(Frame.NORMAL);
                                            }
                                            for (Task task : tasks) {
                                                task = task.setTemplate(Utilities.getDefaultTask().template);
                                                NewTaskDefaultAction.createTaskInDefaultDirectory(project, task);
                                            }
                                        } else {
                                            Messenger.publishMessage("Unknown task type from Chrome parser: " + type,
                                                    NotificationType.WARNING);
                                            System.err.println(body);
                                        }
                                    }
                                });
                            }
                        } catch (ProcessCanceledException e) {
                            throw e;
                        } catch (Throwable ignored) {}
                    }
                }
            }).start();
        } catch (IOException e) {
            Messenger.publishMessage("Could not create serverSocket for Chrome parser, probably another CHelper-" +
                    "eligible project is running?", NotificationType.ERROR);
        }
    }

    @Override
    public void dispose() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
