package org.prevoz.android.search

import android.os.Bundle
import android.support.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.hannesdorfmann.mosby.mvp.MvpFragment
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import org.prevoz.android.R
import org.prevoz.android.model.City
import org.prevoz.android.model.CityNameTextValidator
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.PrevozActivity
import org.prevoz.android.util.StringUtil
import org.prevoz.android.util.ViewUtils
import org.threeten.bp.LocalDate
import javax.inject.Inject

class SearchFragment : MvpFragment<SearchFragment, SearchFormPresenter>(), DatePickerDialog.OnDateSetListener {

    @Inject lateinit var localeUtil: LocaleUtil
    @Inject lateinit var database: PrevozDatabase
    @Inject lateinit var cityNameTextValidator: CityNameTextValidator

    @BindView(R.id.search_container)
    lateinit var searchContainer: ViewGroup
    @BindView(R.id.search_date_edit)
    lateinit var searchDate: EditText
    @BindView(R.id.search_from)
    lateinit var searchFrom: MaterialAutoCompleteTextView

    @BindView(R.id.search_to)
    lateinit var searchTo: MaterialAutoCompleteTextView
    @BindView(R.id.search_button)
    lateinit var searchButton: View

    @BindView(R.id.search_button_text)
    lateinit var searchButtonText: TextView
    @BindView(R.id.search_button_img)
    lateinit var searchButtonImage: ImageView
    @BindView(R.id.search_button_progress)
    lateinit var searchButtonProgress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PrevozActivity).applicationComponent.inject(this)
        retainInstance = true
    }

    override fun createPresenter(): SearchFormPresenter {
        return SearchFormPresenter((activity as PrevozActivity).applicationComponent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val views = inflater.inflate(R.layout.fragment_search, container, false)
        ButterKnife.bind(this, views)
        setupViews()
        return views
    }

    private fun setupViews() {
        // Handle input action for next on to
        searchTo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                onDateClicked()
                searchTo.clearFocus()
                searchButton.requestFocus()
                true
            } else {
                false
            }
        }

        searchFrom.validator = cityNameTextValidator
        searchTo.validator = cityNameTextValidator

        val fromAdapter = CityAutocompleteAdapter(activity!!, database, localeUtil)
        val toAdapter = CityAutocompleteAdapter(activity!!, database, localeUtil)
        searchFrom.setAdapter(fromAdapter)
        searchTo.setAdapter(toAdapter)
    }

    @OnClick(R.id.search_date)
    fun onDateClicked() {
        presenter.onDateClicked()
    }

    // This is duplicated to allow clicking on logo or edittext
    @OnClick(R.id.search_date_edit)
    fun onDateEditClicked() {
        onDateClicked()
    }

    @OnClick(R.id.search_button)
    fun onSearchClicked() {
        ViewUtils.hideKeyboard(activity)
        presenter.from = StringUtil.splitStringToCity(searchFrom.text.toString())
        presenter.to = StringUtil.splitStringToCity(searchTo.text.toString())
        presenter.search()
    }

    @OnClick(R.id.search_swap_towns_interceptor)
    fun onLocationIconClicked() {
        TransitionManager.beginDelayedTransition(searchContainer)
        presenter.swapCities()
    }

    fun showDate(selectedDate: LocalDate) {
        TransitionManager.beginDelayedTransition(searchContainer)
        searchDate.setText(localeUtil.localizeDate(selectedDate.atStartOfDay(LocaleUtil.getLocalTimezone())))
    }

    fun showFrom(from: City?) {
        TransitionManager.beginDelayedTransition(searchContainer)
        if (from == null) {
            searchFrom.setText("")
        } else {
            searchFrom.setText(from.toString())
        }

        searchFrom.clearFocus()
    }

    fun showTo(to: City?) {
        TransitionManager.beginDelayedTransition(searchContainer)
        if (to == null) {
            searchTo.setText("")
        } else {
            searchTo.setText(to.toString())
        }

        searchTo.clearFocus()
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
        searchButton.isEnabled = false
        searchButtonImage.visibility = View.INVISIBLE
        searchButtonProgress.visibility = View.VISIBLE
        searchButtonText.text = getString(R.string.search_form_button_searching)
    }

    fun hideLoadingThrobber() {
        searchButton.isEnabled = true
        searchButtonImage.visibility = View.VISIBLE
        searchButtonProgress.visibility = View.INVISIBLE
        searchButtonText.text = getString(R.string.search_form_button_search)
    }
}
