package org.prevoz.android.c2dm;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

public class GCMTools
{
    private static final String LOG_TAG = "Prevoz.GCM";

    public static void checkRegisterGCM(Context ctx)
    {
        new RegisterForGcm(ctx).execute();
    }

    private static class RegisterForGcm extends AsyncTask<Void, Void, Void>
    {
        private final Context ctx;

        public RegisterForGcm(Context ctx)
        {
            this.ctx = ctx.getApplicationContext();
        }

        @Override
        protected Void doInBackground(Void... params)
        {

            String gcmId;

            try
            {
                gcmId = GoogleCloudMessaging.getInstance(ctx).register(NotificationManager.GCM_PROJECT_ID);
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, "Failed to retrieve GCM key.", e);
                return null;
            }


            if (gcmId == null || gcmId.length() == 0)
                return null;

            Log.d(LOG_TAG, "Registering GCM with key " + gcmId);
            NotificationManager.getInstance(ctx).setRegistrationId(gcmId);
            return null;
        }
    }
}
