package net.egork.chelper.configurations;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import net.egork.chelper.ui.CHelperIcons;
import net.egork.chelper.util.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TaskConfigurationType implements ConfigurationType {
    private final ConfigurationFactory factory;
    public static TaskConfigurationType INSTANCE;

    public TaskConfigurationType() {
        factory = new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(Project project) {
                return new TaskConfiguration("Task", project, Utilities.getDefaultTask(), factory);
            }

            @NotNull
            @Override
            public String getId() {
                return "CHelperTaskFactory";
            }
        };
        INSTANCE = this;
    }

    public String getDisplayName() {
        return "Task";
    }

    public String getConfigurationTypeDescription() {
        return "CHelper Task";
    }

    public Icon getIcon() {
        return CHelperIcons.TASK;
    }

    @NotNull
    public String getId() {
        return "Task";
    }

    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{factory};
    }
}
