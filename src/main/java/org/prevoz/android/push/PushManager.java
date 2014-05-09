package org.prevoz.android.push;

import android.content.Context;
import android.os.ConditionVariable;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.api.Scope;

import java.io.IOException;

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
