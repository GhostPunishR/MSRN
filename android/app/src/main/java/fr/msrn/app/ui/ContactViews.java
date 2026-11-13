// Copyright (C) 2026 GhostPunishR and contributors
// SPDX-License-Identifier: AGPL-3.0-only

package fr.msrn.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import fr.msrn.app.R;
import org.json.JSONObject;

/** Native contact rows with one accessible action and an explicit presence label. */
public final class ContactViews {
    private ContactViews() { }

    public static View row(Context context, JSONObject user, String avatar, View.OnClickListener action) {
        String name = user.optString("display_name");
        String username = user.optString("username");
        String presence = user.optString("presence", "offline");
        String personal = user.optString("personal_message");
        int unread = Math.max(0, user.optInt("unread_count", 0));
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14));
        row.setMinimumHeight(dp(context, 84));
        row.setBackgroundResource(R.drawable.msrn_contact);
        row.setOnClickListener(action);
        row.setFocusable(true);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.bottomMargin = dp(context, 8);
        row.setLayoutParams(rowParams);

        int presenceColor = context.getColor("online".equals(presence) ? R.color.msrn_online
            : "away".equals(presence) ? R.color.msrn_away
            : "busy".equals(presence) ? R.color.msrn_busy : R.color.msrn_offline);
        TextView picture = text(context, avatar, 24, R.color.msrn_ink);
        picture.setGravity(Gravity.CENTER);
        GradientDrawable frame = new GradientDrawable();
        frame.setColor(context.getColor(R.color.msrn_surface_soft));
        frame.setCornerRadius(dp(context, 16));
        frame.setStroke(dp(context, 2), presenceColor);
        picture.setBackground(frame);
        picture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(picture, new LinearLayout.LayoutParams(dp(context, 52), dp(context, 52)));

        LinearLayout identity = new LinearLayout(context);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        LinearLayout.LayoutParams identityParams = new LinearLayout.LayoutParams(0, -2, 1);
        identityParams.setMarginStart(dp(context, 12));
        identityParams.setMarginEnd(dp(context, 8));
        TextView title = text(context, name, 16, R.color.msrn_ink);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        identity.addView(title);
        TextView state = text(context, "@" + username + " · " + ContactPresentation.presenceLabel(presence), 12, R.color.msrn_muted);
        state.setPadding(0, dp(context, 3), 0, 0);
        identity.addView(state);
        if (!personal.isEmpty()) {
            TextView message = text(context, personal, 13, R.color.msrn_muted);
            message.setPadding(0, dp(context, 4), 0, 0);
            identity.addView(message);
        }
        row.addView(identity, identityParams);

        if (unread > 0) {
            TextView badge = text(context, ContactPresentation.unreadLabel(unread), 12, R.color.msrn_on_primary);
            badge.setGravity(Gravity.CENTER);
            badge.setMinWidth(dp(context, 28));
            badge.setMinHeight(dp(context, 28));
            badge.setPadding(dp(context, 7), dp(context, 4), dp(context, 7), dp(context, 4));
            GradientDrawable fill = new GradientDrawable();
            fill.setColor(context.getColor(R.color.msrn_primary));
            fill.setCornerRadius(dp(context, 14));
            badge.setBackground(fill);
            badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(badge);
        }
        String description = name + ", @" + username + ", " + ContactPresentation.presenceLabel(presence)
            + (personal.isEmpty() ? "" : ", " + personal);
        if (unread > 0) description += ", " + context.getResources().getQuantityString(R.plurals.unread_messages, unread, unread);
        row.setContentDescription(description);
        return row;
    }

    private static TextView text(Context context, String value, float size, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(context.getColor(color));
        text.setMaxLines(1);
        text.setEllipsize(TextUtils.TruncateAt.END);
        return text;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
