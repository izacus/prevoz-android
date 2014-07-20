package org.prevoz.android.provider;

import android.net.Uri;
import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.DatetimeColumn;
import edu.mit.mobile.android.content.column.TextColumn;

public class Notification implements ContentItem
{
    public static final String PATH = "notifications/subscription";
    public static final Uri CONTENT_URI = ProviderUtils.toContentUri(DataProvider.AUTHORITY, PATH);

    @DBColumn(type = TextColumn.class, notnull = true)
    public static final String FROM_CITY = "l_from";
    @DBColumn(type = TextColumn.class, notnull = true)
    public static final String FROM_COUNTRY = "c_from";
    @DBColumn(type = TextColumn.class, notnull = true)
    public static final String TO_CITY = "l_to";
    @DBColumn(type = TextColumn.class, notnull = true)
    public static final String TO_COUNTRY = "c_to";
    @DBColumn(type = DatetimeColumn.class, notnull = true)
    public static final String DATE = "date";
    @DBColumn(type = DatetimeColumn.class, notnull = true, defaultValue = DatetimeColumn.NOW_IN_MILLISECONDS)
    public static final String REGISTERED_DATE = "reg_date";
}