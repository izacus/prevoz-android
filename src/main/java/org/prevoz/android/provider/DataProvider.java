package org.prevoz.android.provider;

import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.QuerystringWrapper;
import edu.mit.mobile.android.content.SimpleContentProvider;
import org.prevoz.android.model.NotificationSubscription;

public class DataProvider extends SimpleContentProvider
{
    public static final String AUTHORITY = "org.prevoz.android.data";
    public static final int DB_VERSION = 15;

    public DataProvider()
    {
        super(AUTHORITY, DB_VERSION);

        // Cities
        final GenericDBHelper locationHelper = new GenericDBHelper(Location.class);
        final QuerystringWrapper locationWrapper = new QuerystringWrapper(locationHelper);
        addDirAndItemUri(locationWrapper, Location.PATH);

        // Countries
        final GenericDBHelper countryHelper = new GenericDBHelper(Country.class);
        final QuerystringWrapper countryWrapper = new QuerystringWrapper(countryHelper);
        addDirAndItemUri(countryWrapper, Country.PATH);

        // Search history
        final GenericDBHelper historyHelper = new GenericDBHelper(SearchHistoryItem.class);
        final QuerystringWrapper historyWrapper = new QuerystringWrapper(historyHelper);
        addDirAndItemUri(historyWrapper, SearchHistoryItem.PATH);

        // Notifications
        final GenericDBHelper notificationHelper = new GenericDBHelper(Notification.class);
        final QuerystringWrapper notificationWrapper = new QuerystringWrapper(notificationHelper);
        addDirAndItemUri(notificationWrapper, Notification.PATH);
    }

}
