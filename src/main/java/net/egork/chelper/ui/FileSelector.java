package net.egork.chelper.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import net.egork.chelper.util.FileUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class FileSelector extends JPanel {
    private final JTextField textField;
    private final JButton button;

    /**
     * Creates a UI component for selecting a file.
     * @param project      The current IntelliJ project context, used to determine
     *                     the base directory.
     * @param initialValue The initial path to display in the text field.
     *                     This should be a relative path from the project's base directory.
     * @param extension    The file extension of the selected file.
     */
    public FileSelector(final Project project, String initialValue, String extension) {
        this(project, initialValue, extension, false);
    }

    /**
     * Creates a UI component for selecting a file.
     * @param project                      The current IntelliJ project context, used to determine
     *                                     the base directory.
     * @param initialValue                 The initial path to display in the text field.
     *                                     This should be a relative path from the project's base directory.
     * @param extension                    The file extension of the selected file.
     * @param allowOutsideProjectWorkspace If true, users are allowed to select a file outside the
     *                                     project workspace.
     */
    public FileSelector(final Project project, String initialValue, String extension,
                        boolean allowOutsideProjectWorkspace) {
        super(new BorderLayout());
        textField = new JTextField(initialValue);
        button = new JButton("...") {
            @Override
            public Dimension getPreferredSize() {
                Dimension dimension = super.getPreferredSize();
                //noinspection SuspiciousNameCombination
                dimension.width = dimension.height;
                return dimension;
            }
        };
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                VirtualFile baseDir = FileUtilities.getBaseDir(project);

                // Only allow selecting a file.
                // If allowAllFiles is false, restrict the selected file to be under baseDir.
                boolean chooseFiles = true;
                boolean chooseFolders = false;
                boolean chooseJars = false;
                boolean chooseJarsAsFiles = false;
                boolean chooseJarsContents = false;
                boolean chooseMultiple = false;
                FileChooserDescriptor descriptor = new FileChooserDescriptor(
                        chooseFiles, chooseFolders, chooseJars, chooseJarsAsFiles, chooseJarsContents, chooseMultiple)
                        .withFileFilter(file -> extension.equals(file.getExtension()) &&
                                (allowOutsideProjectWorkspace || FileUtilities.isChild(baseDir, file)));
                if (!allowOutsideProjectWorkspace) {
                    descriptor.withRoots(baseDir);
                }

                PathChooserDialog dialog = FileChooserFactory.getInstance()
                        .createPathChooser(descriptor, project, FileSelector.this);
                VirtualFile toSelect = allowOutsideProjectWorkspace
                        ? VfsUtil.findFileByIoFile(new File(textField.getText()), false)
                        : baseDir.findFileByRelativePath(textField.getText());
                if (toSelect == null) {
                    toSelect = baseDir;
                }
                dialog.choose(toSelect, new Consumer<List<VirtualFile>>() {
                    public void consume(List<VirtualFile> virtualFiles) {
                        if (virtualFiles.size() == 1) {
                            String path = allowOutsideProjectWorkspace
                                    ? virtualFiles.get(0).getPath()
                                    : FileUtilities.getRelativePath(baseDir, virtualFiles.get(0));
                            if (path != null) {
                                textField.setText(path);
                            }
                        }
                    }
                });
            }
        });
        add(textField, BorderLayout.CENTER);
        add(button, BorderLayout.EAST);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    public String getText() {
        return textField.getText();
    }
}
