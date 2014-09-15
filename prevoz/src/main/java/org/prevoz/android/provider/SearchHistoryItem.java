package org.prevoz.android.provider;

import android.net.Uri;

import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.DatetimeColumn;
import edu.mit.mobile.android.content.column.TextColumn;


public class SearchHistoryItem implements ContentItem
{
    public static final String PATH = "search/history";
    public static final Uri CONTENT_URI = ProviderUtils.toContentUri(DataProvider.AUTHORITY, PATH);

    @DBColumn(type = TextColumn.class, notnull = false)
    public static final String FROM_CITY = "l_from";
    @DBColumn(type = TextColumn.class, notnull = false)
    public static final String FROM_COUNTRY = "c_from";
    @DBColumn(type = TextColumn.class, notnull = false)
    public static final String TO_CITY = "l_to";
    @DBColumn(type = TextColumn.class, notnull = false)
    public static final String TO_COUNTRY = "c_to";
    @DBColumn(type = DatetimeColumn.class, notnull = true)
    public static final String DATE = "date";
}
