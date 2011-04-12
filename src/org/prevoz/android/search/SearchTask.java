package org.prevoz.android.search;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.RideType;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.StringUtil;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class SearchTask extends AsyncTask<SearchRequest, Void, SearchResults>
{
	private SearchRequest searchRequest;
	
	public SearchTask()
	{}

	@Override
	protected SearchResults doInBackground(SearchRequest... request) {
		
		SearchResults results = null;
		String responseString = null;

		
		// Parameter sanity check
		if (request.length != 1)
			return null;
		
		searchRequest = request[0];
		
		// Get data from HTTP server
		try
		{
			responseString = HTTPHelper.httpGet(Globals.API_URL + "/search/" + 
					                            (searchRequest.getSearchType() == RideType.SHARE ? "shares/": "seekers/"),
					                            HTTPHelper.buildGetParams(prepareParameters(searchRequest)));
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Error while requesting search data!", e);
			searchRequest.getCallback().sendEmptyMessage(0);
			return new SearchResults(searchRequest.getSearchType(), prepareError(searchRequest.getContext().getString(R.string.network_error)));
		}

		if (responseString == null)
		{
			searchRequest.getCallback().sendEmptyMessage(0);
			return new SearchResults(searchRequest.getSearchType(), prepareError(searchRequest.getContext().getString(R.string.server_error)));
		}

		// Parse into a response object
		try
		{
			JSONObject root = new JSONObject(responseString);

			RideType rideType = root.getString("search_type") == "shares" ? RideType.SHARE
					: RideType.SEEK;

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
														 jsonRide.getString("author"),
														 jsonRide.isNull("price") ? null : jsonRide.getDouble("price"),
														 HTTPHelper.parseISO8601(jsonRide.getString("date_iso8601")));

						rides.add(ride);
					}
					catch (ParseException e)
					{
						Log.e(this.toString(), "Failed to parse date for ride ID" + jsonRide.getInt("id"), e);
					}
				}

				results = new SearchResults(rideType, rides);
			}
			else
			{
				HashMap<String, String> errors = new HashMap<String, String>();
				JSONObject errorList = root.getJSONObject("error");

				@SuppressWarnings("unchecked")
				Iterator<String> keyIterator = errorList.keys();

				while (keyIterator.hasNext())
				{
					String key = keyIterator.next();
					String value = errorList.getJSONArray(key).getString(0);

					errors.put(key, value);
				}

				results = new SearchResults(rideType, errors);
			}
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Error while parsing JSON response!", e);
			results = new SearchResults(searchRequest.getSearchType(), prepareError(searchRequest.getContext().getString(R.string.server_error)));
		}
		
		searchRequest.getCallback().sendEmptyMessage(0);
		return results;
	}
	
	private HashMap<String, String> prepareError(String message)
	{
		HashMap<String, String> errors = new HashMap<String, String>();
		errors.put("error", message);
		return errors;
	}
	
	private HashMap<String, String> prepareParameters(SearchRequest request)
	{
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		parameters.put("f", request.getFrom());
		parameters.put("fc", "SI");
		parameters.put("t", request.getTo());
		parameters.put("tc", "SI");
		parameters.put("client", "android" + StringUtil.numberOnly(request.getContext().getString(R.string.app_version), false));

		// Build date
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		parameters.put("d", formatter.format(request.getWhen().getTime()));

		int search_type = request.getSearchType().ordinal();
		parameters.put("search_type", String.valueOf(search_type));
		
		return parameters;
	}
	
	public void contextChanged(Activity context, Handler callback)
	{
		searchRequest.contextChanged(context, callback);
	}
}
