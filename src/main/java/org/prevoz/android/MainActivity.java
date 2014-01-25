
package org.prevoz.android;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.FragmentByTag;
import org.prevoz.android.search.SearchFragment;
import org.prevoz.android.search.SearchFragment_;

@EActivity(R.layout.activity_main)
public class MainActivity extends SherlockFragmentActivity
{
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";

    @FragmentByTag(SEARCH_FRAGMENT_TAG)
    protected SearchFragment searchFragment;

    @AfterViews
    protected void initActivity()
    {
        // Attach search fragment if it's missing
        FragmentManager fm = getSupportFragmentManager();
        if (searchFragment == null)
        {
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.main_container, new SearchFragment_(), SEARCH_FRAGMENT_TAG);
            transaction.commit();
        }
    }
}
