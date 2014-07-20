package org.prevoz.android.provider;

import android.net.Uri;

import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.TextColumn;

public class Country implements ContentItem
{
    public static final String PATH = "geo/country";
    public static final Uri CONTENT_URI = ProviderUtils.toContentUri(DataProvider.AUTHORITY, PATH);

    @DBColumn(type = TextColumn.class)
    public static final String COUNTRY_CODE = "country_code";
    @DBColumn(type = TextColumn.class)
    public static final String NAME = "name";
    @DBColumn(type = TextColumn.class)
    public static final String LANGUAGE = "lang";
}
