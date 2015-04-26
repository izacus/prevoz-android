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
import org.prevoz.android.util.ContentUtils;
import org.prevoz.android.util.ViewUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.schedulers.Schedulers;

public class PushManager
{
    private static final String GCM_ID_KEY = "GCM_ID_";
    private static final String LOG_TAG = "Prevoz.Push";
    private static final String GCM_PROJECT_ID = "121500391433";

    private final Context context;
    private Observable<String> gcmIdObservable;
    boolean available = false;

    public PushManager(Context ctx)
    {
        this.context = ctx;
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

    public List<NotificationSubscription> getSubscriptions()
    {
        return ContentUtils.getNotificationSubscriptions(context);
    }

    public void setSubscriptionStatus(final Activity context, final City from, final City to, final Calendar date, final boolean subscribed)
    {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        gcmIdObservable.flatMap((gcmId) -> ApiClient.getAdapter().setSubscriptionState(gcmId,
                                                from.getDisplayName(),
                                                from.getCountryCode(),
                                                to.getDisplayName(),
                                                to.getCountryCode(),
                                                dateFormat.format(date.getTime()),
                                                subscribed ? "subscribe" : "unsubscribe"))
                       .subscribeOn(Schedulers.io())
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(
                               status -> {
                                   if (subscribed)
                                       ContentUtils.addNotificationSubscription(context, from, to, date);
                                   else
                                       ContentUtils.deleteNotificationSubscription(context, from, to, date);

                                   ViewUtils.showMessage(context, subscribed ? "Prijavljeni ste na obvestila." : "Obveščanje preklicano.", false);
                                   EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged());
                               },
                               throwable -> {
                                   ViewUtils.showMessage(context, "Obveščanja ni bilo mogoče vklopiti.", true);
                                   EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged());
                               });
    }

    public boolean isSubscribed(City from, City to, Calendar date)
    {
        return ContentUtils.isSubscribedForNotification(context, from, to, date);
    }

    public boolean isPushAvailable()
    {
        return available;
    }
}
