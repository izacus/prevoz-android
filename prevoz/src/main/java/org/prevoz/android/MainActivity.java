
package org.prevoz.android;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;

import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.myrides.MyRidesFragment;
import org.prevoz.android.myrides.NewRideActivity;
import org.prevoz.android.push.PushFragment;
import org.prevoz.android.search.SearchResultsFragment;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.ViewUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@SuppressLint("Registered")
public class MainActivity extends PrevozActivity
{
    public static final int REQUEST_CODE_AUTHORIZE_MYRIDES = 100;
    public static final int REQUEST_CODE_AUTHORIZE_NEWRIDE = 101;

    private static final String SEARCH_FRAGMENT_TAG = "SearchResultsFragment";
    private static final String PUSH_NOTIFICATIONS_FRAGMENT_TAG = "PushNotificationsFragment";
    private static final String MY_RIDES_FRAGMENT_TAG = "MyRidesFragment";

    private static final String PREF_SHOWN_LOGIN_PROMPT = "Prevoz.LoginPromptShown";

    @BindView(R.id.main_toolbar)
    protected Toolbar toolbar;

    @BindView(R.id.main_drawer)
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle drawerLayoutToggle;

    @BindView(R.id.main_left_drawer_list)
    protected ListView leftDrawer;

    @BindView(R.id.main_left_drawer_username)
    protected TextView leftUsername;
    @BindView(R.id.main_left_drawer_logout)
    protected TextView leftLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ab);
            ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(getString(R.string.app_name), icon, getResources().getColor(R.color.prevoztheme_color_dark));
            setTaskDescription(td);
        }

        setSupportActionBar(toolbar);
        drawerLayoutToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayoutToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.setDrawerListener(drawerLayoutToggle);

        prepareDrawer();
        checkAuthenticated();

        // Attach search fragment if it's missing
        if (savedInstanceState == null)
        {
            FragmentManager fm = getSupportFragmentManager();

            if (fm.findFragmentByTag(SEARCH_FRAGMENT_TAG) == null) {
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.main_search_container,
                                    new SearchResultsFragment(getApplicationComponent()),
                                    SEARCH_FRAGMENT_TAG);
                transaction.commit();
            }
        }
    }

    protected void checkAuthenticated()
    {
        authUtils.getUsername()
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(this::setDrawerUsername);
    }

    protected void setDrawerUsername(String username)
    {
        if (username == null)
        {
            leftUsername.setText(getString(R.string.app_name));
            leftLogout.setVisibility(View.GONE);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean(PREF_SHOWN_LOGIN_PROMPT, false)) {
                Typeface tf = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
                Snackbar.with(this)
                        .text("Niste prijavljeni.")
                        .textTypeface(tf)
                        .colorResource(R.color.prevoztheme_color_dark)
                        .actionLabel("PRIJAVA")
                        .actionLabelTypeface(tf)
                        .actionListener(snackbar -> authUtils.requestAuthentication(MainActivity.this, 0))
                        .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                        .show(this);

                prefs.edit().putBoolean(PREF_SHOWN_LOGIN_PROMPT, true).apply();
            }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.fragment_myrides, menu);

        menu.findItem(R.id.menu_myrides_add).setOnMenuItemClickListener(item -> {
            if (!authUtils.isAuthenticated()) {
                authUtils.requestAuthentication(MainActivity.this, REQUEST_CODE_AUTHORIZE_NEWRIDE);
            } else {
                Intent i = new Intent(this, NewRideActivity.class);
                ActivityCompat.startActivity(this, i, ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_up, 0).toBundle());
            }

            return true;
        });

        return true;
    }

    @OnClick(R.id.main_left_user_box)
    protected void logoutClick()
    {
        if (authUtils.getUsername() == null)
            return;

        new AlertDialog.Builder(this, R.style.Prevoz_Theme_Dialog)
                       .setTitle("Odjava")
                       .setMessage("Se res želite odjaviti?")
                       .setPositiveButton("Odjavi", (dialog, which) -> authUtils.logout().subscribeOn(Schedulers.io()).subscribe())
                       .setNegativeButton("Prekliči", null)
                       .show();
    }

    protected void triggerSearchFromIntent(Intent intent)
    {
        showFragment(UiFragment.FRAGMENT_SEARCH, false);
        City from = intent.getParcelableExtra("from");
        City to = intent.getParcelableExtra("to");

        LocalDate date = LocalDate.from(Instant.ofEpochMilli(intent.getLongExtra("when", 0)).atZone(LocaleUtil.getLocalTimezone()));
        int[] highlights = intent.getIntArrayExtra("highlights");

        Route route = new Route(from, to);
        EventBus.getDefault().postSticky(new Events.NewSearchEvent(route, date, highlights));
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
        return drawerLayoutToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);

    }

    private void prepareDrawer()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
        adapter.add("Iskanje");
        adapter.add("Moji prevozi");
        adapter.add("Obvestila");

        leftDrawer.setAdapter(adapter);
        leftDrawer.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
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
        });
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
                    f = new SearchResultsFragment(getApplicationComponent());
                    tag = SEARCH_FRAGMENT_TAG;
                }
                break;
            case FRAGMENT_MY_RIDES:
                if (fm.findFragmentByTag(MY_RIDES_FRAGMENT_TAG) == null)
                {
                    f = new MyRidesFragment();
                    tag = MY_RIDES_FRAGMENT_TAG;
                }
                break;
            case FRAGMENT_NOTIFICATIONS:
                if (fm.findFragmentByTag(PUSH_NOTIFICATIONS_FRAGMENT_TAG) == null)
                {
                    f = new PushFragment();
                    tag = PUSH_NOTIFICATIONS_FRAGMENT_TAG;
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
                else {
                    Intent i = new Intent(this, NewRideActivity.class);
                    startActivity(i);
                }
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        SearchResultsFragment fragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentByTag(SEARCH_FRAGMENT_TAG);
        if (fragment != null && fragment.showingResults())
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
        EventBus.getDefault().removeStickyEvent(e);
    }

    public void onEventMainThread(Events.ShowMessage e) {
        ViewUtils.showMessage(this, e.getMessage(this), e.isError());
        EventBus.getDefault().removeStickyEvent(e);
    }
}
