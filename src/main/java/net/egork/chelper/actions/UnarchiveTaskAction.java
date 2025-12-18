package net.egork.chelper.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.codegeneration.CodeGenerationUtilities;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.CHelperIcons;
import net.egork.chelper.util.*;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class UnarchiveTaskAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        final Project project = Utilities.getProject(e.getDataContext());
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true, false, false, false, false, true
        )
            .withTitle("Select Task Files")
            .withDescription("Choose .task, or .json files")
            .withFileFilter(file ->
                                "task".equalsIgnoreCase(file.getExtension()) ||
                                "json".equalsIgnoreCase(file.getExtension()));
        VirtualFile archiveDir = FileUtilities.getFile(project, Utilities.getData(project).archiveDirectory);
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, archiveDir);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                try {
                    for (VirtualFile taskFile : files) {
                        if ("task".equals(taskFile.getExtension())) {
                            Task task = Task.loadTask(new InputReader(taskFile.getInputStream()));
                            if (unarchiveTask(taskFile, task, project)) {
                                return;
                            }
                        } else if ("json".equals(taskFile.getExtension())) {
                            Task task = TaskUtilities.mapper.readValue(FileUtilities.getInputStream(taskFile), Task.class);
                            if (unarchiveTask(taskFile, task, project)) {
                                return;
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private boolean unarchiveTask(VirtualFile taskFile, Task task, Project project) throws IOException {
        VirtualFile baseDirectory = FileUtilities.getFile(project, task.location);
        if (baseDirectory == null) {
            Messenger.publishMessage("Directory where task was located is no longer exists",
                    NotificationType.ERROR);
            return true;
        }
        FileUtilities.saveConfiguration(TaskUtilities.getTaskFileName(task.name), task, baseDirectory);
        List<String> toCopy = new ArrayList<String>();
        toCopy.add(task.taskClass);
        toCopy.add(task.checkerClass);
        Collections.addAll(toCopy, task.testClasses);
        String aPackage = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, task.location));
        if (aPackage == null || aPackage.isEmpty()) {
            int result = JOptionPane.showOptionDialog(null, "Task location is not under source or in default" +
                            "package, do you want to put it in default directory instead?", "Restore task",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    CHelperIcons.RESTORE, null, null);
            if (result == JOptionPane.YES_OPTION) {
                String defaultDirectory = Utilities.getData(project).defaultDirectory;
                baseDirectory = FileUtilities.getFile(project, defaultDirectory);
                aPackage = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, defaultDirectory));
                task = task.setLocation(defaultDirectory);
            }
        }
        for (String className : toCopy) {
            String fullClassName = className;
            int position = className.lastIndexOf('.');
            if (position != -1) {
                className = className.substring(position + 1);
            }
            VirtualFile file = taskFile.getParent().findChild(className + ".java");
            if (file != null) {
                String fileContent = FileUtilities.readTextFile(file);
                if (aPackage != null && !aPackage.isEmpty()) {
                    fileContent = CodeGenerationUtilities.changePackage(fileContent, aPackage);
                    String fqn = aPackage + "." + className;
                    if (task.taskClass.equals(fullClassName)) {
                        task = task.setTaskClass(fqn);
                    } else if (task.checkerClass.equals(fullClassName)) {
                        task = task.setCheckerClass(fqn);
                    } else {
                        for (int i = 0; i < task.testClasses.length; i++) {
                            if (task.testClasses[i].equals(fqn)) {
                                task.testClasses[i] = fqn;
                                break;
                            }
                        }
                    }
                }
                FileUtilities.writeTextFile(baseDirectory, className + ".java", fileContent);
            }
        }
        Utilities.createConfiguration(task, true, project);
        return false;
    }
}
