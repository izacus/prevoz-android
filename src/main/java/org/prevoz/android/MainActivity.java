
package org.prevoz.android;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.googlecode.androidannotations.annotations.*;

import org.prevoz.android.search.SearchResultsFragment;
import org.prevoz.android.search.SearchResultsFragment_;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.util.Locale;

@EActivity(R.layout.activity_main)
public class MainActivity extends FragmentActivity
{
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";

    @ViewById(R.id.main_drawer)
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerLayoutToggle;

    @ViewById(R.id.main_left_drawer)
    protected ListView leftDrawer;

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
        drawerLayoutToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, 0, 0);
        drawerLayout.setDrawerListener(drawerLayoutToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        prepareDrawer();

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
        drawerLayoutToggle.onConfigurationChanged(newConfig);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        drawerLayoutToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (drawerLayoutToggle.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
    }

    private void prepareDrawer()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        adapter.add("Moji prevozi");
        adapter.add("Obvestila");

        leftDrawer.setAdapter(adapter);
    }

    @ItemClick(R.id.main_left_drawer)
    protected void clickDrawerOption(int position)
    {

    }
}
