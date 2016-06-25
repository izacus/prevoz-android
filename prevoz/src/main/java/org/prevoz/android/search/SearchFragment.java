package org.prevoz.android.search;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.prevoz.android.PrevozFragment;
import org.prevoz.android.R;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.model.Route;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.StringUtil;
import org.prevoz.android.util.ViewUtils;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import icepick.Icepick;
import icepick.State;
import rx.schedulers.Schedulers;

public class SearchFragment extends PrevozFragment implements DatePickerDialog.OnDateSetListener {
    @BindView(R.id.search_date_edit)
    protected EditText searchDate;
    @BindView(R.id.search_from)
    protected MaterialAutoCompleteTextView searchFrom;
    @BindView(R.id.search_to)
    protected MaterialAutoCompleteTextView searchTo;
    @BindView(R.id.search_button)
    protected View searchButton;

    @BindView(R.id.search_button_text)
    protected TextView searchButtonText;
    @BindView(R.id.search_button_img)
    protected ImageView searchButtonImage;
    @BindView(R.id.search_button_progress)
    protected ProgressBar searchButtonProgress;

    @State protected LocalDate selectedDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PrevozActivity)getActivity()).getApplicationComponent().inject(this);
        Icepick.restoreInstanceState(this, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View views = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, views);

        if (selectedDate == null)
        {
            selectedDate = LocalDate.now();
        }

        updateShownDate();

        // Handle input action for next on to
        searchTo.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT)
            {
                onDateClicked();
                searchTo.clearFocus();
                searchButton.requestFocus();
                return true;
            }

            return false;
        });

        searchFrom.setValidator(new CityNameTextValidator(getActivity(), database));
        searchTo.setValidator(new CityNameTextValidator(getActivity(), database));
        setupAdapters();

        return views;
    }

    @OnClick(R.id.search_date)
    protected void onDateClicked()
    {
        FragmentActivity activity = getActivity();
        if (activity == null || !isAdded()) return;

        ViewUtils.hideKeyboard(activity);

        DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                                                                selectedDate.getYear(),
                                                                selectedDate.getMonthValue() - 1,
                                                                selectedDate.getDayOfMonth());
        dialog.show(activity.getFragmentManager(), "SearchDate");
    }

    // This is duplicated to allow clicking on logo or edittext
    @OnClick(R.id.search_date_edit)
    protected void onDateEditClicked()
    {
        onDateClicked();
    }

    @OnClick(R.id.search_button)
    protected void onSearchClicked()
    {
        updateSearchButtonProgress(true);
        ViewUtils.hideKeyboard(getActivity());
        startSearch();
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day)
    {
        selectedDate = selectedDate.withYear(year).withMonth(month + 1).withDayOfMonth(day);
        updateShownDate();
    }

    public void setupAdapters()
    {
        // Initialization requres DB access that's why this is here.
        Activity activity = getActivity();
        if (activity == null) return;   // Happens when fragment is detached

        /*final CityAutocompleteAdapter fromAdapter = new CityAutocompleteAdapter(activity, database);
        final CityAutocompleteAdapter toAdapter = new CityAutocompleteAdapter(activity, database);
        searchFrom.setAdapter(fromAdapter);
        searchTo.setAdapter(toAdapter); */
    }

    protected void startSearch()
    {
        Schedulers.io().createWorker().schedule(() -> {
            City fromCity = StringUtil.splitStringToCity(searchFrom.getText().toString());
            City toCity = StringUtil.splitStringToCity(searchTo.getText().toString());

            database.addSearchToHistory(fromCity, toCity, selectedDate);
            EventBus.getDefault().post(new Events.NewSearchEvent(fromCity, toCity, selectedDate));
        });
    }

    private void updateShownDate()
    {
        searchDate.setText(LocaleUtil.localizeDate(getResources(), selectedDate.atStartOfDay(LocaleUtil.getLocalTimezone())));
    }

    private void updateSearchButtonProgress(boolean progressShown)
    {
        if (!isAdded()) return;
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

        if (e.searchInProgress)
            updateSearchButtonProgress(true);

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }
}
