package org.prevoz.android.search;

import java.util.Calendar;
import java.util.Date;

import org.prevoz.android.R;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;

public class SearchActivity extends Activity implements OnDateSetListener
{
    private static final int DIALOG_DATEPICKER_ID = 0;
    
    private Resources resources = null;
    
    // Search form fields
    private EditText searchDateText = null;
    private Calendar selectedDate = null;
    private DatePickerDialog datePickerDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.search_activity);
	resources = getResources();
	
	// Create beginning search date text
	searchDateText = (EditText)findViewById(R.id.dateField);
	selectedDate = Calendar.getInstance();
	searchDateText.setText(localizeDate(selectedDate));
	searchDateText.setOnClickListener(new OnClickListener()
	{
	    
	    @Override
	    public void onClick(View v)
	    {
		showDialog(DIALOG_DATEPICKER_ID);
	    }
	});
	
	datePickerDialog = new DatePickerDialog(this, this, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DATE));
    }
    
    /**
     * Builds a localized date string with day name
     */
    private String localizeDate(Calendar date)
    {
	StringBuilder dateString = new StringBuilder();
	
	String[] dayNames = resources.getStringArray(R.array.day_names);
	dateString.append(dayNames[date.get(Calendar.DAY_OF_WEEK) - 1]);
	dateString.append(", ");
	
	String[] monthNames =  resources.getStringArray(R.array.month_names);
	dateString.append(date.get(Calendar.DATE) + ". " + monthNames[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR));
	
	
	return dateString.toString();
    }

    @Override
    /**
     * Invoked when user selects new date in search form
     * Sets new date in the form.
     */
    public void onDateSet(DatePicker view, 
	    		  int year, 
	    		  int monthOfYear,
	    		  int dayOfMonth)
    {
	selectedDate.set(year, monthOfYear, dayOfMonth);
	
	searchDateText.setText(localizeDate(selectedDate));
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
	switch(id)
	{
		case DIALOG_DATEPICKER_ID:
		    return datePickerDialog;
		default:
		    break;
	}
	
	return super.onCreateDialog(id);
    }

}
