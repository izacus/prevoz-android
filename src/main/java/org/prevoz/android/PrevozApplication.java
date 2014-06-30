package org.prevoz.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import com.crashlytics.android.Crashlytics;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.util.Database;

@EApplication
public class PrevozApplication extends Application
{
    public static int VERSION = -1;

    @Bean
    protected AuthenticationUtils authUtils;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Crashlytics.start(this);

        try
        {
            VERSION = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        new PruneHistory().execute();
    }

    private class PruneHistory extends AsyncTask<Void, Void, Void>
    {

        @Override
        protected Void doInBackground(Void... params)
        {
            authUtils.updateRetrofitAuthenticationCookie();
            Database.deleteHistoryEntries(PrevozApplication.this, 10);
            Database.pruneOldNotifications(PrevozApplication.this);
            return null;
        }
    }
}
