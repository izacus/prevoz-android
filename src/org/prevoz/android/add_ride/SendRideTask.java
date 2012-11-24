package org.prevoz.android.add_ride;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.rideinfo.Ride;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.LocaleUtil;

import android.os.Handler;
import android.util.Log;

public class SendRideTask implements Runnable
{
	public static final int SEND_SUCCESS = 0;
	public static final int SEND_ERROR = 1;
	public static final int SERVER_ERROR = 2;
	public static final int AUTHENTICATION_ERROR = 3;

	private HashMap<String, String> parameters;
	private Handler callback;

	private String errorMessage = "";
	private int createdRideId = 0;

	public SendRideTask(Ride ride)
	{
		parameters = new HashMap<String, String>();
		
		SimpleDateFormat dateFormatter = LocaleUtil.getSimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
		timeFormatter.setTimeZone(LocaleUtil.getLocalTimezone());
		
		parameters.put("transptype", String.valueOf(ride.getType().ordinal()));
		parameters.put("transpfrom", ride.getFrom().getDisplayName());
		parameters.put("transpfromcountry", ride.getFrom().getCountryCode());
		parameters.put("transpto", ride.getTo().getDisplayName());
		parameters.put("transptocountry", ride.getTo().getCountryCode());

		parameters.put("transpdate", dateFormatter.format(ride.getTime()));
		parameters.put("transptime", timeFormatter.format(ride.getTime()));

		parameters.put("transpppl", String.valueOf(ride.getPeople()));
		parameters.put("transpinsured", String.valueOf(ride.isInsured()));

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
	}

	public void startTask(Handler callback)
	{
		this.callback = callback;

		Thread worker = new Thread(this);
		worker.setDaemon(true);
		worker.start();
	}

	public void run()
	{
		// Build parameter list
		String params = HTTPHelper.buildGetParams(parameters);

		try
		{
			String response = HTTPHelper.httpGet(Globals.API_URL
					+ "/carshare/create/", params);

			Log.d(this.toString(), response);
			
			// Check for non-JSON authentication error response
			if (response.equalsIgnoreCase("Forbidden\n"))
			{
				callback.sendEmptyMessage(AUTHENTICATION_ERROR);
				return;
			}
			
			JSONObject jsonO = new JSONObject(response);

			if (jsonO.has("error"))
			{
				JSONObject errorList = jsonO.getJSONObject("error");

				@SuppressWarnings("unchecked")
				Iterator<String> keys = errorList.keys();

				JSONArray errorArray = errorList.getJSONArray(keys.next());
				errorMessage = errorArray.getString(0);

				callback.sendEmptyMessage(SEND_ERROR);
			}
			else
			{
				createdRideId = jsonO.getInt("id");

				callback.sendEmptyMessage(SEND_SUCCESS);
			}
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Error adding new ride with params "
					+ params, e);
			callback.sendEmptyMessage(SERVER_ERROR);
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Error parsing JSON response.", e);
			callback.sendEmptyMessage(SERVER_ERROR);
		}
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public int getRideId()
	{
		return createdRideId;
	}

}
