package org.prevoz.android;

import android.app.Application;
import android.content.res.Configuration;

import android.os.AsyncTask;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.util.Locale;

public class PrevozApplication extends Application
{
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
            Database.deleteHistoryEntries(PrevozApplication.this, 10);
            return null;
        }
    }
}
