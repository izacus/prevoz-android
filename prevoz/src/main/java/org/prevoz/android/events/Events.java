package org.prevoz.android.events;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.prevoz.android.UiFragment;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.threeten.bp.LocalDate;

public class Events
{
    public static class NewSearchEvent
    {
        @NonNull public final Route route;
        @NonNull public final LocalDate date;
        @NonNull public final int[] rideIds;

        public NewSearchEvent(@NonNull Route route,
                              @NonNull LocalDate date)
        {
            this.route = route;
            this.date = date;
            this.rideIds = new int[0];
        }

        public NewSearchEvent(@NonNull Route route,
                              @NonNull LocalDate date,
                              @Nullable int[] rideIds)
        {
            this.route = route;
            this.date = date;
            this.rideIds = rideIds == null ? new int[0] : rideIds;
        }
    }

    public static class ClearSearchEvent {}

    public static class SearchComplete {}

    public static class SearchFillWithRoute
    {
        public final Route route;
        public final LocalDate date;
        public final boolean searchInProgress;

        public SearchFillWithRoute(Route route)
        {
            this.route = route;
            this.date = null;
            this.searchInProgress = false;
        }

        public SearchFillWithRoute(Route route, LocalDate date, boolean inProgress)
        {
            this.route = route;
            this.date = date;
            this.searchInProgress = inProgress;
        }
    }

    public static class NotificationSubscriptionStatusChanged {}

    public static class MyRideStatusUpdated
    {
        public final RestRide ride;
        public final boolean deleted;

        public MyRideStatusUpdated(RestRide ride, boolean deleted)
        {
            this.ride = ride;
            this.deleted = deleted;
        }
    }

    public static class LoginStateChanged{}

    public static class ShowFragment {
        public final UiFragment fragment;
        public final boolean backstack;
        public final Bundle params;

        public ShowFragment(UiFragment fragment, boolean backstack) {
            this.fragment = fragment;
            this.backstack = backstack;
            this.params = null;
        }

        public ShowFragment(UiFragment fragment, boolean backstack, Bundle params) {
            this.fragment = fragment;
            this.backstack = backstack;
            this.params = params;
        }
    }

    public static class ShowMessage {
        private final int stringId;
        private final String text;
        private final boolean isError;

        public ShowMessage(String text, boolean error) {
            this.text = text;
            this.stringId = 0;
            this.isError = error;
        }
        public ShowMessage(@StringRes int stringId, boolean error) {
            this.stringId = stringId;
            this.text = null;
            this.isError = error;
        }

        public String getMessage(Context ctx) {
            if (text != null) return text;
            return ctx.getResources().getString(stringId);
        }

        public boolean isError() {
            return isError;
        }
    }
}
