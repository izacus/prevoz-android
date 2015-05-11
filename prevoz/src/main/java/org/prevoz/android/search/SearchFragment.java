package org.prevoz.android.search;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

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

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import icepick.Icepick;
import icepick.Icicle;
import rx.schedulers.Schedulers;

public class SearchFragment extends Fragment implements DatePickerDialog.OnDateSetListener {
    @InjectView(R.id.search_date_edit)
    protected EditText searchDate;
    @InjectView(R.id.search_from)
    protected MaterialAutoCompleteTextView searchFrom;
    @InjectView(R.id.search_to)
    protected MaterialAutoCompleteTextView searchTo;
    @InjectView(R.id.search_button)
    protected View searchButton;

    @InjectView(R.id.search_button_text)
    protected TextView searchButtonText;
    @InjectView(R.id.search_button_img)
    protected ImageView searchButtonImage;
    @InjectView(R.id.search_button_progress)
    protected ProgressBar searchButtonProgress;

    @Icicle protected Calendar selectedDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View views = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.inject(this, views);

        if (selectedDate == null)
        {
            selectedDate = Calendar.getInstance();
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

        searchFrom.setValidator(new CityNameTextValidator(getActivity()));
        searchTo.setValidator(new CityNameTextValidator(getActivity()));
        setupAdapters();

        return views;
    }

    @OnClick(R.id.search_date)
    protected void onDateClicked()
    {
        FragmentActivity activity = getActivity();
        if (activity == null || !isAdded()) return;

        ViewUtils.hideKeyboard(activity);
        final Calendar calendar = Calendar.getInstance(LocaleUtil.getLocale());

        DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
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
        selectedDate.set(Calendar.YEAR, year);
        selectedDate.set(Calendar.MONTH, month);
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        updateShownDate();
    }

    public void setupAdapters()
    {
        // Initialization requres DB access that's why this is here.
        Activity activity = getActivity();
        if (activity == null) return;   // Happens when fragment is detached

        final CityAutocompleteAdapter fromAdapter = new CityAutocompleteAdapter(activity);
        final CityAutocompleteAdapter toAdapter = new CityAutocompleteAdapter(activity);
        searchFrom.setAdapter(fromAdapter);
        searchTo.setAdapter(toAdapter);
    }

    protected void startSearch()
    {
        Schedulers.io().createWorker().schedule(() -> {
            City fromCity = StringUtil.splitStringToCity(searchFrom.getText().toString());
            City toCity = StringUtil.splitStringToCity(searchTo.getText().toString());

            ContentUtils.addSearchToHistory(getActivity(), fromCity, toCity, selectedDate.getTime());
            EventBus.getDefault().post(new Events.NewSearchEvent(fromCity, toCity, selectedDate));
        });
    }

    private void updateShownDate()
    {
        searchDate.setText(LocaleUtil.localizeDate(getResources(), selectedDate));
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
