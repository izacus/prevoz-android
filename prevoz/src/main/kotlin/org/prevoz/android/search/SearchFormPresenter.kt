package org.prevoz.android.search

import com.hannesdorfmann.mosby.mvp.MvpPresenter
import de.greenrobot.event.EventBus
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.events.Events
import org.prevoz.android.model.City
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.model.Route
import org.threeten.bp.LocalDate
import rx.schedulers.Schedulers
import javax.inject.Inject

class SearchFormPresenter(component: ApplicationComponent) : MvpPresenter<SearchFragment> {
    init {
        component.inject(this)
    }

    @Inject lateinit var database : PrevozDatabase

    // Currently selected state
    var view: SearchFragment? = null

    var from : City? = null
    var to : City? = null
    var selectedDate : LocalDate = LocalDate.now()
        set(value) {
            view?.showDate(value)
            field = value
        }

    override fun attachView(view: SearchFragment?) {
        this.view = view
        view?.showFrom(from)
        view?.showTo(to)
        view?.showDate(selectedDate)
        EventBus.getDefault().register(this)
    }

    fun onDateClicked() {
        view?.showDateDialog(selectedDate)
    }

    fun swapCities() {
        val tmp = from
        from = to
        to = tmp

        view?.showFrom(from)
        view?.showTo(to)
    }

    fun search() {
        view?.showLoadingThrobber()
        Schedulers.io().createWorker().schedule {
            database.addSearchToHistory(from, to, selectedDate)
            EventBus.getDefault().post(Events.NewSearchEvent(Route(from, to), selectedDate));
        }
    }

    override fun detachView(retainInstance: Boolean) {
        view = null
        EventBus.getDefault().unregister(this)
    }

    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") e : Events.SearchComplete) {
        view?.hideLoadingThrobber()
    }

    fun onEventMainThread(e : Events.SearchFillWithRoute) {
        from = e.route.from
        to = e.route.to
        view?.showFrom(from)
        view?.showTo(to)
    }
}