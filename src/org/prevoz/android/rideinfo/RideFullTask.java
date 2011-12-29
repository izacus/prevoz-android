package org.prevoz.android.rideinfo;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.util.HTTPHelper;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class RideFullTask extends AsyncTask<Boolean, Void, Integer>
{
	public static final int REQUEST_RIDE_FULL = 4;
	public static final int REQUEST_RIDE_NOT_FULL = 5;
	
	private int rideId;
	private Handler callback;
	
	public RideFullTask(int rideId, Handler callback)
	{
		this.callback = callback;
		this.rideId = rideId;
	}
	
	@Override
	protected Integer doInBackground(Boolean... full)
	{
		boolean isFull = full[0];
		
		try
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("state", isFull ? "full" : "available");
			String response = HTTPHelper.httpPost(Globals.API_URL + "/carshare/full/" + rideId + "/", params, false);
			
			try
			{
				JSONObject responseObject = new JSONObject(response);
				
				if (responseObject.has("full"))
				{
					int msg;
					if (responseObject.getBoolean("full"))
					{
						msg = REQUEST_RIDE_FULL;
					}
					else
					{
						msg = REQUEST_RIDE_NOT_FULL;
					}
					
					callback.sendEmptyMessage(msg);
					return msg;
				}
			}
			catch (JSONException e)
			{
				// Fall through to return error statements
			}
			
			callback.sendEmptyMessage(Globals.REQUEST_ERROR_SERVER);
			return Globals.REQUEST_ERROR_SERVER;
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Error while logging out.", e);
			callback.sendEmptyMessage(Globals.REQUEST_ERROR_SERVER);
		}
		
		
		return Globals.REQUEST_ERROR_NETWORK;
	}
}
