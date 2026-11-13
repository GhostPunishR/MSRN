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

/** Creates safe text-only message bubbles without modifying stored messages. */
public final class MessageBubbles {
    private MessageBubbles() { }

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
        String time = timestamp.length() >= 16 ? timestamp.substring(11, 16) : "";
        detail.setText((mine ? "Vous" : "Contact") + (time.isEmpty() ? "" : " · " + time));
        detail.setTextColor(Color.rgb(98, 112, 131));
        detail.setTextSize(12f);
        detail.setPadding(dp(context, 5), dp(context, 3), dp(context, 5), 0);
        row.addView(detail);
        history.addView(row, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }
}
