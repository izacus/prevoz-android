package org.prevoz.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;

import com.crashlytics.android.Crashlytics;

import org.prevoz.android.auth.AuthenticationModule;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.util.ContentUtils;

import java.io.File;

import javax.inject.Inject;

public class PrevozApplication extends Application
{
    public static int VERSION = -1;
    private ApplicationComponent component;

    @Inject
    protected AuthenticationUtils authUtils;

    @Override
    public void onCreate()
    {
        super.onCreate();
        component = DaggerApplicationComponent.builder()
                                              .applicationModule(new ApplicationModule(this))
                                              .build();
        component.inject(this);

        if (!BuildConfig.DEBUG) Crashlytics.start(this);

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

    public ApplicationComponent component() {
        return component;
    }

    private class PruneHistory extends AsyncTask<Void, Void, Void>
    {

        @Override
        protected Void doInBackground(Void... params)
        {
            ContentUtils.importDatabase(PrevozApplication.this);
            ContentUtils.deleteOldHistoryEntries(PrevozApplication.this, 5);
            ContentUtils.pruneOldNotifications(PrevozApplication.this);
            authUtils.updateRetrofitAuthenticationCookie();

            // Check for old database file
            File settingsDb = new File(Environment.getDataDirectory(), "/databases/settings.db");
            if (settingsDb.exists())
                settingsDb.delete();

            return null;
        }
    }
}
