package org.prevoz.android.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import org.prevoz.android.GPSManager;
import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.RideType;
import org.prevoz.android.SectionedAdapter;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.search.SearchResultAdapter.SearchResultViewWrapper;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class SearchActivity extends Activity implements OnDateSetListener
{
    public static final String SEARCH_REQUEST = "org.prevoz.android.search.SEARCH_REQUEST";
    public static final String SEARCH_FROM = "org.prevoz.android.search.SEARRCH_FROM";
    public static final String SEARCH_TO = "org.prevoz.android.search.SEARRCH_TO";
    public static final String SEARCH_DATE = "org.prevoz.android.search.SEARRCH_DATE";
    
    private static final int DIALOG_DATEPICKER_ID = 0;
    private static final int DIALOG_LOADING = 1;
    
    private static SearchActivity instance = null;
    
    public static SearchActivity getInstance()
    {
	return instance;
    }
    
    private static enum SearchViews
    {
	SEARCH_FORM,
	SEARCH_RESULTS
    }
    
    private class SearchIntentReceiver extends BroadcastReceiver
    {
	@Override
	public void onReceive(Context context, Intent intent)
	{
	    Log.i(this.toString(), "Received broadcast to start search!");
	 
	    currentView = SearchViews.SEARCH_FORM;
	    ViewFlipper searchFlipper = (ViewFlipper)findViewById(R.id.search_flipper);
	    searchFlipper.setDisplayedChild(0);
	    
	    // Populate search fields
	    String from = intent.getExtras().getString(SEARCH_FROM);
	    String to = intent.getExtras().getString(SEARCH_TO);
	    
	    Calendar cal = Calendar.getInstance();
	    cal.setTimeInMillis(intent.getExtras().getLong(SEARCH_DATE));
	    populateSearchForm(from, to, cal);
	    
	    startSearch();
	}
	
    }
    
    private class SearchTypeSpinnerItem
    {
	private int typeNum;
	private String displayName;
	
	public SearchTypeSpinnerItem(int typeNum, String displayName)
	{
	    this.typeNum = typeNum;
	    this.displayName = displayName;
	}
	
	public String toString()
	{
	    return displayName;
	}
	
	public int type()
	{
	    return typeNum;
	}
    }
    
    private SearchViews currentView;
    
    // Search form fields
    private EditText searchDateText = null;
    private Calendar selectedDate = null;
    private DatePickerDialog datePickerDialog = null;
    
    // Progress for search
    private ProgressDialog searchProgress = null;
    
    // Search results
    private SectionedAdapter resultsAdapter = null;
    
    // Search results
    private SearchResults searchResults = null;
    
    // GPS manager
    private GPSManager activeManager = null;
    
    // Search broadcast receiver
    private final IntentFilter intentFilter = new IntentFilter(SEARCH_REQUEST);
    private SearchIntentReceiver receiver = new SearchIntentReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	instance = this;
	
	setContentView(R.layout.search_activity);
	selectedDate = Calendar.getInstance();
	
	LocationAutocompleteAdapter adapter = new LocationAutocompleteAdapter(this, null);
	
	// Set autocomplete options for locations
	AutoCompleteTextView fromText = (AutoCompleteTextView)findViewById(R.id.fromField);
	AutoCompleteTextView toText = (AutoCompleteTextView)findViewById(R.id.toField);
	
	fromText.setAdapter(adapter);
	toText.setAdapter(adapter);
	
	fromText.setThreshold(1);
	toText.setThreshold(1);
	
	// Populate search type spinner
	SearchTypeSpinnerItem items[] = new SearchTypeSpinnerItem[2];
	
	// Watch out for the search type number, it's switched in API
	items[0] = new SearchTypeSpinnerItem(1, getString(R.string.searching_shares));
	items[1] = new SearchTypeSpinnerItem(0, getString(R.string.searching_seekers));
	
	Spinner searchTypeSpinner = (Spinner)findViewById(R.id.search_type);
	
	ArrayAdapter<SearchTypeSpinnerItem> spinnerAdapter = new ArrayAdapter<SearchActivity.SearchTypeSpinnerItem>(this, 
														    android.R.layout.simple_spinner_item, 
														    items);
	searchTypeSpinner.setAdapter(spinnerAdapter);
	spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	
	// Try to restore state if able
	if (savedInstanceState != null)
	{
	    // Restore set date
	    selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date"));
	    
	    // Re-populate search form
	    populateSearchForm(savedInstanceState.getString("from_loc"), savedInstanceState.getString("to_loc"), selectedDate);
	    
	    // Set current view
	    currentView = SearchViews.values()[savedInstanceState.getInt("current_view")];
	    
	    // Select right search type radio button
	    searchTypeSpinner.setSelection(savedInstanceState.getInt("search_type"));
	}
	else
	{
	    currentView = SearchViews.SEARCH_FORM;
	    searchTypeSpinner.setSelection(0);
	}
	
	// Create beginning search date text
	searchDateText = (EditText)findViewById(R.id.dateField);
	searchDateText.setText(localizeDate(selectedDate));
	searchDateText.setOnClickListener(new OnClickListener() 
	{
	    
	    public void onClick(View v)
	    {
		showDialog(DIALOG_DATEPICKER_ID);
	    }
	});
	
	datePickerDialog = new DatePickerDialog(this, this, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DATE));
	
	// Search button functionality
	Button srchButton = (Button)findViewById(R.id.doSearch);
	srchButton.setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		startSearch();
	    }
	});
	
	// GPS buttons
	((ImageButton)findViewById(R.id.gps_from)).setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		fillInGPS(R.id.fromField);
	    }
	});
	
	((ImageButton)findViewById(R.id.gps_to)).setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		fillInGPS(R.id.toField);
	    }
	});
	
	// Register for receival of search requests
	registerReceiver(receiver, intentFilter);
	
	// Re-get search results if on 2nd view
	if (currentView == SearchViews.SEARCH_RESULTS)
	{
	    startSearch();
	}
    }
    

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
	super.onSaveInstanceState(outState);
	
	// Store currently selected date
	outState.putLong("selected_date", selectedDate.getTimeInMillis());
	
	// Store currently shown view
	outState.putInt("current_view", currentView.ordinal());
	
	// Store typed-in locations
	outState.putString("from_loc", ((TextView)findViewById(R.id.fromField)).getText().toString());
	outState.putString("to_loc", ((TextView)findViewById(R.id.toField)).getText().toString());
	
	// Store currently selected search type
	outState.putInt("search_type", ((Spinner)findViewById(R.id.search_type)).getSelectedItemPosition());
	
	if (activeManager != null)
	    activeManager.cancelSearch();
    }



    @Override
    protected void onDestroy()
    {
	super.onDestroy();
	unregisterReceiver(receiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
	if (currentView == SearchViews.SEARCH_RESULTS && keyCode == KeyEvent.KEYCODE_BACK)
	{
	    ViewFlipper searchFlipper = (ViewFlipper)findViewById(R.id.search_flipper);
	    searchFlipper.setDisplayedChild(0);
	    currentView = SearchViews.SEARCH_FORM;
	    return true;
	}
	    
	return super.onKeyDown(keyCode, event);
    }

    /**
     * Builds a localized date string with day name
     */
    private String localizeDate(Calendar date)
    {
	Resources resources = getResources();
	
	StringBuilder dateString = new StringBuilder();
	
	dateString.append(LocaleUtil.getDayName(resources, date) + ", ");
	dateString.append(LocaleUtil.getFormattedDate(resources, date));
	
	return dateString.toString();
    }

    
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
    /**
     * Create dialog callback
     */
    protected Dialog onCreateDialog(int id)
    {
	switch(id)
	{
		case DIALOG_DATEPICKER_ID:
		    return datePickerDialog;
		    
		case DIALOG_LOADING:
		    searchProgress = new ProgressDialog(this);
		    searchProgress.setMessage(getString(R.string.searching));
		    return searchProgress;
		    
		default:
		    break;
	}
	
	return super.onCreateDialog(id);
    }

    private void populateSearchForm(String from, String to, Calendar date)
    {
	    ((AutoCompleteTextView)findViewById(R.id.fromField)).setText(from);
	    ((AutoCompleteTextView)findViewById(R.id.toField)).setText(to);
	    ((EditText)findViewById(R.id.dateField)).setText(localizeDate(date));
	    selectedDate = date;
    }
    
    /**
     * Starts search query with populated form data
     */
    private void startSearch()
    {
	showDialog(DIALOG_LOADING);
	
	// Build a search request
	HashMap<String, String> parameters = new HashMap<String, String>();
	
	parameters.put("f", ((AutoCompleteTextView)findViewById(R.id.fromField)).getText().toString());
	parameters.put("fc", "SI");
	parameters.put("t", ((AutoCompleteTextView)findViewById(R.id.toField)).getText().toString());
	parameters.put("tc", "SI");
	parameters.put("client", "android" + StringUtil.numberOnly(getString(R.string.app_version), false));
	
	// Build date
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	parameters.put("d", formatter.format(selectedDate.getTime()));
	
	int search_type = ((SearchTypeSpinnerItem)((Spinner)findViewById(R.id.search_type)).getSelectedItem()).type();
	parameters.put("search_type", String.valueOf(search_type));
	
	SearchRequest request = new SearchRequest(this, 
						  search_type == 0 ? RideType.SEEK : RideType.SHARE, 
					          parameters);
	final SearchTask task = new SearchTask();
	
	Handler searchHandler = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		switch(msg.what)
		{
			case Globals.REQUEST_SUCCESS:
			    SearchActivity.getInstance().setSearchResults(task.getResults());
			    SearchActivity.getInstance().buildResultsList();
			break;
			
			case Globals.REQUEST_ERROR_SERVER:
			    Toast.makeText(SearchActivity.getInstance(), R.string.server_error, Toast.LENGTH_LONG).show();
			break;
			
			case Globals.REQUEST_ERROR_NETWORK:
			    Toast.makeText(SearchActivity.getInstance(), R.string.network_error, Toast.LENGTH_LONG).show();
			break;
		}
		
		searchProgress.dismiss();
	    }
	    
	};
	
	task.startSearch(request, searchHandler); 
    }
    
    private void buildResultsList()
    {
	resultsAdapter = new SectionedAdapter()
	{
	    
	    @Override
	    protected View getHeaderView(String caption, 
		    			 int index, 
		    			 View convertView,
		    			 ViewGroup parent)
	    {
		TextView result = (TextView)convertView;
		
		if (convertView == null)
		{
		    result = (TextView)getLayoutInflater().inflate(R.layout.list_header, null);
		}

		result.setText(caption);

		return result;
	    }
	};
	
	ListView resultsList = (ListView)findViewById(R.id.list_results);
	
	// Build categories
	if (searchResults.getRides() != null && searchResults.getRides().size() > 0)
	{
        	// Put rides into buckets by paths
        	HashMap<String, ArrayList<SearchRide>> ridesByPath = new HashMap<String, ArrayList<SearchRide>>();
        	for (SearchRide ride : searchResults.getRides())
        	{
        	    String path = ride.getFrom() + " - " + ride.getTo();
        	    
        	    if (ridesByPath.get(path) == null)
        		ridesByPath.put(path, new ArrayList<SearchRide>());
        	    
        	    ridesByPath.get(path).add(ride);
        	}
        	
        	ArrayList<String> ridePaths = new ArrayList<String>(ridesByPath.keySet());
        	Collections.sort(ridePaths);
        	
        	for (String path : ridePaths)
        	{
        	    resultsAdapter.addSection(path, new SearchResultAdapter(SearchActivity.getInstance(), ridesByPath.get(path)));
        	}
        	
        	resultsList.setAdapter(resultsAdapter);
        	
        	// Clicked item activates RideInfo activity
        	resultsList.setOnItemClickListener(new OnItemClickListener()
		{

		    public void onItemClick(AdapterView<?> parent, 
			    		    View view,
			    		    int position, 
			    		    long id)
		    {
			SearchResultViewWrapper viewWrapper = (SearchResultViewWrapper)view.getTag();
			Intent intent = new Intent(SearchActivity.getInstance(), RideInfoActivity.class);
			intent.putExtra(RideInfoActivity.RIDE_ID, viewWrapper.getRideId());
			startActivity(intent);
		    }
		});
	}
	else
	{
	    String[] noResults = new String[1];
	    noResults[0] = getString(R.string.search_no_results);
	    
	    ArrayAdapter<String> noResultsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, noResults);
	    resultsList.setAdapter(noResultsAdapter);
	    resultsList.setOnItemClickListener(null);
	}
	
	ViewFlipper searchFlipper = (ViewFlipper)findViewById(R.id.search_flipper);
	searchFlipper.setDisplayedChild(1);
	currentView = SearchViews.SEARCH_RESULTS;
    }
    
    // Getters and setters
    public SearchResults getSearchResults()
    {
        return searchResults;
    }

    private void setSearchResults(SearchResults searchResults)
    {
        this.searchResults = searchResults;
    }
    
    private void fillInGPS(int field)
    {
	final AutoCompleteTextView fillField = (AutoCompleteTextView)findViewById(field);
	
	// Disable GPS buttons
	((ImageButton)findViewById(R.id.gps_to)).setEnabled(false);
	((ImageButton)findViewById(R.id.gps_from)).setEnabled(false);
	fillField.setEnabled(false);
	
	final String oldHint = fillField.getHint().toString(); 
	
	fillField.setHint(R.string.searching);
	
	final GPSManager gpsManager = new GPSManager();
	activeManager = gpsManager;
	
	final SearchActivity searchActivity = this;
	
	Handler callback = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		fillField.setHint(oldHint);
		
		// Re-enable fields
		((ImageButton)findViewById(R.id.gps_to)).setEnabled(true);
		((ImageButton)findViewById(R.id.gps_from)).setEnabled(true);
		fillField.setEnabled(true);
		
		if (msg.what == GPSManager.GPS_PROVIDER_UNAVALABLE)
		{
		    Toast.makeText(searchActivity, R.string.gps_error, Toast.LENGTH_LONG).show();
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
