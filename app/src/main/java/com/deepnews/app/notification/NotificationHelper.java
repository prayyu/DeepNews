package com.deepnews.app.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.deepnews.app.MainActivity;
import com.deepnews.app.R;
import com.deepnews.app.util.Logger;

import java.util.List;

public class NotificationHelper {

    private static final Logger log = Logger.get(NotificationHelper.class);
    private static final String CHANNEL_ID = "news_keyword";
    private static final String CHANNEL_NAME = "关键词订阅";
    private static final String CHANNEL_DESC = "当新闻匹配订阅关键词时推送通知";
    private static final int NOTIFICATION_ID = 1001;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                log.d("通知渠道已创建");
            }
        }
    }

    public static void showKeywordMatchNotification(Context context, List<String> matchedTitles, int keywordCount) {
        createChannel(context);

        int matchCount = matchedTitles.size();
        String title = "发现 " + matchCount + " 条匹配新闻";
        String content = "共 " + keywordCount + " 个订阅关键词，最新: " + matchedTitles.get(0);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (matchCount > 1) {
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            for (int i = 0; i < Math.min(matchCount, 5); i++) {
                style.addLine(matchedTitles.get(i));
            }
            if (matchCount > 5) {
                style.setSummaryText("还有 " + (matchCount - 5) + " 条匹配");
            }
            builder.setStyle(style);
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        log.d("已推送关键词通知: " + title);
    }
}
