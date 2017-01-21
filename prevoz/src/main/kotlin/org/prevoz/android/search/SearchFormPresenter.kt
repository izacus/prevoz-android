package org.prevoz.android.search

import com.hannesdorfmann.mosby.mvp.MvpNullObjectBasePresenter
import de.greenrobot.event.EventBus
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.events.Events
import org.prevoz.android.model.City
import org.prevoz.android.model.PrevozDatabase
import org.threeten.bp.LocalDate
import rx.schedulers.Schedulers
import javax.inject.Inject

class SearchFormPresenter(component: ApplicationComponent) : MvpNullObjectBasePresenter<SearchFragment>() {
    init {
        component.inject(this)
    }

    @Inject lateinit var database : PrevozDatabase

    // Currently selected state
    var from : City? = null
    var to : City? = null
    var selectedDate : LocalDate = LocalDate.now()
        set(value) {
            view.showDate(value)
        }

    override fun attachView(view: SearchFragment?) {
        super.attachView(view)
        view?.showFrom(from)
        view?.showTo(to)
        view?.showDate(selectedDate)
        EventBus.getDefault().register(this)
    }

    fun onDateClicked() {
        view.showDateDialog(selectedDate)
    }

    fun search() {
        view.showLoadingThrobber()
        Schedulers.io().createWorker().schedule {
            database.addSearchToHistory(from, to, selectedDate)
            EventBus.getDefault().post(Events.NewSearchEvent(from, to, selectedDate));
        }
    }

    override fun detachView(retainInstance: Boolean) {
        super.detachView(retainInstance)
        EventBus.getDefault().unregister(this)
    }

    fun onEventMainThread(e : Events.SearchComplete) {
        view.hideLoadingThrobber()
    }
}