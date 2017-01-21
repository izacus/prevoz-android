package org.prevoz.android.search

import android.os.Bundle
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

    @Inject lateinit var database: PrevozDatabase

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
    }

    override fun createPresenter(): SearchFormPresenter {
        return SearchFormPresenter((activity as PrevozActivity).applicationComponent)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val views = inflater!!.inflate(R.layout.fragment_search, container, false)
        ButterKnife.bind(this, views)
        setupViews()
        return views
    }

    fun setupViews() {
        // Handle input action for next on to
        searchTo.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                onDateClicked()
                searchTo.clearFocus()
                searchButton.requestFocus()
                true
            } else {
                false
            }
        }

        searchFrom.validator = CityNameTextValidator(activity, database)
        searchTo.validator = CityNameTextValidator(activity, database)

        val fromAdapter = CityAutocompleteAdapter(activity, database)
        val toAdapter = CityAutocompleteAdapter(activity, database)
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
        searchDate.setText(LocaleUtil.localizeDate(resources, selectedDate.atStartOfDay(LocaleUtil.getLocalTimezone())))
    }

    fun showFrom(from: City?) {
        if (from == null) {
            searchFrom.setText("")
        } else {
            searchFrom.setText(from.toString())
        }
    }

    fun showTo(to: City?) {
        if (to == null) {
            searchTo.setText("")
        } else {
            searchTo.setText(to.toString())
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
