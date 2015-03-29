package org.prevoz.android.model;

import android.support.annotation.Nullable;

public enum  Bookmark {
    GOING_WITH,
    OUT_OF_SEATS,
    NOT_GOING_WITH,
    BOOKMARK;

    public static boolean shouldShow(@Nullable Bookmark bookmark) {
        return bookmark != null && (bookmark == GOING_WITH || bookmark == BOOKMARK);
    }
}
