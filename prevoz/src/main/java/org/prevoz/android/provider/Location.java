package org.prevoz.android.provider;


import android.net.Uri;

import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.FloatColumn;
import edu.mit.mobile.android.content.column.IntegerColumn;
import edu.mit.mobile.android.content.column.TextColumn;

public class Location implements ContentItem
{
    public static final String PATH = "geo/city";
    public static final Uri CONTENT_URI = ProviderUtils.toContentUri(DataProvider.AUTHORITY, PATH);

    @DBColumn(type = TextColumn.class)
    public static final String NAME = "name";
    @DBColumn(type = TextColumn.class)
    public static final String NAME_ASCII = "name_ascii";
    @DBColumn(type = TextColumn.class)
    public static final String COUNTRY = "country";
    @DBColumn(type = FloatColumn.class)
    public static final String LATITUDE = "lat";
    @DBColumn(type = FloatColumn.class)
    public static final String LONGTITUDE = "lng";
    @DBColumn(type = IntegerColumn.class)
    public static final String SORT_NUMBER = "sort";
}
