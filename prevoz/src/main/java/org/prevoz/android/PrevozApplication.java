package org.prevoz.android;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.jakewharton.threetenabp.AndroidThreeTen;

import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.util.LocaleUtil;

import java.io.File;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;

public class PrevozApplication extends Application
{
    public static int VERSION = -1;
    private ApplicationComponent component;

    @Inject
    protected AuthenticationUtils authUtils;

    @Inject
    protected PrevozDatabase database;

    @Override
    public void onCreate()
    {
        LocaleUtil.checkSetLocale(this, getResources().getConfiguration());
        super.onCreate();

        Crashlytics crashlytics = new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build();
        Fabric.with(this, crashlytics, new Answers());

        AndroidThreeTen.init(this);
        component = DaggerApplicationComponent.builder()
                                              .applicationModule(new ApplicationModule(this))
                                              .build();
        component.inject(this);

        try
        {
            VERSION = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        new PruneHistory(database, authUtils).execute();
    }

    public ApplicationComponent component() {
        return component;
    }

    private static class PruneHistory extends AsyncTask<Void, Void, Void>
    {
        private final PrevozDatabase database;
        private final AuthenticationUtils authUtils;

        public PruneHistory(PrevozDatabase database, AuthenticationUtils authenticationUtils) {
            this.database = database;
            this.authUtils = authenticationUtils;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            database.deleteOldHistoryEntries(5);
            database.pruneOldNotifications();
            authUtils.updateRetrofitAuthenticationCookie();

            // Check for old database file
            File settingsDb = new File(Environment.getDataDirectory(), "/databases/settings.db");
            if (settingsDb.exists()) {
                settingsDb.delete();
            }

            return null;
        }
    }
}
