package org.prevoz.android.push;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.ConditionVariable;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestPushStatus;
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
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

@EBean(scope = EBean.Scope.Singleton)
public class PushManager
{
    private static final String GCM_ID_KEY = "GCM_ID_";
    private static final String LOG_TAG = "Prevoz.Push";
    private static final String GCM_PROJECT_ID = "121500391433";

    private final ConditionVariable initLock;

    private final Context context;
    private boolean available = false;
    private String gcmId = null;

    public PushManager(Context ctx)
    {
        this.context = ctx;

        initLock = new ConditionVariable(false);
        setup();
    }



    @Background
    protected void setup()
    {
        try
        {
            // Check for play services first
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
            if (result != ConnectionResult.SUCCESS)
            {
                Log.e(LOG_TAG, "Google play services not available on device: " + GooglePlayServicesUtil.getErrorString(result));
                return;
            }

            // Try to get GCM ID from preferences
            int version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String PREF_KEY = GCM_ID_KEY + String.valueOf(version);
            gcmId = prefs.getString(PREF_KEY, null);

            if (gcmId == null)
            {
                GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                gcmId = gcm.register(GCM_PROJECT_ID);
                prefs.edit().putString(PREF_KEY, gcmId).apply();
            }

            available = true;
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "Failed to register for notifications.", e);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e(LOG_TAG, "Failed!", e);
        }
        finally
        {
            initLock.open();
        }
    }

    public List<NotificationSubscription> getSubscriptions()
    {
        return ContentUtils.getNotificationSubscriptions(context);
    }

    public void setSubscriptionStatus(final Activity context, final City from, final City to, final Calendar date, final boolean subscribed)
    {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        ApiClient.getAdapter().setSubscriptionState(gcmId,
                                                    from.getDisplayName(),
                                                    from.getCountryCode(),
                                                    to.getDisplayName(),
                                                    to.getCountryCode(),
                                                    dateFormat.format(date.getTime()),
                                                    subscribed ? "subscribe" : "unsubscribe", new Callback<RestPushStatus>() {

            @Override
            public void success(RestPushStatus restPushStatus, Response response)
            {
                if (subscribed)
                    ContentUtils.addNotificationSubscription(context, from, to, date);
                else
                    ContentUtils.deleteNotificationSubscription(context, from, to, date);

                ViewUtils.showMessage(context, subscribed ? "Prijavljeni ste na obvestila." : "Obveščanje preklicano.", false);
                EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged());
            }

            @Override
            public void failure(RetrofitError retrofitError)
            {
                ViewUtils.showMessage(context, "Obveščanja ni bilo mogoče vklopiti.", true);
                EventBus.getDefault().post(new Events.NotificationSubscriptionStatusChanged());
            }
        });
    }

    public boolean isSubscribed(City from, City to, Calendar date)
    {
        return ContentUtils.isSubscribedForNotification(context, from, to, date);
    }

    public String getGcmId()
    {
        initLock.block();
        return gcmId;
    }

    public boolean isPushAvailable()
    {
        return available && gcmId != null;
    }
}
