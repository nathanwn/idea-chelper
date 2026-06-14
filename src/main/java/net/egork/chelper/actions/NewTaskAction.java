package net.egork.chelper.actions;

import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.CreateTaskDialog;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class NewTaskAction extends CreateElementActionBase {
    @Override
    protected void invokeDialog(@NotNull Project project,
                                @NotNull PsiDirectory directory,
                                @NotNull Consumer<? super PsiElement[]> elementsConsumer) {
        elementsConsumer.accept(create("", directory));
    }

    @NotNull
    @Override
    protected PsiElement @NotNull [] create(@NotNull String s, @NotNull PsiDirectory psiDirectory) {
        return createTask(s, psiDirectory, null);
    }

    public static PsiElement[] createTask(String s, PsiDirectory psiDirectory, Task template) {
        if (!FileUtilities.isJavaDirectory(psiDirectory)) {
            return PsiElement.EMPTY_ARRAY;
        }
        Task task = CreateTaskDialog.showDialog(psiDirectory, s, template, true);
        if (task == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        PsiElement main = Utilities.getPsiElement(psiDirectory.getProject(), task.taskClass);
        if (main == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        Utilities.createConfiguration(task, true, psiDirectory.getProject());
        return new PsiElement[]{main};
    }

    @Override
    protected String getErrorTitle() {
        return "Error";
    }

    @Override
    protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
        return "New task " + newName;
    }

    @Override
    protected boolean isAvailable(DataContext dataContext) {
        PsiDirectory directory = FileUtilities.getDirectory(dataContext);
        return FileUtilities.isJavaDirectory(directory);
    }
}
