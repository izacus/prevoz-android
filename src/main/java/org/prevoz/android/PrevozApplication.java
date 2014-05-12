package org.prevoz.android;

import android.app.Application;
import android.content.res.Configuration;

import android.os.AsyncTask;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EApplication;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.util.Locale;

@EApplication
public class PrevozApplication extends Application
{
    @Bean
    protected AuthenticationUtils authUtils;

    @Override
    public void onCreate()
    {
        super.onCreate();
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
