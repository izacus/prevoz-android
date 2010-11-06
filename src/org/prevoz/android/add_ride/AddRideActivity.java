package org.prevoz.android.add_ride;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.prevoz.android.GPSManager;
import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.RideType;
import org.prevoz.android.rideinfo.Ride;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.search.LocationAutocompleteAdapter;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class AddRideActivity extends Activity
{
    private static AddRideActivity instance;
    public static AddRideActivity getInstance()
    {
	return instance;
    }
    
    private static final int DATEPICKER_DIALOG_ID = 0;
    private static final int TIMEPICKER_DIALOG_ID = 1;
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
    
    private class WebViewController extends WebViewClient
    {
	private ProgressDialog loadingDialog;
	private Context context;
	
	public WebViewController(Context context)
	{
	    this.context = context;
	}
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url)
	{
	    Log.d(this.toString(), "Loading URL " + url);
	    
	    view.loadUrl(url);
	    
	    return true;
	}
	
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon)
	{
	    if (loadingDialog == null || !loadingDialog.isShowing())
	    {
		loadingDialog = ProgressDialog.show(context, null, context.getString(R.string.loading));
	    }
	    
	    Log.i(this.toString(), "Page started " + url);
	    
	    if (url.contains("/login/success"))
	    {
		HTTPHelper.updateSessionCookies(AddRideActivity.getInstance());
		checkLoginStatus();
		
		if (loadingDialog != null && loadingDialog.isShowing())
		{
		    try
		    {
			loadingDialog.dismiss();
		    }
		    catch(IllegalArgumentException e)
		    {
			loadingDialog = null;
		    }
		}
	    }
	    
	    super.onPageStarted(view, url, favicon);
	}

	@Override
	public void onPageFinished(WebView view, String url)
	{
	    try
	    {
		if (loadingDialog != null && loadingDialog.isShowing())
		{
		    loadingDialog.dismiss();
		}
	    }
	    catch (IllegalArgumentException e)
	    {
		loadingDialog = null;
	    };
	}
	
    }
    
    private class PeopleSpinnerObject
    {
	public int number;
	
	public PeopleSpinnerObject(int number)
	{
	    this.number = number;
	}
	
	public int getNumber()
	{
	    return number;
	}
	
	public String toString()
	{
	    return number + " " + LocaleUtil.getStringNumberForm(getResources(), R.array.people_tags, number);
	}
    }
    
    private enum AddViews
    {
	LOGIN,
	FORM,
	PREVIEW
    }
    
    // Current view
    private AddViews currentView;
    
    // Private fields
    private Calendar selectedDate;
    private GPSManager activeManager = null;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	AddRideActivity.instance = this;
	setContentView(R.layout.add_ride_activity);
	
	selectedDate = Calendar.getInstance();
	// Reset time to some sane value
	selectedDate.add(Calendar.HOUR_OF_DAY, 1);
	selectedDate.set(Calendar.MINUTE, 0);
	
	if (savedInstanceState != null)
	{
	    selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date"));
	    prepareAddForm();
	    
	    Ride existing = new Ride(savedInstanceState);
	    
	    ((Spinner)findViewById(R.id.add_type)).setSelection(existing.getType().ordinal());
	    
	    ((AutoCompleteTextView)findViewById(R.id.add_from)).setText(existing.getFrom());
	    ((AutoCompleteTextView)findViewById(R.id.add_to)).setText(existing.getTo());
	    ((EditText)findViewById(R.id.add_phone)).setText(existing.getContact());
	    ((EditText)findViewById(R.id.add_comment)).setText(existing.getComment());
	    ((Spinner)findViewById(R.id.add_ppl)).setSelection(existing.getPeople() - 1);
	    
	    if (existing.getPrice() != null)
	    {
		((EditText)findViewById(R.id.add_price)).setText(String.valueOf(existing.getPrice()));
	    }
	    
	    AddViews view = AddViews.values()[savedInstanceState.getInt("view")];
	    switchView(view);
	}
	else
	{
	    switchView(AddViews.LOGIN);
	}
    }
    
    
    
    @Override
    protected void onResume()
    {
	super.onResume();
	checkLoginStatus();
    }

    /**
     * Request current user's login status 
     */
    private void checkLoginStatus()
    {
	LoginStatusTask checkStatusTask = new LoginStatusTask(this);
	
	// Dispatch response to updateLoginStatus method
	Handler callbackHandler = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		updateLoginStatus(LoginStatus.values()[msg.what]);
	    }
	};
	
	checkStatusTask.start(callbackHandler);
    }
    
    /**
     * Show login dialog if user is not logged in or display ride add status
     * @param status
     */
    private void updateLoginStatus(LoginStatus status)
    {
	switch(status)
	{
		case UNKNOWN:
		    Toast.makeText(this, R.string.server_error, Toast.LENGTH_LONG).show();
		    break;
		    
		case LOGGED_IN:
		    
		    // Store received api key
		    
		    
		    prepareAddForm();
		    switchView(AddViews.FORM);
		    break;
		    
		case NOT_LOGGED_IN:
		    showWebLogin();
		    break;
	}
    }
    
    private void prepareAddForm()
    {	
	updateSelectedDate(selectedDate);
	updateSelectedTime(selectedDate);
	
	// Prepare autocomplete
	LocationAutocompleteAdapter places = new LocationAutocompleteAdapter(this, null);
	
	AutoCompleteTextView fromField = (AutoCompleteTextView)findViewById(R.id.add_from);
	AutoCompleteTextView toField = (AutoCompleteTextView)findViewById(R.id.add_to);
	
	fromField.setAdapter(places);
	toField.setAdapter(places);
	
	fromField.setThreshold(1);
	toField.setThreshold(1);
	
	// Add date callback
	EditText addDateField = (EditText)findViewById(R.id.add_date);
	addDateField.setOnClickListener(new View.OnClickListener()
	{
	    public void onClick(View v)
	    {
		showDialog(DATEPICKER_DIALOG_ID);
	    }
	});
	
	// Add clock callback
	EditText addTimeField = (EditText)findViewById(R.id.add_time);
	addTimeField.setOnClickListener(new View.OnClickListener()
	{
	    public void onClick(View v)
	    {
		showDialog(TIMEPICKER_DIALOG_ID);
	    }
	});
	
	// People list spinner popuation
	Spinner addPpl = (Spinner)findViewById(R.id.add_ppl);
	
	PeopleSpinnerObject[] peopleSpinnerObject = new PeopleSpinnerObject[6];
	for (int i = 0; i < 6; i++)
	    peopleSpinnerObject[i] = new PeopleSpinnerObject(i + 1);
	
	ArrayAdapter<PeopleSpinnerObject> peopleAdapter = new ArrayAdapter<PeopleSpinnerObject>(this, android.R.layout.simple_spinner_item, peopleSpinnerObject);
	addPpl.setAdapter(peopleAdapter);
	peopleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	
	// Search type spinner population
	String[] types = new String[] { getString(R.string.add_share), getString(R.string.add_seek) };
	ArrayAdapter<String> typesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, types);
	typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	((Spinner)findViewById(R.id.add_type)).setAdapter(typesAdapter);
	
	// Add next button callback
	Button nextButton = (Button)findViewById(R.id.add_button);
	nextButton.setOnClickListener(new View.OnClickListener()
	{
	    
	    public void onClick(View v)
	    {
		showPreview();
	    }
	});
	
	// Prepare GPS button callbacks
	((ImageButton)findViewById(R.id.add_gps_from)).setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		fillInGPS(R.id.add_from);
	    }
	});
	
	((ImageButton)findViewById(R.id.add_gps_to)).setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		fillInGPS(R.id.add_to);
	    }
	});
    }
    
    /**
     * Validates ride information
     * @return true if the ride is valid, false otherwise
     */
    private boolean validateRide(Ride ride)
    {
	// Check origin and destination
	if (ride.getFrom().trim().length() == 0 || ride.getTo().trim().length() == 0)
	{
	    Toast.makeText(this, R.string.add_missing_loc, Toast.LENGTH_LONG).show();
	    return false;
	}
	
	// Check phone number
	if (ride.getContact().trim().length() == 0)
	{
	    Toast.makeText(this, R.string.add_missing_phone, Toast.LENGTH_LONG).show();
	    return false;
	}
	
	return true;
    }
    
    private void showPreview()
    {
	// Get entered data
	final Ride ride = getEnteredRide();
	
	if (!validateRide(ride))
	    return;
	
	// Hide call and SMS buttons
	((Button)findViewById(R.id.rideinfo_call)).setVisibility(View.INVISIBLE);
	((Button)findViewById(R.id.rideinfo_sms)).setVisibility(View.INVISIBLE);
	
	// Populate preview view
	((TextView)findViewById(R.id.rideinfo_from)).setText(ride.getFrom());
	((TextView)findViewById(R.id.rideinfo_to)).setText(ride.getTo());
	((TextView)findViewById(R.id.rideinfo_time)).setText(timeFormatter.format(ride.getTime()));
	((TextView)findViewById(R.id.rideinfo_day)).setText(LocaleUtil.getDayName(getResources(), ride.getTime()) + ",");
	((TextView)findViewById(R.id.rideinfo_date)).setText(LocaleUtil.getFormattedDate(getResources(), ride.getTime()));
	
	if (ride.getPrice() == null)
	{
	    ((TextView)findViewById(R.id.rideinfo_price)).setText("?");
	}
	else
	{
	    ((TextView)findViewById(R.id.rideinfo_price)).setText(String.format("%1.1f €", ride.getPrice()));
	}
	((TextView)findViewById(R.id.rideinfo_people)).setText(String.valueOf(ride.getPeople()));
	((TextView)findViewById(R.id.rideinfo_peopletag)).setText(LocaleUtil.getStringNumberForm(getResources(), R.array.people_tags, ride.getPeople()));
	
	((TextView)findViewById(R.id.rideinfo_comment)).setText(ride.getComment());
	((TextView)findViewById(R.id.rideinfo_phone)).setText(ride.getContact());
	
	// Show post button
	Button postButton = (Button)findViewById(R.id.rideinfo_delsend);
	postButton.setText(getString(R.string.add_send));
	postButton.setVisibility(View.VISIBLE);
	
	postButton.setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		postRide(ride);
	    }
	});
	
	switchView(AddViews.PREVIEW);
    }
    
    private void postRide(Ride ride)
    {
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	
	HashMap<String, String> parameters = new HashMap<String, String>();
	parameters.put("transptype", String.valueOf(ride.getType().ordinal()));
	parameters.put("transpfrom", ride.getFrom());
	parameters.put("transpfromcountry", "SI");
	parameters.put("transpto", ride.getTo());
	parameters.put("transptocountry", "SI");
	
	parameters.put("transpdate", dateFormatter.format(ride.getTime()));
	parameters.put("transptime", timeFormatter.format(ride.getTime()));
	
	parameters.put("transpppl", String.valueOf(ride.getPeople()));
	
	if (ride.getPrice() != null)
	{
	    parameters.put("transpprice", String.valueOf(ride.getPrice()));
	}
	else
	{
	    parameters.put("transpprice", "");
	}
	
	parameters.put("transpphone", ride.getContact());
	parameters.put("transpdescr", ride.getComment());
	
	
	final ProgressDialog sendProgress = ProgressDialog.show(this, null, getString(R.string.add_sending));
	final SendRideTask task = new SendRideTask();
	
	Handler sendCallback = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		sendProgress.dismiss();
		rideSent(msg.what, task);
	    }
	};
	
	sendProgress.show();
	task.startTask(parameters, sendCallback);
    }
    
    private void rideSent(int status, SendRideTask doneTask)
    {
	switch(status)
	{
		case SendRideTask.SERVER_ERROR:
		    Toast.makeText(this, R.string.server_error, Toast.LENGTH_LONG).show();
		    break;
		
		case SendRideTask.SEND_ERROR:
		    Toast.makeText(this, doneTask.getErrorMessage(), Toast.LENGTH_LONG).show();
		    switchView(AddViews.FORM);
		    break;
		  
		case SendRideTask.SEND_SUCCESS:
		    switchView(AddViews.FORM);
		    Intent intent = new Intent(this, RideInfoActivity.class);
		    intent.putExtra(RideInfoActivity.RIDE_ID, doneTask.getRideId());
		    startActivity(intent);
		    break;
	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
	
	
	switch(id)
	{
		case DATEPICKER_DIALOG_ID:
		    
		    OnDateSetListener datePicked = new OnDateSetListener()
		    {
		        public void onDateSet(DatePicker view, 
		        		      int year, 
		        		      int monthOfYear,
		        		      int dayOfMonth)
		        {
		            Calendar selectedDate = Calendar.getInstance();
		            selectedDate.set(Calendar.YEAR, year);
		            selectedDate.set(Calendar.MONTH, monthOfYear);
		            selectedDate.set(Calendar.DATE, dayOfMonth);
		            updateSelectedDate(selectedDate);
		        }
		    };
		    
		    return new DatePickerDialog(this, 
			    			datePicked, 
			    			selectedDate.get(Calendar.YEAR), 
			    			selectedDate.get(Calendar.MONTH), 
			    			selectedDate.get(Calendar.DATE));
		case TIMEPICKER_DIALOG_ID:
		    OnTimeSetListener timePicked = new OnTimeSetListener()
		    {
		        public void onTimeSet(TimePicker view, int hourOfDay, int minute)
		        {
		    		Calendar selectedTime = Calendar.getInstance();
		    		selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
		    		selectedTime.set(Calendar.MINUTE, minute);
		    		updateSelectedTime(selectedTime);
		        }
		    };
		    
		    return new TimePickerDialog(this, 
			    			timePicked, 
			    			selectedDate.get(Calendar.HOUR_OF_DAY), 
			    			selectedDate.get(Calendar.MINUTE), true);
	}
	
	return super.onCreateDialog(id);
    }

    private void updateSelectedDate(Calendar newDate)
    {
	EditText addDateField = (EditText)findViewById(R.id.add_date);
	selectedDate.set(newDate.get(Calendar.YEAR), newDate.get(Calendar.MONTH), newDate.get(Calendar.DATE));
	addDateField.setText(LocaleUtil.getDayName(getResources(), selectedDate) + ", " + LocaleUtil.getFormattedDate(getResources(), selectedDate));
    }
    
    private void updateSelectedTime(Calendar newTime)
    {
	EditText addTimeField = (EditText)findViewById(R.id.add_time);
	selectedDate.set(Calendar.HOUR_OF_DAY, newTime.get(Calendar.HOUR_OF_DAY));
	selectedDate.set(Calendar.MINUTE, newTime.get(Calendar.MINUTE));
	
	addTimeField.setText(timeFormatter.format(newTime.getTime()));
    }
    
    /**
     * Shows user the login form
     */
    private void showWebLogin()
    {
	WebView view = (WebView)findViewById(R.id.webview);
	view.setWebViewClient(new WebViewController(this));
	
	view.loadUrl(Globals.LOGIN_URL);
	
	switchView(AddViews.LOGIN);
    }
    
    /**
     * Extracts ride form fields
     * @return Ride object with populated data
     */
    private Ride getEnteredRide()
    {
	RideType type;
	
	if (((Spinner)findViewById(R.id.add_type)).getSelectedItemPosition() == 0)
	{
	    type = RideType.SHARE;
	}
	else
	{
	    type = RideType.SEEK;
	}
	
	String from = ((AutoCompleteTextView)findViewById(R.id.add_from)).getText().toString();
	String to = ((AutoCompleteTextView)findViewById(R.id.add_to)).getText().toString();
	
	// Get price and check if it's empty
	Double price;
	EditText priceText = (EditText)findViewById(R.id.add_price);
	
	String priceString = StringUtil.numberOnly(priceText.getText().toString().trim(), true);
	
	if (priceString.length() == 0)
	{
	    price = null;
	}
	else 
	{
	    price = Double.parseDouble(priceString);
	}
	
	// Get number of people count
	Spinner numPeopleSpinner = (Spinner)findViewById(R.id.add_ppl);
	PeopleSpinnerObject psObject = (PeopleSpinnerObject)numPeopleSpinner.getSelectedItem();
	
	int numPeople = (psObject == null ? 0 : psObject.getNumber());
	
	Ride ride = new Ride(-1,
			     type,
			     from,
			     to,
			     selectedDate.getTime(),
			     numPeople,
			     price,
			     null,
			     StringUtil.numberOnly(((EditText)findViewById(R.id.add_phone)).getText().toString(), false),
			     ((EditText)findViewById(R.id.add_comment)).getText().toString(),
			     true);
	
	return ride;
    }
    
    private void switchView(AddViews view)
    {
	if (currentView == view)
	    return;
	
	ViewFlipper addFlipper = (ViewFlipper)findViewById(R.id.add_flipper);
	addFlipper.setDisplayedChild(view.ordinal());
	currentView = view;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
	if (keyCode == KeyEvent.KEYCODE_BACK && currentView == AddViews.PREVIEW)
	{
	    switchView(AddViews.FORM);
	    return true;
	}
	
	return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
	// Store current instance state
	outState.putLong("selected_date", selectedDate.getTimeInMillis());
	
	Ride ride = getEnteredRide();
	ride.storeToBundle(outState);
	
	// Store current view
	outState.putInt("view", currentView.ordinal());
	
	if (activeManager != null)
	    activeManager.cancelSearch();
    }
    
    private void fillInGPS(int field)
    {
	final AutoCompleteTextView fillField = (AutoCompleteTextView)findViewById(field);
	
	// Disable GPS buttons
	((ImageButton)findViewById(R.id.add_gps_to)).setEnabled(false);
	((ImageButton)findViewById(R.id.add_gps_from)).setEnabled(false);
	fillField.setEnabled(false);
	
	final String oldHint = fillField.getHint().toString();
	final String oldText = fillField.getText().toString();
	
	fillField.setHint(R.string.searching);
	fillField.setText("");
	
	final GPSManager gpsManager = new GPSManager();
	activeManager = gpsManager;
	
	final AddRideActivity addActivity = this;
	
	Handler callback = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		fillField.setHint(oldHint);
		
		// Re-enable fields
		((ImageButton)findViewById(R.id.add_gps_to)).setEnabled(true);
		((ImageButton)findViewById(R.id.add_gps_from)).setEnabled(true);
		fillField.setEnabled(true);
		
		if (msg.what == GPSManager.GPS_PROVIDER_UNAVALABLE)
		{
		    Toast.makeText(addActivity, R.string.gps_error, Toast.LENGTH_LONG).show();
		    fillField.setText(oldText);
		}
		else if (msg.what == GPSManager.GPS_LOCATION_OK)
		{
		    fillField.setText(gpsManager.getCurrentCity());
		}
		
		activeManager = null;
	    }
	};
	
	gpsManager.findCurrentCity(this, callback);
    }
}

