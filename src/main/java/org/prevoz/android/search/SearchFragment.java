package org.prevoz.android.search;

import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.googlecode.androidannotations.annotations.*;
import org.prevoz.android.R;
import org.prevoz.android.events.Events;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.util.Calendar;

import de.greenrobot.event.EventBus;

@EFragment(R.layout.fragment_search)
public class SearchFragment extends Fragment implements DatePickerDialog.OnDateSetListener
{
    @ViewById(R.id.search_date_edit)
    protected EditText searchDate;
    @ViewById(R.id.search_from)
    protected AutoCompleteTextView searchFrom;
    @ViewById(R.id.search_to)
    protected AutoCompleteTextView searchTo;

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
        SQLiteDatabase db = Database.getSettingsDatabase(getActivity());
        searchFrom.setAdapter(new CityAutocompleteAdapter(getActivity(), db));
        searchTo.setAdapter(new CityAutocompleteAdapter(getActivity(), db));
    }


    @Click(R.id.search_date)
    protected void clickDate()
    {
        final Calendar calendar = Calendar.getInstance();
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
        EventBus.getDefault().post(new Events.NewSearchEvent(searchFrom.getText().toString(), searchTo.getText().toString(), selectedDate));
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day)
    {
        selectedDate.set(Calendar.YEAR, year);
        selectedDate.set(Calendar.MONTH, month);
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        updateShownDate();
    }


    private void updateShownDate()
    {
        searchDate.setText(LocaleUtil.localizeDate(getResources(), selectedDate));
    }
}
