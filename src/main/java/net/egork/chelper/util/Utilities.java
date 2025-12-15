package net.egork.chelper.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cojac.CojacAgent;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.ProjectData;
import net.egork.chelper.ProjectDataManager;
import net.egork.chelper.actions.TopCoderAction;
import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.configurations.TopCoderConfigurationType;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.task.*;
import net.egork.chelper.tester.NewTester;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

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
    private static Parser defaultParser = Parser.PARSERS[0];

    public static void checkInstalled(Project project, ProjectData configuration) {
        if (!configuration.extensionProposed) {
            JPanel panel = new JPanel(new BorderLayout(15, 15));
            JLabel description = new JLabel("<html>You can now use new CHelper extension to parse<br>" +
                    "tasks directly from Google Chrome<br>(currently supported - Yandex.Contest, Codeforces and HackerRank)<br><br>Do you want to install it?</html>");
            JButton download = new JButton("Download");
            JButton close = new JButton("Close");
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.add(download, BorderLayout.WEST);
            buttonPanel.add(close, BorderLayout.EAST);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            panel.add(description, BorderLayout.CENTER);
            final JDialog dialog = new JDialog();
            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            download.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(new URL("https://chrome.google.com/webstore/detail/chelper-extension/eicjndbmlajfjdhephbcjdeegmmoadip").toURI());
                        } catch (IOException ignored) {
                        } catch (URISyntaxException ignored) {
                        }
                    }
                    dialog.setVisible(false);
                }
            });
            panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            dialog.setContentPane(panel);
            dialog.pack();
            Point center = Utilities.getLocation(project, panel.getSize());
            dialog.setLocation(center);
            dialog.setVisible(true);
            configuration.completeExtensionProposal(project);
        }
    }

    public static PsiElement getPsiElement(Project project, String classFQN) {
        return JavaPsiFacade.getInstance(project).findClass(classFQN, GlobalSearchScope.allScope(project));
    }

    public static void ensureLibrary(final Project project) {
        final ProjectData data = Utilities.getData(project);
        if (data.libraryVersion == ProjectData.CURRENT_LIBRARY_VERSION) {
            return;
        }
        fixLibrary(project);
        data.completeMigration(project);
    }

    public static void fixLibrary(Project project) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                Class[] neededClasses = {NewTester.class, CojacAgent.class, JsonCreator.class, ObjectMapper.class, com.fasterxml.jackson.core.JsonParser.class};
                LibraryTable table = ProjectLibraryTable.getInstance(project);
                Library library = table.getLibraryByName("CHelper");
                if (library == null) {
                    library = table.createLibrary("CHelper");
                }
                for (Class aClass : neededClasses) {
                    String path = TopCoderAction.getJarPathForClass(aClass);
                    VirtualFile jar = VirtualFileManager.getInstance().findFileByUrl(VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, path) + JarFileSystem.JAR_SEPARATOR);
                    Library.ModifiableModel libraryModel = library.getModifiableModel();
                    libraryModel.addRoot(jar, OrderRootType.CLASSES);
                    libraryModel.commit();
                }
                for (Module module : ModuleManager.getInstance(project).getModules()) {
                    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
                    if (model.findLibraryOrderEntry(library) == null) {
                        model.addLibraryEntry(library);
                        model.commit();
                    }
                }
            }
        });
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

    public static Parser getDefaultParser() {
        return defaultParser;
    }

    public static void setDefaultParser(Parser defaultParser) {
        Utilities.defaultParser = defaultParser;
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

    public static RunnerAndConfigurationSettings createConfiguration(TopCoderTask task, boolean setActive, Project project) {
        RunManager manager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings old = manager.findConfigurationByName(task.name);
        if (old != null) {
            manager.removeConfiguration(old);
        }
        ConfigurationFactory configurationFactory = TopCoderConfigurationType.INSTANCE.getConfigurationFactories()[0];
        RunnerAndConfigurationSettings settings = manager.createConfiguration(task.name, configurationFactory);
        RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof TopCoderConfiguration) {
            TopCoderConfiguration taskConfiguration = (TopCoderConfiguration) runConfiguration;
            taskConfiguration.setName(task.name);
            taskConfiguration.setConfiguration(task);
        } else {
            throw new IllegalStateException("Factory did not produce TopCoderConfiguration.");
        }

        manager.addConfiguration(settings);
        if (setActive) {
            manager.setSelectedConfiguration(settings);
        }
        return settings;
    }

    public static String getSimpleName(String className) {
        int position = className.lastIndexOf('.');
        if (position != -1) {
            className = className.substring(position + 1);
        }
        return className;
    }

    public static boolean isSupported(RunConfiguration configuration) {
        return configuration instanceof TaskConfiguration || configuration instanceof TopCoderConfiguration;
    }
}
