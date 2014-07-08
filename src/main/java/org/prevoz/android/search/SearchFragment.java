package org.prevoz.android.search;

import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import de.greenrobot.event.EventBus;
import org.androidannotations.annotations.*;
import org.prevoz.android.R;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.model.Route;
import org.prevoz.android.util.ContentUtils;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;
import org.prevoz.android.util.ViewUtils;

import java.util.Calendar;

@EFragment(R.layout.fragment_search)
public class SearchFragment extends Fragment implements DatePickerDialog.OnDateSetListener
{
    @ViewById(R.id.search_date_edit)
    protected EditText searchDate;
    @ViewById(R.id.search_from)
    protected AutoCompleteTextView searchFrom;
    @ViewById(R.id.search_to)
    protected AutoCompleteTextView searchTo;
    @ViewById(R.id.search_button)
    protected View searchButton;

    @ViewById(R.id.search_button_text)
    protected TextView searchButtonText;
    @ViewById(R.id.search_button_img)
    protected ImageView searchButtonImage;
    @ViewById(R.id.search_button_progress)
    protected ProgressBar searchButtonProgress;

    @InstanceState
    protected Calendar selectedDate;

    @AfterViews
    protected void initFragment()
    {
        if (selectedDate == null)
        {
            selectedDate = Calendar.getInstance();
            updateShownDate();
        }

        // Setup autocomplete text views
        searchFrom.setAdapter(new CityAutocompleteAdapter(getActivity()));
        searchTo.setAdapter(new CityAutocompleteAdapter(getActivity()));
        searchFrom.setValidator(new CityNameTextValidator(getActivity()));
        searchTo.setValidator(new CityNameTextValidator(getActivity()));

        // Handle input action for next on to
        searchTo.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_NEXT)
                {
                    clickDate();
                    searchTo.clearFocus();
                    searchButton.requestFocus();
                    return true;
                }

                return false;
            }
        });
    }


    @Click(R.id.search_date)
    protected void clickDate()
    {
        ViewUtils.hideKeyboard(getActivity());
        final Calendar calendar = Calendar.getInstance(LocaleUtil.getLocale());
        DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                                                               calendar.get(Calendar.YEAR),
                                                               calendar.get(Calendar.MONTH),
                                                               calendar.get(Calendar.DAY_OF_MONTH),
                                                               false);
        dialog.show(getActivity().getSupportFragmentManager(), "SearchDate");
    }

    // This is duplicated to allow clicking on logo or edittext
    @Click(R.id.search_date_edit)
    protected void clickDateEdit()
    {
        clickDate();
    }

    @Click(R.id.search_button)
    protected void clickSearch()
    {
        updateSearchButtonProgress(true);
        ViewUtils.hideKeyboard(getActivity());
        startSearch();
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day)
    {
        selectedDate.set(Calendar.YEAR, year);
        selectedDate.set(Calendar.MONTH, month);
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        updateShownDate();
    }

    private void startSearch()
    {
        City fromCity = StringUtil.splitStringToCity(searchFrom.getText().toString());
        City toCity = StringUtil.splitStringToCity(searchTo.getText().toString());

        ContentUtils.addSearchToHistory(getActivity(), fromCity, toCity, selectedDate.getTime());
        EventBus.getDefault().post(new Events.NewSearchEvent(fromCity, toCity, selectedDate));
    }

    private void updateShownDate()
    {
        searchDate.setText(LocaleUtil.localizeDate(getResources(), selectedDate));
    }

    private void updateSearchButtonProgress(boolean progressShown)
    {
        searchButton.setEnabled(!progressShown);
        searchButtonImage.setVisibility(progressShown ? View.INVISIBLE : View.VISIBLE);
        searchButtonProgress.setVisibility(progressShown ? View.VISIBLE : View.INVISIBLE);
        searchButtonText.setText(progressShown ? getString(R.string.search_form_button_searching) : getString(R.string.search_form_button_search));
    }

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

        EventBus.getDefault().removeStickyEvent(e);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
    }
}
