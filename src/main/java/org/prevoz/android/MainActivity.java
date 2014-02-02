
package org.prevoz.android;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.FragmentByTag;

import org.prevoz.android.events.Events;
import org.prevoz.android.search.SearchFragment;
import org.prevoz.android.search.SearchFragment_;
import org.prevoz.android.search.SearchResultsFragment;
import org.prevoz.android.search.SearchResultsFragment_;
import org.prevoz.android.util.Database;

import de.greenrobot.event.EventBus;

@EActivity(R.layout.activity_main)
public class MainActivity extends SherlockFragmentActivity
{
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";
    private static final String SEARCH_RESULTS_FRAGMENT_TAG = "SearchResultsFragment";

    @FragmentByTag(SEARCH_FRAGMENT_TAG)
    protected SearchFragment searchFragment;

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
 //       EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
//        EventBus.getDefault().register(this);
    }

/*    public void onEventMainThread(Events.NewSearchEvent e)
    {
        Log.d("Prevoz", "Starting search for " + e.from + "-" + e.to + " [" + e.date.toString() + "]");
        SearchResultsFragment fragment = new SearchResultsFragment_();
        Bundle arguments = new Bundle();
        arguments.putString(SearchResultsFragment.PARAM_SEARCH_FROM, e.from);
        arguments.putString(SearchResultsFragment.PARAM_SEARCH_TO, e.to);
        arguments.putLong(SearchResultsFragment.PARAM_SEARCH_DATE, e.date.getTimeInMillis());
        fragment.setArguments(arguments);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_search_results_container, fragment, SEARCH_RESULTS_FRAGMENT_TAG);
        transaction.commit();
    } */
}
