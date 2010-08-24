package org.prevoz.android.search;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.RideType;
import org.prevoz.android.util.HTTPUtils;

import android.os.Handler;
import android.util.Log;

public class SearchTask implements Runnable
{   
    private Handler handler = null;
    
    private SearchRequest request = null;
    private SearchResults response = null;
    
    public SearchTask()
    {
    }
    
    
    public void startSearch(SearchRequest request, Handler callback)
    {
	this.request = request;
	this.handler = callback;
	
	Thread thread = new Thread(this);
	thread.setDaemon(true);
	thread.start();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run()
    {
	String responseString = null;
	
	// Get data from HTTP server
	try
	{
	    responseString = HTTPUtils.httpGet(Globals.API_URL + "/search/shares/", 
		    			       HTTPUtils.buildGetParams(request.getParameters()));
	}
	catch(IOException e)
	{
	    Log.e(this.toString(), "Error while requesting search data!", e);
	    handler.sendEmptyMessage(Globals.REQUEST_ERROR_NETWORK);
	    return;
	}
	
	if (responseString == null)
	{
	    handler.sendEmptyMessage(Globals.REQUEST_ERROR_SERVER);
	    return;
	}
	
	// Parse into a response object
	try
	{
	    JSONObject root = new JSONObject(responseString);
	    
	    RideType rideType = root.getString("search_type") == "shares" ? RideType.SHARE : RideType.SEEK;
	    
	    // Request was successful
	    if (root.has("carshare_list"))
	    {
		ArrayList<SearchRide> rides = new ArrayList<SearchRide>();
		JSONArray shareList = root.getJSONArray("carshare_list");
		
		
		for (int i = 0; i < shareList.length(); i++)
		{
		    JSONObject jsonRide = shareList.getJSONObject(i);
		    
		    // ISO8601 date format
		    
		    try
		    {
			SearchRide ride = new SearchRide(jsonRide.getInt("id"), 
			    			     	 jsonRide.getString("from"), 
			    			     	 jsonRide.getString("to"), 
			    			     	 jsonRide.isNull("price") ? null : jsonRide.getDouble("price"), 
			    			         HTTPUtils.parseISO8601(jsonRide.getString("date_iso8601")));
			
			rides.add(ride);
		    }
		    catch (ParseException e)
		    {
			Log.e(this.toString(), "Failed to parse date for ride ID" + jsonRide.getInt("id"), e);
		    }
		}
		
		this.response = new SearchResults(rideType, rides);
	    }
	    else
	    {
		HashMap<String, String> errors = new HashMap<String, String>();
		JSONObject errorList = root.getJSONObject("error");
		
		Iterator<String> keyIterator = errorList.keys();
		
		while(keyIterator.hasNext())
		{
		    String key = keyIterator.next();
		    String value = errorList.getJSONArray(key).getString(0);
		    
		    errors.put(key, value);
		}
		
		this.response = new SearchResults(rideType, errors);
	    }
	}
	catch(JSONException e)
	{
	    Log.e(this.toString(), "Error while parsing JSON response!", e);
	    handler.sendEmptyMessage(Globals.REQUEST_ERROR_SERVER);
	    
	    return;
	}
	
	// Send completion message
	handler.sendEmptyMessage(Globals.REQUEST_SUCCESS);
    }
    
    public SearchResults getResults()
    {
	return response;
    }
}

