package org.prevoz.android.search

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

import com.hannesdorfmann.mosby.mvp.MvpFragment
import com.hannesdorfmann.mosby.mvp.MvpView
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import org.prevoz.android.PrevozFragment
import org.prevoz.android.R
import org.prevoz.android.events.Events
import org.prevoz.android.model.City
import org.prevoz.android.model.CityNameTextValidator
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.model.Route
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.PrevozActivity
import org.prevoz.android.util.StringUtil
import org.prevoz.android.util.ViewUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import de.greenrobot.event.EventBus
import icepick.Icepick
import icepick.State
import rx.schedulers.Schedulers

class SearchFragment : MvpFragment<MvpView, SearchFormPresenter>(), DatePickerDialog.OnDateSetListener {


    @BindView(R.id.search_date_edit)
    var searchDate: EditText? = null
    @BindView(R.id.search_from)
    var searchFrom: MaterialAutoCompleteTextView? = null
    @BindView(R.id.search_to)
    var searchTo: MaterialAutoCompleteTextView? = null
    @BindView(R.id.search_button)
    var searchButton: View? = null

    @BindView(R.id.search_button_text)
    var searchButtonText: TextView? = null
    @BindView(R.id.search_button_img)
    var searchButtonImage: ImageView? = null
    @BindView(R.id.search_button_progress)
    var searchButtonProgress: ProgressBar? = null

    override fun createPresenter(): SearchFormPresenter {
        return SearchFormPresenter((activity as PrevozActivity).applicationComponent)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val views = inflater!!.inflate(R.layout.fragment_search, container, false)
        ButterKnife.bind(this, views)
        return views
    }

    protected fun setupViews(database: PrevozDatabase) {
        // Handle input action for next on to
        searchTo!!.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                onDateClicked()
                searchTo!!.clearFocus()
                searchButton!!.requestFocus()
                return@searchTo.setOnEditorActionListener true
            }

            false
        }

        searchFrom!!.validator = CityNameTextValidator(activity, database)
        searchTo!!.validator = CityNameTextValidator(activity, database)

        val fromAdapter = CityAutocompleteAdapter(activity, database)
        val toAdapter = CityAutocompleteAdapter(activity, database)
        searchFrom!!.setAdapter(fromAdapter)
        searchTo!!.setAdapter(toAdapter)
    }

    @OnClick(R.id.search_date)
    fun onDateClicked() {
        val activity = activity
        if (activity == null || !isAdded) return
    }

    // This is duplicated to allow clicking on logo or edittext
    @OnClick(R.id.search_date_edit)
    fun onDateEditClicked() {
        onDateClicked()
    }

    @OnClick(R.id.search_button)
    fun onSearchClicked() {
        ViewUtils.hideKeyboard(activity)
        presenter.from = StringUtil.splitStringToCity(searchFrom!!.text.toString())
        presenter.to = StringUtil.splitStringToCity(searchTo!!.text.toString())
        presenter.search()
    }

    private fun updateSearchButtonProgress(progressShown: Boolean) {
        if (!isAdded) return
        searchButton!!.isEnabled = !progressShown
        searchButtonImage!!.visibility = if (progressShown) View.INVISIBLE else View.VISIBLE
        searchButtonProgress!!.visibility = if (progressShown) View.VISIBLE else View.INVISIBLE
        searchButtonText!!.text = if (progressShown) getString(R.string.search_form_button_searching) else getString(R.string.search_form_button_search)
    }

    /*
    public void onEventMainThread(Events.SearchComplete e)
    {
        updateSearchButtonProgress(false);
    }

    public void onEventMainThread(Events.SearchFillWithRoute e)
    {
        Route r = e.route;
        if (r.getFrom() == null)
            searchFrom.setText("");
        else
            searchFrom.setText(r.getFrom().toString());

        if (r.getTo() == null)
            searchTo.setText("");
        else
            searchTo.setText(r.getTo().toString());

        if (e.date != null)
        {
            selectedDate = e.date;
            updateShownDate();
        }

        if (e.searchInProgress)
            updateSearchButtonProgress(true);

        EventBus.getDefault().removeStickyEvent(e);
    }*/

    fun showDate(selectedDate: LocalDate) {
        searchDate!!.setText(LocaleUtil.localizeDate(resources, selectedDate.atStartOfDay(LocaleUtil.getLocalTimezone())))
    }

    fun showFrom(from: City?) {
        if (from == null) {
            searchFrom!!.setText("")
        } else {
            searchFrom!!.setText(from.toString())
        }
    }

    fun showTo(to: City?) {
        if (to == null) {
            searchTo!!.setText("")
        } else {
            searchTo!!.setText(to.toString())
        }
    }

    fun showDateDialog(selectedDate: LocalDate) {
        val activity = activity ?: return
        ViewUtils.hideKeyboard(activity)
        val dialog = DatePickerDialog.newInstance(this,
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth)
        dialog.show(activity.fragmentManager, "SearchDate")
    }

    override fun onDateSet(datePickerDialog: DatePickerDialog, year: Int, month: Int, day: Int) {
        val selectedDate = LocalDate.of(year, month + 1, day)
        presenter.selectedDate = selectedDate
    }

    fun showLoadingThrobber() {
        searchButton!!.isEnabled = false
        searchButtonImage!!.visibility = View.INVISIBLE
        searchButtonProgress!!.visibility = View.VISIBLE
        searchButtonText!!.text = getString(R.string.search_form_button_searching)
    }
}
