package org.prevoz.android

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.TabLayout
import android.support.v4.app.*
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

import com.nispok.snackbar.Snackbar

import org.prevoz.android.events.Events
import org.prevoz.android.model.City
import org.prevoz.android.model.Route
import org.prevoz.android.myrides.MyRidesFragment
import org.prevoz.android.myrides.NewRideActivity
import org.prevoz.android.push.PushFragment
import org.prevoz.android.search.SearchResultsFragment
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.PrevozActivity
import org.prevoz.android.util.ViewUtils
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

import java.util.Locale

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import de.greenrobot.event.EventBus
import org.prevoz.android.search.SearchFragment
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

@SuppressLint("Registered")
class MainActivity : PrevozActivity() {

    @BindView(R.id.main_toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.main_tablayout)
    lateinit var tabLayout: TabLayout

    @BindView(R.id.main_pager)
    lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val icon = BitmapFactory.decodeResource(resources, R.drawable.icon_ab)
            val td = ActivityManager.TaskDescription(getString(R.string.app_name), icon, resources.getColor(R.color.prevoztheme_color_dark))
            setTaskDescription(td)
        }

        setSupportActionBar(toolbar)
        setupViewPager()
        tabLayout.setupWithViewPager(viewPager)
    }

    fun setupViewPager() {
        viewPager.adapter = MainPagerAdapter(resources, supportFragmentManager)
    }

    class MainPagerAdapter(val resources: Resources, fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
        val searchFragment : SearchFragment = SearchFragment()
        val myRidesFragment : MyRidesFragment = MyRidesFragment()
        val notificationsFragment : PushFragment = PushFragment()

        override fun getItem(position: Int): Fragment {
            return when(position) {
                0 -> searchFragment
                1 -> myRidesFragment
                2 -> notificationsFragment
                else -> throw IllegalArgumentException("Wrong fragment requested!")
            }
        }

        override fun getCount(): Int {
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when(position) {
                0 -> "Iskanje"
                1 -> "Moji prevozi"
                2 -> "Obvestila"
                else -> throw IllegalArgumentException("Wrong fragment title requested!")
            }
        }
    }

    /*
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
    } */

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().registerSticky(this)

        if (intent.hasExtra("from") && intent.hasExtra("to")) {
            triggerSearchFromIntent(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra("from") && intent.hasExtra("to")) {
            triggerSearchFromIntent(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val mi = menuInflater
        mi.inflate(R.menu.fragment_myrides, menu)

        menu.findItem(R.id.menu_myrides_add).setOnMenuItemClickListener { item ->
            if (!authUtils.isAuthenticated) {
                authUtils.requestAuthentication(this@MainActivity, REQUEST_CODE_AUTHORIZE_NEWRIDE)
            } else {
                val i = Intent(this, NewRideActivity::class.java)
                ActivityCompat.startActivity(this, i, ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_up, 0).toBundle())
            }

            true
        }

        return true
    }

    /*
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
    } */

    protected fun triggerSearchFromIntent(intent: Intent) {
        //showFragment(UiFragment.FRAGMENT_SEARCH, false);
        val from = intent.getParcelableExtra<City>("from")
        val to = intent.getParcelableExtra<City>("to")

        val date = LocalDate.from(Instant.ofEpochMilli(intent.getLongExtra("when", 0)).atZone(LocaleUtil.getLocalTimezone()))
        val highlights = intent.getIntArrayExtra("highlights")

        val route = Route(from, to)
        EventBus.getDefault().postSticky(Events.NewSearchEvent(route, date, highlights))
        EventBus.getDefault().postSticky(Events.SearchFillWithRoute(route, date, true))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        //drawerLayoutToggle.onConfigurationChanged(newConfig);
        val appLocale = LocaleUtil.getLocale()
        if (newConfig.locale !== appLocale) {
            newConfig.locale = appLocale
            super.onConfigurationChanged(newConfig)
            Locale.setDefault(appLocale)
            baseContext.resources.updateConfiguration(newConfig, baseContext.resources.displayMetrics)
        } else {
            super.onConfigurationChanged(newConfig)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_AUTHORIZE_MYRIDES || requestCode == REQUEST_CODE_AUTHORIZE_NEWRIDE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //showFragment(UiFragment.FRAGMENT_SEARCH, false);
            } else if (resultCode == Activity.RESULT_OK) {
                /*
                if (requestCode == REQUEST_CODE_AUTHORIZE_MYRIDES)
                    showFragment(UiFragment.FRAGMENT_MY_RIDES, false);
                else {
                    Intent i = new Intent(this, NewRideActivity.class);
                    startActivity(i);
                }*/
            }
        }
    }

    /*
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG) as SearchResultsFragment
        if (fragment != null && fragment.showingResults()) {
            EventBus.getDefault().post(Events.ClearSearchEvent())
        } else {
            super.onBackPressed()
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
    } */

    fun onEventMainThread(e: Events.ShowMessage) {
        ViewUtils.showMessage(this, e.getMessage(this), e.isError)
        EventBus.getDefault().removeStickyEvent(e)
    }

    companion object {
        @JvmField val REQUEST_CODE_AUTHORIZE_MYRIDES = 100
        @JvmField val REQUEST_CODE_AUTHORIZE_NEWRIDE = 101

        private val SEARCH_FRAGMENT_TAG = "SearchResultsFragment"
        private val PUSH_NOTIFICATIONS_FRAGMENT_TAG = "PushNotificationsFragment"
        private val MY_RIDES_FRAGMENT_TAG = "MyRidesFragment"

        private val PREF_SHOWN_LOGIN_PROMPT = "Prevoz.LoginPromptShown"
    }
}
