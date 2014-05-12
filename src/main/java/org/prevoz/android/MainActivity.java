
package org.prevoz.android;

import android.accounts.Account;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.widget.DrawerLayout;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.googlecode.androidannotations.annotations.*;

import de.greenrobot.event.EventBus;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.search.SearchResultsFragment;
import org.prevoz.android.search.SearchResultsFragment_;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.util.Calendar;
import java.util.Locale;

@EActivity(R.layout.activity_main)
public class MainActivity extends SherlockFragmentActivity
{
    private static final String SEARCH_FRAGMENT_TAG = "SearchFragment";

    @ViewById(R.id.main_drawer)
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerLayoutToggle;

    @ViewById(R.id.main_left_drawer_list)
    protected ListView leftDrawer;

    @ViewById(R.id.main_left_drawer_username)
    protected TextView leftUsername;

    @FragmentByTag(SEARCH_FRAGMENT_TAG)
    protected SearchResultsFragment searchFragment;

    @Bean
    protected AuthenticationUtils authUtils;

    @Bean
    protected PushManager pushManager;  // This is here for initialization at startup

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
        drawerLayoutToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_navigation_drawer, 0, 0);
        drawerLayout.setDrawerListener(drawerLayoutToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        prepareDrawer();
        checkAuthenticated();

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

    @Background
    protected void checkAuthenticated()
    {
        String username = authUtils.getUsername();
        if (username != null)
            setDrawerUsername(username);
    }

    @UiThread
    protected void setDrawerUsername(String username)
    {
        leftUsername.setText(username);
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

        if (getIntent().hasExtra("from") && getIntent().hasExtra("to"))
        {
            City from = getIntent().getParcelableExtra("from");
            City to = getIntent().getParcelableExtra("to");

            Calendar date = Calendar.getInstance();
            date.setTimeZone(LocaleUtil.getLocalTimezone());
            date.setTimeInMillis(getIntent().getLongExtra("when", 0));

            EventBus.getDefault().postSticky(new Events.NewSearchEvent(from, to, date));
            Route route = new Route(from, to);
            EventBus.getDefault().postSticky(new Events.SearchFillWithRoute(route, date));

        }
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
        if (drawerLayoutToggle.onOptionsItemSelected(getMenuItem(item)))
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

    @ItemClick(R.id.main_left_drawer_list)
    protected void clickDrawerOption(int position)
    {

    }

    private android.view.MenuItem getMenuItem(final MenuItem item)
    {
        return new android.view.MenuItem()
        {

            @Override
            public int getItemId()
            {
                return item.getItemId();
            }

            @Override
            public int getGroupId()
            {
                return 0;
            }

            @Override
            public int getOrder()
            {
                return 0;
            }

            @Override
            public android.view.MenuItem setTitle(CharSequence title)
            {
                return null;
            }

            @Override
            public android.view.MenuItem setTitle(int title)
            {
                return null;
            }

            @Override
            public CharSequence getTitle()
            {
                return null;
            }

            @Override
            public android.view.MenuItem setTitleCondensed(CharSequence title)
            {
                return null;
            }

            @Override
            public CharSequence getTitleCondensed()
            {
                return null;
            }

            @Override
            public android.view.MenuItem setIcon(Drawable icon)
            {
                return null;
            }

            @Override
            public android.view.MenuItem setIcon(int iconRes)
            {
                return null;
            }

            @Override
            public Drawable getIcon()
            {
                return null;
            }

            @Override
            public android.view.MenuItem setIntent(Intent intent)
            {
                return null;
            }

            @Override
            public Intent getIntent()
            {
                return null;
            }

            @Override
            public android.view.MenuItem setShortcut(char numericChar, char alphaChar)
            {
                return null;
            }

            @Override
            public android.view.MenuItem setNumericShortcut(char numericChar)
            {
                return null;
            }

            @Override
            public char getNumericShortcut()
            {
                return 0;
            }

            @Override
            public android.view.MenuItem setAlphabeticShortcut(char alphaChar)
            {
                return null;
            }

            @Override
            public char getAlphabeticShortcut()
            {
                return 0;
            }

            @Override
            public android.view.MenuItem setCheckable(boolean checkable)
            {
                return null;
            }

            @Override
            public boolean isCheckable()
            {
                return false;
            }

            @Override
            public android.view.MenuItem setChecked(boolean checked)
            {
                return null;
            }

            @Override
            public boolean isChecked()
            {
                return false;
            }

            @Override
            public android.view.MenuItem setVisible(boolean visible)
            {
                return null;
            }

            @Override
            public boolean isVisible()
            {
                return false;
            }

            @Override
            public android.view.MenuItem setEnabled(boolean enabled)
            {
                return null;
            }

            @Override
            public boolean isEnabled()
            {
                return item.isEnabled();
            }

            @Override
            public boolean hasSubMenu()
            {
                return false;
            }

            @Override
            public SubMenu getSubMenu()
            {
                return null;
            }

            @Override
            public android.view.MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener)
            {
                return null;
            }

            @Override
            public ContextMenu.ContextMenuInfo getMenuInfo()
            {
                return null;
            }

            @Override
            public void setShowAsAction(int actionEnum)
            {

            }

            @Override
            public android.view.MenuItem setShowAsActionFlags(int actionEnum)
            {
                return null;
            }

            @Override
            public android.view.MenuItem setActionView(View view)
            {
                return null;
            }

            @Override
            public android.view.MenuItem setActionView(int resId)
            {
                return null;
            }

            @Override
            public View getActionView()
            {
                return null;
            }

            @Override
            public android.view.MenuItem setActionProvider(ActionProvider actionProvider)
            {
                return null;
            }

            @Override
            public ActionProvider getActionProvider()
            {
                return null;
            }

            @Override
            public boolean expandActionView()
            {
                return false;
            }

            @Override
            public boolean collapseActionView()
            {
                return false;
            }

            @Override
            public boolean isActionViewExpanded()
            {
                return false;
            }

            @Override
            public android.view.MenuItem setOnActionExpandListener(OnActionExpandListener listener)
            {
                return null;
            }
        };
    }
}
