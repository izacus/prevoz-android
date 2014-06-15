package org.prevoz.android.events;

import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;

import java.util.Calendar;

public class Events
{
    public static class NewSearchEvent
    {
        public final City from;
        public final City to;
        public final Calendar date;

        public NewSearchEvent(City from, City to, Calendar date)
        {
            this.from = from;
            this.to = to;
            this.date = date;
        }
    }

    public static class ClearSearchEvent {}

    public static class SearchComplete {}

    public static class SearchFillWithRoute
    {
        public final Route route;
        public final Calendar date;

        public SearchFillWithRoute(Route route)
        {
            this.route = route;
            this.date = null;
        }

        public SearchFillWithRoute(Route route, Calendar date)
        {
            this.route = route;
            this.date = date;
        }
    }

    public static class NotificationSubscriptionStatusChanged {}

    public static class RideDeleted
    {
        public final Long id;

        public RideDeleted(Long id)
        {
            this.id = id;
        }
    }
}
