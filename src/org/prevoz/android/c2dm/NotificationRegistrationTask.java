package org.prevoz.android.c2dm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.util.HTTPHelper;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class NotificationRegistrationTask extends AsyncTask<Void, Void, Boolean> 
{
	private static final String REGISTER_URL = "http://prevoz.org/api/c2dm/register/";

	private NotificationRegistrationRequest request;
	private Handler callback;
	
	public NotificationRegistrationTask(NotificationRegistrationRequest request)
	{
		this.request = request;
	}
	
	public NotificationRegistrationRequest getRequest() {
		return request;
	}

	@Override
	protected Boolean doInBackground(Void... params) 
	{
		String responseString = null;
		try
		{
			Map<String, String> postParams = new HashMap<String, String>();
			postParams.put("registration_id", request.getRegistrationId());
			postParams.put("from", request.getFrom());
			postParams.put("to", request.getTo());
			postParams.put("fromcountry", "SI");
			postParams.put("tocountry", "SI");
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			postParams.put("date", formatter.format(request.getWhen().getTime()));
			postParams.put("action", request.isRegister() ? "subscribe" : "unsubscribe");
			
			
			responseString = HTTPHelper.httpPost(REGISTER_URL, postParams, false);
			
			if (responseString == null)
				return false;
			
			JSONObject root = new JSONObject(responseString);
			if (root.has("status") && root.getString("status").equalsIgnoreCase("success"))
			{
				return true;
			}
			
			return false;
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Failed to (un)register for C2DM: " + e.getMessage(), e);
			return false;
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Corrupted response from register server: " + responseString, e);
			return false;
		}
		finally
		{
			if (callback != null)
			{
				callback.sendEmptyMessage(0);
			}
		}
	}
	
	public void setCallback(Handler callback)
	{
		this.callback = callback;
	}
}
