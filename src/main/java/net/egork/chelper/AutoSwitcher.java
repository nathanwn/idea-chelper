package net.egork.chelper;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.FileUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
@Service(Service.Level.PROJECT)
public final class AutoSwitcher implements Disposable {
    private final Project project;
    private volatile boolean busy;

    public AutoSwitcher(Project project) {
        this.project = project;

        addSelectedConfigurationListener();
        addFileEditorListeners();
    }

    private void addFileEditorListeners() {
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileOpened(FileEditorManager source, VirtualFile file) {
                selectTask(file);
            }

            private void selectTask(final VirtualFile file) {
                Runnable selectTaskRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (busy || file == null) {
                            return;
                        }
                        final RunManager runManager = RunManager.getInstance(project);
                        List<RunnerAndConfigurationSettings> allSettings = RunManager
                                .getInstance(project).getAllSettings();
                        for (RunnerAndConfigurationSettings settings : allSettings) {
                            @NotNull RunConfiguration configuration = settings.getConfiguration();
                            if (configuration instanceof TaskConfiguration) {
                                Task task = ((TaskConfiguration) configuration).getConfiguration();
                                if (task != null && file.equals(FileUtilities.getFileByFQN(task.taskClass, configuration.getProject()))) {
                                    busy = true;
                                    runManager.setSelectedConfiguration(settings);
                                    busy = false;
                                    return;
                                }
                            }
                        }

                    }
                };

                DumbService.getInstance(project).smartInvokeLater(selectTaskRunnable);
            }

            @Override
            public void selectionChanged(FileEditorManagerEvent event) {
                selectTask(event.getNewFile());
            }
        });
    }

    private void addSelectedConfigurationListener() {
        MessageBusConnection connection = project.getMessageBus().connect(this);
        connection.subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
            @Override
            public void runConfigurationSelected(@Nullable RunnerAndConfigurationSettings settings) {
                if (settings == null) {
                    return;
                }
                RunConfiguration configuration = settings.getConfiguration();
                if (busy || !(configuration instanceof TaskConfiguration)) {
                    return;
                }
                busy = true;
                VirtualFile toOpen = FileUtilities
                        .getFileByFQN(((TaskConfiguration) configuration).getConfiguration().taskClass, configuration.getProject());
                if (toOpen != null) {
                    final VirtualFile finalToOpen = toOpen;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        FileEditorManager.getInstance(project).openFile(finalToOpen, true);
                    }, project.getDisposed());
                }
                busy = false;
            }
        });
    }

    @Override
    public void dispose() {
        // pass
    }
}
