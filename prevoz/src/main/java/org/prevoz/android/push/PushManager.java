package org.prevoz.android.push;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.prevoz.android.api.ApiClient;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.util.ViewUtils;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.List;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
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

    public void setSubscriptionStatus(final Activity context, final City from, final City to, final LocalDate date, final boolean subscribed)
    {
        gcmIdObservable.flatMap((gcmId) -> ApiClient.getAdapter().setSubscriptionState(gcmId,
                                                from.getDisplayName(),
                                                from.getCountryCode(),
                                                to.getDisplayName(),
                                                to.getCountryCode(),
                                                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                                subscribed ? "subscribe" : "unsubscribe"))
                       .subscribeOn(Schedulers.io())
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(
                               status -> {
                                   Observable<Boolean> databaseObservable;
                                   if (subscribed) {
                                       databaseObservable = database.addNotificationSubscription(from, to, date);
                                   } else {
                                       databaseObservable = database.deleteNotificationSubscription(from, to, date);
                                   }

                                   databaseObservable
                                           .observeOn(AndroidSchedulers.mainThread())
                                           .subscribe(success -> {
                                               ViewUtils.showMessage(context, subscribed ? "Prijavljeni ste na obvestila." : "Obveščanje preklicano.", false);
                                               EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged());
                                           },
                                           e -> Log.e(LOG_TAG, "Failed to update notification DB!", e));
                               },
                               throwable -> {
                                   ViewUtils.showMessage(context, "Obveščanja ni bilo mogoče vklopiti.", true);
                                   EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged());
                               });
    }

    public Single<Boolean> isSubscribed(City from, City to, LocalDate date)
    {
        return database.isSubscribedForNotification(from, to, date);
    }

    public boolean isPushAvailable()
    {
        return available;
    }
}
