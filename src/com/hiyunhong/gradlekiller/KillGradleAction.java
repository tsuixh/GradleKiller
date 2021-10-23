package com.hiyunhong.gradlekiller;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kill gradle daemon process action. Current implementation only supports Windows.
 *
 * @author tsuixh
 * @since 2021-10-23
 */
public class KillGradleAction extends AnAction {
    /**
     * Process id on windows is number format.
     */
    private final Pattern mProcessIdPattern = Pattern.compile("\\d+");

    /**
     * Kill Gradle notification group.
     */
    private final NotificationGroup mNotificationGroup = new NotificationGroup("GradleKiller", NotificationDisplayType.BALLOON, true);

    @Override
    public void actionPerformed(@NonNull AnActionEvent event) {
        List<String> gradleProcessIds;
        try {
            gradleProcessIds = queryGradleProcess();
            boolean isKillSuccess = true;
            for (String pid : gradleProcessIds) {
                isKillSuccess &= killProcess(pid);
            }
            if (isKillSuccess) {
                showNotification("Kill gradle success.", NotificationType.INFORMATION);
            } else {
                showNotification("Kill gradle may failed.", NotificationType.WARNING);
            }
        } catch (IOException | InterruptedException e) {
            showNotification("Kill gradle error due to exception.", NotificationType.ERROR);
        }
    }

    /**
     * Show balloon messages.
     *
     * @param message message content.
     * @param notificationType notification type, such as warn, error etc.
     */
    private void showNotification(String message, NotificationType notificationType) {
        Notification notification = mNotificationGroup.createNotification(message, notificationType);
        Notifications.Bus.notify(notification);
    }

    /**
     * Force kill processes by pid.
     *
     * @param pid process id
     * @return true if process being killed, false if exception is throed.
     */
    private boolean killProcess(@NonNull String pid) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec("taskkill /F /PID " + pid);
            process.waitFor();
            return true;
        } catch (IOException| InterruptedException e) {
            return false;
        }
    }

    /**
     * Query gradle daemon process ids on windows.
     *
     * @return gradle process ids
     * @throws IOException IOException throws by Runtime#exec(String command)
     * @throws InterruptedException InterruptedException throws by Process#waitFor()
     */
    private List<String> queryGradleProcess() throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec("wmic process where \"commandline like '%gradle-launcher%' and name like '%java%'\" get processid");
        process.waitFor();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        List<String> result = new ArrayList<>();
        while ((line = bufferedReader.readLine()) != null) {
            String trim = line.trim();
            if (trim.length() == 0) {
                continue;
            }
            Matcher matcher = mProcessIdPattern.matcher(trim);
            if (matcher.matches()) {
                result.add(trim);
            }
        }
        return result;
    }
}
