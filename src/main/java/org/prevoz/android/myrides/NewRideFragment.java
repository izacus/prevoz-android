package org.prevoz.android.myrides;

import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.googlecode.androidannotations.annotations.*;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import org.prevoz.android.R;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.search.CityAutocompleteAdapter;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.ViewUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@EFragment(R.layout.fragment_newride)
public class NewRideFragment extends Fragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener
{
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", LocaleUtil.getLocale());

    @ViewById(R.id.newride_from)
    protected AutoCompleteTextView textFrom;
    @ViewById(R.id.newride_to)
    protected AutoCompleteTextView textTo;

    @ViewById(R.id.newride_date_edit)
    protected EditText textDate;

    @ViewById(R.id.newride_time_edit)
    protected EditText textTime;

    @ViewById(R.id.newride_price)
    protected EditText textPrice;

    @InstanceState
    protected Calendar setTime;

    @InstanceState
    protected boolean dateSet;

    @InstanceState
    protected boolean timeSet;

    @AfterViews
    protected void initFragment()
    {
        setTime = Calendar.getInstance(LocaleUtil.getLocale());

        // Round time to nearest 30 mins
        if (setTime.get(Calendar.MINUTE) >= 45 || setTime.get(Calendar.MINUTE) <= 15)
            setTime.set(Calendar.MINUTE, 0);
        else
            setTime.set(Calendar.MINUTE, 30);

        SQLiteDatabase db = Database.getSettingsDatabase(getActivity());
        textFrom.setAdapter(new CityAutocompleteAdapter(getActivity(), db));
        textTo.setAdapter(new CityAutocompleteAdapter(getActivity(), db));
        textFrom.setValidator(new CityNameTextValidator(getActivity()));
        textTo.setValidator(new CityNameTextValidator(getActivity()));
    }

    @Click(R.id.newride_date_edit)
    protected void clickDate()
    {
        ViewUtils.hideKeyboard(getActivity());
        DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                                                                setTime.get(Calendar.YEAR),
                                                                setTime.get(Calendar.MONTH),
                                                                setTime.get(Calendar.DAY_OF_MONTH),
                                                                false);
        dialog.show(getActivity().getSupportFragmentManager(), "NewDate");
    }

    @Click(R.id.newride_time_edit)
    protected void clickTime()
    {
        ViewUtils.hideKeyboard(getActivity());
        TimePickerDialog dialog = TimePickerDialog.newInstance(this,
                                                                setTime.get(Calendar.HOUR_OF_DAY),
                                                                setTime.get(Calendar.MINUTE),
                                                                true,
                                                                false);
        dialog.show(getActivity().getSupportFragmentManager(), "NewTime");
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day)
    {
        setTime.set(Calendar.YEAR, year);
        setTime.set(Calendar.MONTH, month);
        setTime.set(Calendar.DAY_OF_MONTH, day);

        dateSet = true;
        textDate.setText(LocaleUtil.getShortFormattedDate(getResources(), setTime));
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int hour, int minute)
    {
        setTime.set(Calendar.HOUR_OF_DAY, hour);
        setTime.set(Calendar.MINUTE, minute);

        timeSet = true;
        textTime.setText(timeFormat.format(setTime.getTime()));
    }
}
