package com.example.myapplication.util;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

public class BadgeUtils {

    private static final int[] COLORS = {
            0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFB8C00,
            0xFF8E24AA, 0xFF00ACC1, 0xFF6D4C41, 0xFF546E7A,
            0xFFD81B60, 0xFF3949AB, 0xFF7CB342, 0xFFF4511E
    };

    public static int getColor(String key) {
        if (key == null || key.isEmpty()) return COLORS[0];
        return COLORS[Math.abs(key.hashCode()) % COLORS.length];
    }

    public static void setBadge(TextView badgeView, String text) {
        String initial = text != null && !text.isEmpty()
                ? text.substring(0, Math.min(1, text.length()))
                : "?";
        badgeView.setText(initial);

        int color = getColor(text);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        bg.setSize(badgeView.getLayoutParams().width,
                badgeView.getLayoutParams().height);
        badgeView.setBackground(bg);
        badgeView.setVisibility(View.VISIBLE);
    }
}
