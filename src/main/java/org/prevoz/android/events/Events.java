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
        public final int[] rideIds;

        public NewSearchEvent(City from, City to, Calendar date)
        {
            this.from = from;
            this.to = to;
            this.date = date;
            this.rideIds = new int[0];
        }

        public NewSearchEvent(City from, City to, Calendar date, int[] rideIds)
        {
            this.from = from;
            this.to = to;
            this.date = date;
            this.rideIds = rideIds == null ? new int[0] : rideIds;
        }
    }

    public static class ClearSearchEvent {}

    public static class SearchComplete {}

    public static class SearchFillWithRoute
    {
        public final Route route;
        public final Calendar date;
        public final boolean searchInProgress;

        public SearchFillWithRoute(Route route)
        {
            this.route = route;
            this.date = null;
            this.searchInProgress = false;
        }

        public SearchFillWithRoute(Route route, Calendar date, boolean inProgress)
        {
            this.route = route;
            this.date = date;
            this.searchInProgress = inProgress;
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

    public static class LoginStateChanged{}
}
