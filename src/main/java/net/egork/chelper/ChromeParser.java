package net.egork.chelper;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.TransactionGuard;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;

@Service(Service.Level.PROJECT)
public final class ChromeParser implements Disposable {
    private static final int PORT = 4243;
    private static final Map<String, Parser> TASK_PARSERS;

    static {
        Map<String, Parser> taskParsers = new HashMap<String, Parser>();
        taskParsers.put("json", new JSONParser());
        TASK_PARSERS = Collections.unmodifiableMap(taskParsers);
    }

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
                            Socket socket = serverSocket.accept();
                            try {
                                BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(socket.getInputStream(), "UTF-8"));
                                while (!reader.readLine().isEmpty()) ;
                                final String type = reader.readLine();
                                StringBuilder builder = new StringBuilder();
                                String s;
                                while ((s = reader.readLine()) != null)
                                    builder.append(s).append('\n');
                                final String page = builder.toString();
                                TransactionGuard.getInstance().submitTransactionAndWait(new Runnable() {
                                    public void run() {
                                        if (TASK_PARSERS.containsKey(type)) {
                                            System.err.println(page);
                                            Collection<Task> tasks = TASK_PARSERS.get(type).parseTaskFromHTML(page);
                                            if (tasks.isEmpty()) {
                                                Messenger.publishMessage("Unable to parse task from " + type, NotificationType.WARNING);
                                                return;
                                            }
                                            JFrame projectFrame = WindowManager.getInstance().getFrame(project);
                                            if (projectFrame.getState() == JFrame.ICONIFIED) {
                                                projectFrame.setState(Frame.NORMAL);
                                            }
                                            for (Task task : tasks) {
                                                task = task.setTemplate(Utilities.getDefaultTask().template);
                                                NewTaskDefaultAction.createTaskInDefaultDirectory(project, task);
                                            }
                                        } else {
                                            Messenger.publishMessage("Unknown task type from Chrome parser: " + type,
                                                    NotificationType.WARNING);
                                            System.err.println(page);
                                        }
                                    }
                                });
                            } finally {
                                socket.close();
                            }
                        } catch (Throwable ignored) {
                        }
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
            } catch (IOException e) {}
        }
    }
}
