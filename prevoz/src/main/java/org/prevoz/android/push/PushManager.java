package org.prevoz.android.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestPushStatus;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.model.Route;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.List;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Single;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class PushManager
{
    private static final String GCM_ID_KEY = "GCM_ID_";
    private static final String LOG_TAG = "Prevoz.Push";
    private static final String GCM_PROJECT_ID = "121500391433";

    private final Context context;
    private final PrevozDatabase database;
    private Observable<String> gcmIdObservable;
    boolean available = false;

    public PushManager(Context ctx, PrevozDatabase database)
    {
        this.context = ctx;
        this.database = database;
        setup();
    }

    protected void setup()
    {
        gcmIdObservable = Observable.defer(() -> {
                // Check for play services first
                int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
                if (result != ConnectionResult.SUCCESS) {
                    Log.e(LOG_TAG, "Google play services not available on device: " + GooglePlayServicesUtil.getErrorString(result));
                    throw OnErrorThrowable.from(new IOException("Google Play services not available."));
                }

            try {
                // Try to get GCM ID from preferences
                int version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String PREF_KEY = GCM_ID_KEY + String.valueOf(version);
                String gcmId = prefs.getString(PREF_KEY, null);

                if (gcmId == null) {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                    gcmId = gcm.register(GCM_PROJECT_ID);
                    prefs.edit().putString(PREF_KEY, gcmId).apply();
                }

                available = (gcmId != null);
                return Observable.just(gcmId);
            } catch (IOException|PackageManager.NameNotFoundException e) {
                throw OnErrorThrowable.from(e);
            }
        }).cache();

        gcmIdObservable
                .subscribeOn(Schedulers.io())
                .subscribe(gcmId -> {
                }, throwable -> Log.e(LOG_TAG, "Error", throwable));
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
        return gcmIdObservable.flatMap((gcmId) -> ApiClient.getAdapter().setSubscriptionState(gcmId,
                                                route.getFrom().getDisplayName(),
                                                route.getFrom().getCountryCode(),
                                                route.getTo().getDisplayName(),
                                                route.getTo().getCountryCode(),
                                                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                                subscribed ? "subscribe" : "unsubscribe"))
                .flatMap(new Func1<RestPushStatus, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(RestPushStatus status) {
                        if (!status.isSuccessful()) throw new RuntimeException("Failed to set push status.");
                        if (subscribed) {
                            return database.addNotificationSubscription(route, date);
                        } else {
                            return database.deleteNotificationSubscription(route, date);
                        }
                    }
                })
                .doOnNext(success -> {
                    if (success) EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged(subscribed));
                })
                .subscribeOn(Schedulers.io())
                .toSingle();
    }

    public Single<Boolean> isSubscribed(@NonNull Route route, @NonNull LocalDate date)
    {
        return database.isSubscribedForNotification(route.getFrom(), route.getTo(), date);
    }

    public boolean isPushAvailable()
    {
        return available;
    }
}
