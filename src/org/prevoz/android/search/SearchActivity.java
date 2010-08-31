package org.prevoz.android.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.search.SearchResultAdapter.SearchResultViewWrapper;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.SectionedAdapter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class SearchActivity extends Activity implements OnDateSetListener
{
    private static final int DIALOG_DATEPICKER_ID = 0;
    private static final int DIALOG_LOADING = 1;
    
    private static SearchActivity instance = null;
    
    public static SearchActivity getInstance()
    {
	return instance;
    }
    
    private Resources resources = null;
    
    // Search form fields
    private EditText searchDateText = null;
    private Calendar selectedDate = null;
    private DatePickerDialog datePickerDialog = null;
    
    // Progress for search
    private ProgressDialog searchProgress = null;
    
    // Search resuls
    private SectionedAdapter resultsAdapter = null;
    
    // Search results
    private SearchResults searchResults = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	instance = this;
	
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
	
	// Search button functionality
	Button srchButton = (Button)findViewById(R.id.doSearch);
	srchButton.setOnClickListener(new OnClickListener()
	{
	    @Override
	    public void onClick(View v)
	    {
		startSearch();
	    }
	});
    }
    
    /**
     * Builds a localized date string with day name
     */
    private String localizeDate(Calendar date)
    {
	StringBuilder dateString = new StringBuilder();
	
	dateString.append(LocaleUtil.getDayName(resources, date) + ", ");
	dateString.append(LocaleUtil.getFormattedDate(resources, date));
	
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

    /**
     * Starts search query with populated form data
     */
    private void startSearch()
    {
	showDialog(DIALOG_LOADING);
	
	// Build a search request
	HashMap<String, String> parameters = new HashMap<String, String>();
	
	parameters.put("f", ((EditText)findViewById(R.id.fromField)).getText().toString());
	parameters.put("fc", "SI");
	parameters.put("t", ((EditText)findViewById(R.id.toField)).getText().toString());
	parameters.put("tc", "SI");
	
	// Build date
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	parameters.put("d", formatter.format(selectedDate.getTime()));
	
	// TODO: implement search types
	parameters.put("search_type", "0");
	
	SearchRequest request = new SearchRequest(this, parameters);
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
	if (searchResults.getRides().size() > 0)
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

		    @Override
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
	
	ViewFlipper searchFlipper = (ViewFlipper)findViewById(R.id.search_flipper);
	searchFlipper.showNext();
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
}
