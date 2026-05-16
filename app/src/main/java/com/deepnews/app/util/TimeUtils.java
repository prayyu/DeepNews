package com.deepnews.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** 时间格式化工具 */
public class TimeUtils {
    public static String getRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(isoTime);
            if (date == null) return "";
            long diff = System.currentTimeMillis() - date.getTime();
            if (diff < 0) return "刚刚";

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes < 1) return "刚刚";
            if (minutes < 60) return minutes + " 分钟前";

            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            if (hours < 24) return hours + " 小时前";

            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days < 7) return days + " 天前";

            SimpleDateFormat dateFmt = new SimpleDateFormat("MM-dd", Locale.getDefault());
            return dateFmt.format(date);
        } catch (Exception e) {
            return "";
        }
    }
}
