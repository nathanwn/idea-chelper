package net.egork.chelper.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PathUtil;
import net.egork.chelper.ProjectData;
import net.egork.chelper.ProjectDataManager;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.tester.NewTester;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Utilities {
    // TODO: The existence of non-persistent defaultConfiguration together with persistent ProjectData is a bit weird.
    // It would be natural for everything to be persistent.
    private static Task defaultConfiguration = new Task(null, TestType.SINGLE, StreamConfiguration.STANDARD,
            StreamConfiguration.STANDARD, new Test[0], null, "-Xmx256m -Xss64m", "Main", null,
            TokenChecker.class.getCanonicalName(), "", new String[0], null, "", true, null, null, false, false,
            "TaskClass.template");

    public static PsiElement getPsiElement(Project project, String classFQN) {
        return JavaPsiFacade.getInstance(project).findClass(classFQN, GlobalSearchScope.allScope(project));
    }

    public static void ensureLibrary(final Project project) {
        final ProjectData data = Utilities.getData(project);
        if (data.libraryVersion == ProjectData.CURRENT_LIBRARY_VERSION) {
            return;
        }
        data.completeMigration(project);
    }

    public static List<String> getTesterRequiredJarPaths() {
        return Stream.of(
            NewTester.class,
            JsonCreator.class,
            ObjectMapper.class,
            com.fasterxml.jackson.core.JsonParser.class
        )
                .map((Class<?> cls) -> PathUtil.getJarPathForClass(cls))
                .collect(Collectors.toList());
    }

    public static boolean isEligible(DataContext dataContext) {
        // NOTE: This method used to be part of the ApplicationComponent implementation,
        // which has been removed. Now it's just a no-op.
        // TODO: Remove this method once it's confirmed that everything still works as expected.
        return true;
    }

    public static Project getProject(DataContext dataContext) {
        return PlatformDataKeys.PROJECT.getData(dataContext);
    }

    public static void updateDefaultTask(Task task) {
        if (task != null) {
            defaultConfiguration = new Task(null, task.testType, task.input, task.output, new Test[0], null,
                    task.vmArgs, task.mainClass, null, TokenChecker.class.getCanonicalName(), "", new String[0], null,
                    task.contestName, task.truncate, null, null, task.includeLocale, task.failOnOverflow, task.template);
        }
    }

    public static Task getDefaultTask() {
        return defaultConfiguration;
    }

    public static ProjectData getData(Project project) {
        return project.getService(ProjectDataManager.class).getData();
    }

    public static void openElement(Project project, PsiElement element) {
        if (element instanceof PsiFile) {
            VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
            if (virtualFile == null) {
                return;
            }
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        } else if (element instanceof PsiClass) {
            FileEditorManager.getInstance(project).openFile(FileUtilities.getFile(project,
                    getData(project).defaultDirectory + "/" + ((PsiClass) element).getName() + ".java"), true);
        }
    }

    public static Point getLocation(Project project, Dimension size) {
        JComponent component = WindowManager.getInstance().getIdeFrame(project).getComponent();
        Point center = component.getLocationOnScreen();
        center.x += component.getWidth() / 2;
        center.y += component.getHeight() / 2;
        center.x -= size.getWidth() / 2;
        center.y -= size.getHeight() / 2;
        return center;
    }

    public static RunnerAndConfigurationSettings createConfiguration(Task task, boolean setActive, Project project) {
        RunManager manager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings old = manager.findConfigurationByName(task.name);
        if (old != null) {
            manager.removeConfiguration(old);
        }
        ConfigurationFactory configurationFactory = TaskConfigurationType.INSTANCE.getConfigurationFactories()[0];
        RunnerAndConfigurationSettings settings = manager.createConfiguration(task.name, configurationFactory);
        RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof TaskConfiguration) {
            TaskConfiguration taskConfiguration = (TaskConfiguration) runConfiguration;
            taskConfiguration.setName(task.name);
            taskConfiguration.setConfiguration(task);
        } else {
            throw new IllegalStateException("Factory did not produce TaskConfiguration.");
        }

        manager.addConfiguration(settings);
        if (setActive) {
            manager.setSelectedConfiguration(settings);
        }
        return settings;
    }

    public static void addProjectData(Project project, ProjectData data) {
        ProjectDataManager projectDataManager = project.getService(ProjectDataManager.class);
        projectDataManager.setData(data);
    }

    public static Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

    public static String getSimpleName(String className) {
        int position = className.lastIndexOf('.');
        if (position != -1) {
            className = className.substring(position + 1);
        }
        return className;
    }
}
