
package org.prevoz.android;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.FragmentByTag;

import org.prevoz.android.search.SearchResultsFragment;
import org.prevoz.android.search.SearchResultsFragment_;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.util.Locale;

@EActivity(R.layout.activity_main)
public class MainActivity extends SherlockFragmentActivity
{
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";

    @FragmentByTag(SEARCH_FRAGMENT_TAG)
    protected SearchResultsFragment searchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        LocaleUtil.checkSetLocale(this, getResources().getConfiguration());
        super.onCreate(savedInstanceState);
        checkInitDatabase();
    }

    @AfterViews
    protected void initActivity()
    {
        // Attach search fragment if it's missing
        FragmentManager fm = getSupportFragmentManager();
        if (searchFragment == null)
        {
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.main_search_container, new SearchResultsFragment_(), SEARCH_FRAGMENT_TAG);
            transaction.commit();
        }
    }

    @Background
    protected void checkInitDatabase()
    {
        Database.getSettingsDatabase(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        Locale appLocale = LocaleUtil.getLocale();
        if (newConfig.locale != appLocale)
        {
            newConfig.locale = appLocale;
            super.onConfigurationChanged(newConfig);
            Locale.setDefault(appLocale);
            getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        }
        else
        {
            super.onConfigurationChanged(newConfig);
        }
    }
}
