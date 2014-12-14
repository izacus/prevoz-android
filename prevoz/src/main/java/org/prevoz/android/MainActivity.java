
package org.prevoz.android;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.myrides.MyRidesFragment_;
import org.prevoz.android.myrides.NewRideFragment;
import org.prevoz.android.myrides.NewRideFragment_;
import org.prevoz.android.push.PushFragment_;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.search.SearchResultsFragment;
import org.prevoz.android.search.SearchResultsFragment_;
import org.prevoz.android.util.LocaleUtil;

import java.util.Calendar;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;

@SuppressLint("Registered")          // AndroidAnnotated activity is registered.
@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.fragment_myrides)
public class MainActivity extends ActionBarActivity
{
    public static final int REQUEST_CODE_AUTHORIZE_MYRIDES = 100;
    public static final int REQUEST_CODE_AUTHORIZE_NEWRIDE = 101;


    private static final String SEARCH_FRAGMENT_TAG = "SearchResultsFragment";
    private static final String PUSH_NOTIFICATIONS_FRAGMENT_TAG = "PushNotificationsFragment";
    private static final String MY_RIDES_FRAGMENT_TAG = "MyRidesFragment";
    private static final String NEW_RIDE_FRAGMENT_TAG = "NewRideFragment";


    @ViewById(R.id.main_drawer)
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerLayoutToggle;

    @ViewById(R.id.main_left_drawer_list)
    protected ListView leftDrawer;

    @ViewById(R.id.main_left_drawer_username)
    protected TextView leftUsername;
    @ViewById(R.id.main_left_drawer_logout)
    protected TextView leftLogout;

    @Bean
    protected AuthenticationUtils authUtils;

    @Bean
    protected PushManager pushManager;  // This is here for initialization at startup

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        LocaleUtil.checkSetLocale(this, getResources().getConfiguration());
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ab);
            ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(getString(R.string.app_name), icon, getResources().getColor(R.color.prevoztheme_color_dark));
            setTaskDescription(td);
        }

        // Attach search fragment if it's missing
        if (savedInstanceState == null)
        {
            FragmentManager fm = getSupportFragmentManager();

            if (fm.findFragmentByTag(SEARCH_FRAGMENT_TAG) == null) {
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.main_search_container, new SearchResultsFragment_(), SEARCH_FRAGMENT_TAG);
                transaction.commit();
            }
        }
    }

    @AfterViews
    protected void initActivity()
    {
        drawerLayoutToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_navigation_drawer, 0, 0);
        drawerLayout.setDrawerListener(drawerLayoutToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        prepareDrawer();
        checkAuthenticated();
    }

    @Background
    protected void checkAuthenticated()
    {
        setDrawerUsername(authUtils.getUsername());
    }

    @UiThread
    protected void setDrawerUsername(String username)
    {
        if (username == null)
        {
            leftUsername.setText(getString(R.string.app_name));
            leftLogout.setVisibility(View.GONE);
        }
        else
        {
            leftUsername.setText(username);
            leftLogout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        EventBus.getDefault().registerSticky(this);

        if (getIntent().hasExtra("from") && getIntent().hasExtra("to"))
        {
            triggerSearchFromIntent(getIntent());
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        if (intent.hasExtra("from") && intent.hasExtra("to"))
        {
            triggerSearchFromIntent(intent);
        }
    }

    @Click(R.id.main_left_user_box)
    protected void logoutClick()
    {
        if (authUtils.getUsername() == null)
            return;

        new MaterialDialog.Builder(this)
                          .title("Odjava")
                          .titleColorRes(R.color.prevoztheme_color_dark)
                          .content("Se res želite odjaviti?")
                          .positiveText("Odjavi")
                          .negativeText("Prekliči")
                          .callback(new MaterialDialog.SimpleCallback() {
                              @Override
                              public void onPositive(MaterialDialog materialDialog) {
                                  authUtils.logout();
                              }
                          })
                          .show();
    }

    protected void triggerSearchFromIntent(Intent intent)
    {
        showFragment(UiFragment.FRAGMENT_SEARCH, false);
        City from = intent.getParcelableExtra("from");
        City to = intent.getParcelableExtra("to");

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(intent.getLongExtra("when", 0));

        int[] highlights = intent.getIntArrayExtra("highlights");

        EventBus.getDefault().postSticky(new Events.NewSearchEvent(from, to, date, highlights));
        Route route = new Route(from, to);
        EventBus.getDefault().postSticky(new Events.SearchFillWithRoute(route, date, true));
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
        adapter.add("Iskanje");
        adapter.add("Moji prevozi");
        adapter.add("Obvestila");

        leftDrawer.setAdapter(adapter);
    }

    protected void showFragment(UiFragment fragment, boolean backstack)
    {
        showFragment(fragment, backstack, null);
    }

    protected void showFragment(UiFragment fragment, boolean backstack, Bundle params)
    {
        FragmentManager fm = getSupportFragmentManager();

        Fragment f = null;
        String tag = null;

        switch(fragment)
        {
            case FRAGMENT_SEARCH:
                if (fm.findFragmentByTag(SEARCH_FRAGMENT_TAG) == null)
                {
                    f = new SearchResultsFragment_();
                    tag = SEARCH_FRAGMENT_TAG;
                }
                break;
            case FRAGMENT_MY_RIDES:
                if (fm.findFragmentByTag(MY_RIDES_FRAGMENT_TAG) == null)
                {
                    f = new MyRidesFragment_();
                    tag = MY_RIDES_FRAGMENT_TAG;
                }
                break;
            case FRAGMENT_NOTIFICATIONS:
                if (fm.findFragmentByTag(PUSH_NOTIFICATIONS_FRAGMENT_TAG) == null)
                {
                    f = new PushFragment_();
                    tag = PUSH_NOTIFICATIONS_FRAGMENT_TAG;
                }
                break;

            case FRAGMENT_NEW_RIDE:
                if (fm.findFragmentByTag(NEW_RIDE_FRAGMENT_TAG) == null)
                {
                    f = new NewRideFragment_();
                    tag = NEW_RIDE_FRAGMENT_TAG;
                }
                break;
        }

        if (f != null)
        {
            if (params != null)
                f.setArguments(params);

            replaceFragment(f, tag, backstack);
        }

        drawerLayout.closeDrawers();
    }

    private void replaceFragment(Fragment fragment, String tag, boolean backstack)
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_search_container, fragment, tag);
        if (backstack)
        {
            ft.addToBackStack(null);
        }
        else
        {
            for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++)
                getSupportFragmentManager().popBackStack();
        }

        ft.commit();
    }

    @ItemClick(R.id.main_left_drawer_list)
    protected void clickDrawerOption(int position)
    {
        switch (position)
        {
            case 0:     // SEARCH
                showFragment(UiFragment.FRAGMENT_SEARCH, false);
                break;
            case 1:     // MY RIDES
                showFragment(UiFragment.FRAGMENT_MY_RIDES, false);
                break;

            case 2:     // PUSH NOTIFICATION LIST
                showFragment(UiFragment.FRAGMENT_NOTIFICATIONS, false);
                break;
        }
    }

    @OptionsItem(R.id.menu_myrides_add)
    protected void clickAddRide()
    {
        if (!authUtils.isAuthenticated())
        {
            authUtils.requestAuthentication(this, REQUEST_CODE_AUTHORIZE_NEWRIDE);
        }
        else
        {
            showFragment(UiFragment.FRAGMENT_NEW_RIDE, true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AUTHORIZE_MYRIDES || requestCode == REQUEST_CODE_AUTHORIZE_NEWRIDE)
        {
            if (resultCode == RESULT_CANCELED)
            {
                showFragment(UiFragment.FRAGMENT_SEARCH, false);
            }
            else if (resultCode == RESULT_OK)
            {
                if (requestCode == REQUEST_CODE_AUTHORIZE_MYRIDES)
                    showFragment(UiFragment.FRAGMENT_MY_RIDES, false);
                else
                    showFragment(UiFragment.FRAGMENT_NEW_RIDE, true);
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        SearchResultsFragment fragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
        NewRideFragment newRideFragment = (NewRideFragment) getSupportFragmentManager().findFragmentByTag(NEW_RIDE_FRAGMENT_TAG);
        if (fragment != null && newRideFragment == null && fragment.showingResults())
        {
            EventBus.getDefault().post(new Events.ClearSearchEvent());
        }
        else
        {
            super.onBackPressed();
        }
    }

    public void onEventMainThread(Events.LoginStateChanged e)
    {
        checkAuthenticated();
        EventBus.getDefault().removeStickyEvent(Events.LoginStateChanged.class);
    }

    public void onEventMainThread(Events.ShowFragment e)
    {
        showFragment(e.fragment, e.backstack, e.params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }
}
