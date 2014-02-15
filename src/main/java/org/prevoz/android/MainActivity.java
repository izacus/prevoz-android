
package org.prevoz.android;

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

@EActivity(R.layout.activity_main)
public class MainActivity extends SherlockFragmentActivity
{
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";

    @FragmentByTag(SEARCH_FRAGMENT_TAG)
    protected SearchResultsFragment searchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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
}
