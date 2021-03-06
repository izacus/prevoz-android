package org.prevoz.android

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import butterknife.BindView
import butterknife.ButterKnife
import de.greenrobot.event.EventBus
import org.prevoz.android.events.Events
import org.prevoz.android.model.City
import org.prevoz.android.model.Route
import org.prevoz.android.myrides.MyRidesFragment
import org.prevoz.android.push.PushFragment
import org.prevoz.android.push.PushManager
import org.prevoz.android.search.SearchResultsFragment
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.PrevozActivity
import org.prevoz.android.util.ViewUtils
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import rx.schedulers.Schedulers
import java.util.*

@SuppressLint("Registered")
class MainActivity : PrevozActivity() {

    @BindView(R.id.main_toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.main_tablayout)
    lateinit var tabLayout: TabLayout

    @BindView(R.id.main_pager)
    lateinit var viewPager: ViewPager

    private lateinit var viewPagerAdapter : MainPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val icon = BitmapFactory.decodeResource(resources, R.drawable.icon_ab)
            val color = ContextCompat.getColor(this, R.color.prevoztheme_color_dark)
            val td = ActivityManager.TaskDescription(getString(R.string.app_name), icon, color)
            setTaskDescription(td)
        }

        setSupportActionBar(toolbar)
        setupViewPager()
        tabLayout.setupWithViewPager(viewPager)
    }

    private fun setupViewPager() {
        viewPagerAdapter = MainPagerAdapter(resources, supportFragmentManager)
        viewPager.adapter = viewPagerAdapter
        viewPager.offscreenPageLimit = 3
    }

    class MainPagerAdapter(val resources: Resources,
                           fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
        val searchFragment : SearchResultsFragment = SearchResultsFragment()

        private val myRidesFragment : MyRidesFragment = MyRidesFragment()
        private val notificationsFragment : PushFragment = PushFragment()

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val logoutItem = menu?.findItem(R.id.menu_logout)
        logoutItem?.isEnabled = authUtils.isAuthenticated
        logoutItem?.isVisible = authUtils.isAuthenticated
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.menu_logout -> {
                AlertDialog.Builder(this, R.style.Prevoz_Theme_Dialog)
                        .setTitle("Odjava")
                        .setMessage("Se res želite odjaviti?")
                        .setPositiveButton("Odjavi", { _, _ ->  authUtils.logout().subscribeOn(Schedulers.io()).subscribe()})
                        .setNegativeButton("Prekliči", null)
                        .show()
                return true
            }
        }

        return false
    }

    protected fun triggerSearchFromIntent(intent: Intent) {
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

    override fun onBackPressed() {
        // Back should clear search on first fragment
        if (viewPager.currentItem == 0 && viewPagerAdapter.searchFragment.showingResults()) {
            EventBus.getDefault().post(Events.ClearSearchEvent())
        } else {
            super.onBackPressed()
        }
    }

    @Suppress("unused")
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") e: Events.LoginStateChanged)
    {
        invalidateOptionsMenu()
        EventBus.getDefault().removeStickyEvent(Events.LoginStateChanged::class.java)
    }

    @Suppress("unused")
    fun onEventMainThread(e: Events.ShowMessage) {
        ViewUtils.showMessage(this, e.getMessage(this), e.isError)
        EventBus.getDefault().removeStickyEvent(e)
    }
}
