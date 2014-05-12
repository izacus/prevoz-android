package org.prevoz.android.push;

import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.api.Scope;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestPushStatus;
import org.prevoz.android.api.rest.RestPushSubscription;
import org.prevoz.android.model.City;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.io.IOException;
import java.util.Date;

@EBean(scope = Scope.Singleton)
public class PushManager
{
    private static final String LOG_TAG = "Prevoz.Push";
    private static final String GCM_PROJECT_ID = "121500391433";

    private ConditionVariable initLock;

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

            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            gcmId = gcm.register(GCM_PROJECT_ID);
            available = true;
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "Failed to register for notifications.", e);
            return;
        }
        finally
        {
            initLock.open();
        }
    }

    public void setSubscriptionStatus(City from, City to, Date date, boolean subscribed)
    {
        ApiClient.getAdapter().setSubscriptionState(new RestPushSubscription(gcmId, from, to, date, subscribed), new Callback<RestPushStatus>() {

            @Override
            public void success(RestPushStatus restPushStatus, Response response)
            {
                Toast.makeText(context, "Registracija uspela.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void failure(RetrofitError retrofitError)
            {
                Toast.makeText(context, "Registracija neuspešna.", Toast.LENGTH_SHORT).show();
            }
        });
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
