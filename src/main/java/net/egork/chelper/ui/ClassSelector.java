package net.egork.chelper.ui;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JavaReferenceEditorUtil;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class ClassSelector extends EditorTextField {
    public ClassSelector(String text, Project project) {
        super(JavaReferenceEditorUtil.createDocument(text, project, true), project, JavaFileType.INSTANCE);
    }
}
