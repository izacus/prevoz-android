package org.prevoz.android.push;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestPushStatus;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.model.Route;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Single;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class PushManager
{
    public static final String NEW_RIDE_CHANNEL_ID = "new-rides-channel";

    private static final String LOG_TAG = "Prevoz.Push";

    @NonNull private final Context applicationContext;
    @NonNull private final PrevozDatabase database;
    @Nullable private String fcmId;
    private boolean available = false;

    private Set<RegisteredNotification> notifications = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public PushManager(@NonNull Context applicationContext,
                       @NonNull PrevozDatabase database)
    {
        this.database = database;
        this.applicationContext = applicationContext;
        setup();
    }

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    protected void setup()
    {
        available = false;
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(result -> {
            fcmId = result.getToken();
            available = true;
            Log.i("Prevoz", "Prevoz FCM ID: " + fcmId);
        });


        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) Objects.requireNonNull(applicationContext.getSystemService(Context.NOTIFICATION_SERVICE));
            NotificationChannel newRidesChannel = new NotificationChannel(NEW_RIDE_CHANNEL_ID, "Novi prevozi", NotificationManager.IMPORTANCE_HIGH);
            newRidesChannel.setDescription("Obvestila o novih prevozih");
            newRidesChannel.setLightColor(ContextCompat.getColor(applicationContext, R.color.prevoztheme_color));
            newRidesChannel.enableLights(true);
            newRidesChannel.enableVibration(true);
            newRidesChannel.setShowBadge(true);
            newRidesChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(newRidesChannel);
        }

        database.getNotificationSubscriptions()
                .flatMap(Observable::from)
                .subscribe(notification -> notifications.add(new RegisteredNotification(notification.getRoute(), notification.getDate())));
    }

    public Single<List<NotificationSubscription>> getSubscriptions()
    {
        return database.getNotificationSubscriptions()
                       .flatMap(Observable::from)
                       .map(n -> new NotificationSubscription(n.getId(), new City(n.getFromCity(), n.getFromCountry()), new City(n.getToCity(), n.getToCountry()), n.getDate()))
                       .toList()
                       .toSingle();
    }

    public Single<Boolean> setSubscriptionStatus(@NonNull final Route route, @NonNull final LocalDate date, final boolean subscribed)
    {
        return ApiClient.getAdapter().setSubscriptionState(fcmId,
                    route.getFrom().getDisplayName(),
                    route.getFrom().getCountryCode(),
                    route.getTo().getDisplayName(),
                    route.getTo().getCountryCode(),
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    subscribed ? "subscribe" : "unsubscribe")
                .flatMap((Func1<RestPushStatus, Observable<Boolean>>) status -> {
                    if (!status.isSuccessful()) throw new RuntimeException("Failed to set push status.");
                    Log.d(LOG_TAG, "Subscription changed for " + route + " / " + date + " / " + subscribed);
                    if (subscribed) {
                        return database.addNotificationSubscription(route, date);
                    } else {
                        return database.deleteNotificationSubscription(route, date);
                    }
                })
                .doOnNext(success -> {
                    if (success) {
                        EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged(route, date, subscribed));
                        if (subscribed) {
                            notifications.add(new RegisteredNotification(route, date));
                        } else {
                            notifications.remove(new RegisteredNotification(route, date));
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .toSingle();
    }

    public boolean isSubscribed(@NonNull Route route, @NonNull LocalDate date)
    {
        return notifications.contains(new RegisteredNotification(route, date));
    }

    public boolean isPushAvailable()
    {
        if (fcmId == null) setup();
        return available;
    }

    private static final class RegisteredNotification {
        @NonNull public final Route route;
        @NonNull public final LocalDate date;

        RegisteredNotification(@NonNull Route route, @NonNull LocalDate date) {
            this.route = route;
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RegisteredNotification that = (RegisteredNotification) o;

            return route.equals(that.route) && date.equals(that.date);
        }

        @Override
        public int hashCode() {
            int result = route.hashCode();
            result = 31 * result + date.hashCode();
            return result;
        }
    }
}
