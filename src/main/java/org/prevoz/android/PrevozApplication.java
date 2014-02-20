package org.prevoz.android;

import android.app.Application;
import android.content.res.Configuration;

import org.prevoz.android.util.LocaleUtil;

import java.util.Locale;

/**
 * Created by jernej on 15/02/14.
 */
public class PrevozApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        Configuration config = getBaseContext().getResources().getConfiguration();

        Locale appLocale = LocaleUtil.getLocale();
        if (config.locale != appLocale)
        {
            Locale.setDefault(appLocale);
            config.locale = appLocale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
    }
}
