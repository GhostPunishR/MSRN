// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.chat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;

/** Creates safe text-only message bubbles without modifying stored messages. */
public final class MessageBubbles {
    private MessageBubbles() { }

    private static final DateTimeFormatter SERVER_TIME = new DateTimeFormatterBuilder()
        .appendPattern("uuuu-MM-dd HH:mm:ss")
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .appendOffset("+HH:mm", "Z")
        .toFormatter(Locale.ROOT);
    private static final DateTimeFormatter LOCAL_TIME =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    public static ZonedDateTime localDateTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return null;
        try {
            String normalized = timestamp.replace('T', ' ');
            normalized = normalized.replaceFirst("([+-]\\\d{2})$", "$1:00");
            normalized = normalized.replaceFirst("([+-]\\\d{2})(\\\d{2})$", "$1:$2");
            return OffsetDateTime.parse(normalized, SERVER_TIME)
                .atZoneSameInstant(ZoneId.systemDefault());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static String localDateKey(String timestamp) {
        ZonedDateTime date = localDateTime(timestamp);
        return date == null ? "" : date.toLocalDate().toString();
    }

    public static String dateLabel(String timestamp) {
        ZonedDateTime date = localDateTime(timestamp);
        if (date == null) return "";
        LocalDate day = date.toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (day.equals(today)) return "Aujourd'hui";
        if (day.equals(today.minusDays(1))) return "Hier";
        return day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale.getDefault()));
    }

    public static void addDateSeparator(LinearLayout history, String label) {
        if (label == null || label.isEmpty()) return;
        Context context = history.getContext();
        TextView separator = new TextView(context);
        separator.setText(label);
        separator.setTextSize(13f);
        separator.setTextColor(Color.rgb(98, 112, 131));
        separator.setGravity(Gravity.CENTER);
        separator.setPadding(dp(context, 8), dp(context, 16), dp(context, 8), dp(context, 12));
        history.addView(separator, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    public static String localTime(String timestamp) {
        ZonedDateTime date = localDateTime(timestamp);
        return date == null ? "" : date.format(LOCAL_TIME);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static void add(LinearLayout history, JSONObject message, String myUserId) {
        Context context = history.getContext();
        boolean mine = myUserId != null && !myUserId.isEmpty()
            && myUserId.equals(message.optString("sender_id"));
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(mine ? Gravity.END : Gravity.START);
        row.setPadding(dp(context, 4), dp(context, 5), dp(context, 4), dp(context, 5));

        TextView bubble = new TextView(context);
        bubble.setText(EmojiSupport.display(message.optString("body")));
        bubble.setTextSize(16f);
        bubble.setTextColor(Color.rgb(25, 40, 61));
        bubble.setPadding(dp(context, 12), dp(context, 9), dp(context, 12), dp(context, 9));
        bubble.setMaxWidth((int) (context.getResources().getDisplayMetrics().widthPixels * 0.78f));
        GradientDrawable background = new GradientDrawable();
        background.setColor(mine ? Color.rgb(213, 237, 255) : Color.rgb(237, 240, 246));
        background.setCornerRadius(dp(context, 15));
        bubble.setBackground(background);
        row.addView(bubble, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView detail = new TextView(context);
        String timestamp = message.optString("created_at");
        String time = localTime(timestamp);
        detail.setText((mine ? "Vous" : "Contact") + (time.isEmpty() ? "" : " · " + time));
        detail.setTextColor(Color.rgb(98, 112, 131));
        detail.setTextSize(12f);
        detail.setPadding(dp(context, 5), dp(context, 3), dp(context, 5), 0);
        row.addView(detail);
        history.addView(row, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }
}
