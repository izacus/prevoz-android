package org.prevoz.android.auth;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.util.HTTPHelper;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class AuthenticationCheckTask extends AsyncTask<Handler, Void, AuthenticationStatus>
{
	private Handler callback = null;
	
	@Override
	protected AuthenticationStatus doInBackground(Handler... params)
	{
		// Check if callback for authentication status is passed
		if (params.length > 0)
		{
			callback = params[0];
		}
		
		Log.i(this.toString(), "Retreving current login status...");
		
		try
		{
			String response = HTTPHelper.httpGet(Globals.API_URL + "/accounts/status/", null);
			
			JSONObject jsonObj = new JSONObject(response);
			boolean isLoggedIn = jsonObj.getBoolean("is_authenticated");

			if (isLoggedIn)
			{
				Log.i(this.toString(), "User is logged in.");
				return AuthenticationStatus.AUTHENTICATED;
			}
			else
			{
				Log.i(this.toString(), "User is not logged in.");
				return AuthenticationStatus.NOT_AUTHENTICATED;
			}
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Failed to retrieve current login status.", e);
			return AuthenticationStatus.UNKNOWN;
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Failed to parse current login status.", e);
			return AuthenticationStatus.UNKNOWN;
		}
	}

	@Override
	protected void onPostExecute(AuthenticationStatus result)
	{
		if (callback != null)
		{
			callback.sendEmptyMessage(result.ordinal());
		}
	}
}
