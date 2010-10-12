package org.prevoz.android.add_ride;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.RideType;
import org.prevoz.android.rideinfo.Ride;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
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
    
    private LoginStatus loginStatus;
    
    private class WebViewController extends WebViewClient
    {
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url)
	{
	    if (url.contains("/login/success"))
	    {
		CookieSyncManager.createInstance(AddRideActivity.getInstance());
		CookieManager cookieManager = CookieManager.getInstance();
		
		// Get newly received session cookies in header form
		String cookies = cookieManager.getCookie("http://prevoz.org");
		
		// Store them to HTTP client
		new HTTPHelper(instance).setSessionCookies(cookies);
		checkLoginStatus();
	    }
	    else
	    {
		view.loadUrl(url);
	    }
	    
	    return true;
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	AddRideActivity.instance = this;
	
	loginStatus = LoginStatus.UNKNOWN;
	setContentView(R.layout.add_ride_activity);
	switchView(AddViews.LOGIN);
	
	// Check if user is logged in
	//checkLoginStatus();
	updateLoginStatus(LoginStatus.LOGGED_IN);
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
	// TODO: remove!
	status = LoginStatus.LOGGED_IN;
	
	switch(status)
	{
		case UNKNOWN:
		    Toast.makeText(this, R.string.server_error, Toast.LENGTH_LONG).show();
		    break;
		    
		case LOGGED_IN:
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
	// Prepare date selector
	updateSelectedDate(Calendar.getInstance());
	
	// Prepare autocomplete
	ArrayAdapter<String> places = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, Globals.locations);
	
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
	
	// Add next button callback
	Button nextButton = (Button)findViewById(R.id.add_button);
	nextButton.setOnClickListener(new View.OnClickListener()
	{
	    
	    public void onClick(View v)
	    {
		showPreview();
	    }
	});
    }
    
    private void showPreview()
    {
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	
	// Get entered data
	Ride ride = getEnteredRide();
	
	// TODO: validate
	
	// Populate preview view
	((TextView)findViewById(R.id.rideinfo_from)).setText(ride.getFrom());
	((TextView)findViewById(R.id.rideinfo_to)).setText(ride.getTo());
	((TextView)findViewById(R.id.rideinfo_time)).setText(timeFormatter.format(ride.getTime()));
	((TextView)findViewById(R.id.rideinfo_day)).setText(LocaleUtil.getDayName(getResources(), ride.getTime()));
	((TextView)findViewById(R.id.rideinfo_date)).setText(LocaleUtil.getFormattedDate(getResources(), ride.getTime()));
	
	((TextView)findViewById(R.id.rideinfo_price)).setText(String.format("%1.1f €", ride.getPrice()));
	((TextView)findViewById(R.id.rideinfo_people)).setText(String.valueOf(ride.getPeople()));
	
	// TODO people tag
	
	((TextView)findViewById(R.id.rideinfo_comment)).setText(ride.getComment());
	((TextView)findViewById(R.id.rideinfo_phone)).setText(ride.getContact());
	
	switchView(AddViews.PREVIEW);
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
	}
	
	return super.onCreateDialog(id);
    }

    private void updateSelectedDate(Calendar newDate)
    {
	EditText addDateField = (EditText)findViewById(R.id.add_date);
	selectedDate = newDate;
	addDateField.setText(LocaleUtil.getDayName(getResources(), selectedDate) + ", " + LocaleUtil.getFormattedDate(getResources(), selectedDate));
    }
    
    /**
     * Shows user the login form
     */
    private void showWebLogin()
    {
	ViewFlipper addFlipper = (ViewFlipper)findViewById(R.id.add_flipper);
	addFlipper.showPrevious();
	
	WebView view = (WebView)findViewById(R.id.webview);
	view.setWebViewClient(new WebViewController());
	view.loadUrl(Globals.LOGIN_URL);
    }
    
    /**
     * Extracts ride form fields
     * @return Ride object with populated data
     */
    private Ride getEnteredRide()
    {
	RideType type;
	
	if (((RadioGroup)findViewById(R.id.add_type)).getCheckedRadioButtonId() == R.id.add_share)
	{
	    type = RideType.SHARE;
	}
	else
	{
	    type = RideType.SEEK;
	}
	
	// TODO
	Date time = Calendar.getInstance().getTime(); 
	
	Ride ride = new Ride(-1,
			     type,
			     ((AutoCompleteTextView)findViewById(R.id.add_from)).getText().toString(),
			     ((AutoCompleteTextView)findViewById(R.id.add_to)).getText().toString(),
			     time,
			     Integer.parseInt(((EditText)findViewById(R.id.add_ppl)).getText().toString()),
			     Double.parseDouble(((EditText)findViewById(R.id.add_price)).getText().toString()),
			     null,
			     ((EditText)findViewById(R.id.add_phone)).getText().toString(),
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
}
