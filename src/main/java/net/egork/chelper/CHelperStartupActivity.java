package net.egork.chelper;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class CHelperStartupActivity implements StartupActivity.DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        ChromeParser chromeParser = ServiceManager.getService(project, ChromeParser.class);
        chromeParser.startServer();
    }
}
