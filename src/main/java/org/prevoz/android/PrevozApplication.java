package org.prevoz.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
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

        CrashManager.register(this, "bf529dcbb5c656bba1195961d0bffd08", new CrashManagerListener() {
            @Override
            public boolean shouldAutoUploadCrashes() {
                return true;
            }
        });

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
