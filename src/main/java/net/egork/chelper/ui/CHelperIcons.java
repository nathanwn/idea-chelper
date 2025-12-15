package net.egork.chelper.ui;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public class CHelperIcons {
    public static final Icon RESTORE = loadIcon("/icons/restore.png");
    public static final Icon TASK = loadIcon("/icons/taskIcon.png");
    public static final Icon TOPCODER = loadIcon("/icons/topcoder.png");
    public static final Icon CODE_CHEF = loadIcon("/icons/codechef.png");
    public static final Icon CODEFORCES = loadIcon("/icons/codeforces.png");
    public static final Icon GCJ = loadIcon("/icons/gcj.png");
    public static final Icon KATTIS = loadIcon("/icons/kattis.png");
    public static final Icon RCC = loadIcon("/icons/rcc.png");
    public static final Icon TIMUS = loadIcon("/icons/timus.png");
    public static final Icon EDIT_TESTS = loadIcon("/icons/editTests.png");
    public static final Icon NEW_TASKS = loadIcon("/icons/newTask.png");
    public static final Icon PARSE_CONTEST = loadIcon("/icons/parseContest.png");
    public static final Icon CHECK = loadIcon("/icons/check.png");

    private static Icon loadIcon(String path) {
        return IconLoader.getIcon(path, CHelperIcons.class);
    }
}
