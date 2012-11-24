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
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.util.AsyncLoader;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

import com.flurry.android.FlurryAgent;

import android.content.Context;
import android.util.Log;

public class SearchResultsLoader extends AsyncLoader<SearchResults> 
{
	private Context context;
	private SearchRequest searchRequest;
	
	public SearchResultsLoader(Context context, SearchRequest request) 
	{
		super(context);
		this.context = context;
		this.searchRequest = request;
	}

	protected String getResponse() throws IOException
	{
		return HTTPHelper.httpGet(Globals.API_URL + "/search/shares/" + HTTPHelper.buildGetParams(prepareParameters(searchRequest)));
	}
	
	@Override
	public SearchResults loadInBackground()
	{
		
		SearchResults results = null;
		String responseString = null;
		
		// Get data from HTTP server
		try
		{
			responseString = getResponse();
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Error while requesting search data!", e);
			FlurryAgent.onError("SearchInvalid", e.getMessage(), "SearchResultsLoader");
			AuthenticationManager.getInstance().clearAuthCookies(this.context);
			return new SearchResults(prepareError(context.getString(R.string.network_error)));
		}

		if (responseString == null)
		{
			return new SearchResults(prepareError(context.getString(R.string.server_error)));
		}

		// Parse into a response object
		try
		{
			JSONObject root = new JSONObject(responseString);
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

				results = new SearchResults(rides);
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

				results = new SearchResults(errors);
			}
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Error while parsing JSON response!", e);
			results = new SearchResults(prepareError(context.getString(R.string.server_error)));
		}
		
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
		
		if (request.getFrom() != null)
		{
			parameters.put("f", request.getFrom().getDisplayName());
			parameters.put("fc", request.getFrom().getCountryCode());
		}
		
		if (request.getTo() != null)
		{
			parameters.put("t", request.getTo().getDisplayName());
			parameters.put("tc", request.getTo().getCountryCode());
		}
		
		parameters.put("client", "android" + StringUtil.numberOnly(context.getString(R.string.app_version), false));

		// Build date
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		formatter.setTimeZone(LocaleUtil.getLocalTimezone());
		parameters.put("d", formatter.format(request.getWhen().getTime()));

		int search_type = RideType.SHARE.ordinal();
		parameters.put("search_type", String.valueOf(search_type));
		
		return parameters;
	}
	

}
