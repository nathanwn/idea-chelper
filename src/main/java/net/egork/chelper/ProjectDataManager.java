package net.egork.chelper;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import net.egork.chelper.codegeneration.CodeGenerationUtilities;
import net.egork.chelper.util.Utilities;

@Service(Service.Level.PROJECT)
public final class ProjectDataManager {
    private ProjectData configuration;

    ProjectDataManager(Project project) {
        this.configuration = ProjectData.load(project);
        if (configuration == null) return;
        DumbService.getInstance(project).smartInvokeLater(() -> {
            Utilities.ensureLibrary(project);
            CodeGenerationUtilities.createTaskClassTemplateIfNeeded(project, null);
            CodeGenerationUtilities.createCheckerClassTemplateIfNeeded(project);
            CodeGenerationUtilities.createTestCaseClassTemplateIfNeeded(project);
        });
    }

    public void setData(ProjectData configuration) {
        this.configuration = configuration;
    }

    public ProjectData getData() {
        return configuration;
    }
}
