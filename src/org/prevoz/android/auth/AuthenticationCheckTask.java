package org.prevoz.android.auth;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.util.HTTPHelper;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class AuthenticationCheckTask extends AsyncTask<Handler, Void, AuthenticationCheckTask.AuthCheckResult>
{
    public static class AuthCheckResult
    {
        public AuthenticationStatus status;
        public String username;

        public AuthCheckResult(AuthenticationStatus status, String username)
        {
            this.status = status;
            this.username = username;
        }
    }

	private Handler callback = null;
	private String apiKey = null;
	
	@Override
	protected AuthCheckResult doInBackground(Handler... params)
	{
		// Check if callback for authentication status is passed
		if (params != null && params.length > 0)
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
				// Retrieve passed apikey
				apiKey = jsonObj.getString("apikey");
                String username = jsonObj.getString("username");
                return new AuthCheckResult(AuthenticationStatus.AUTHENTICATED, username);
			}
			else
			{
				Log.i(this.toString(), "User is not logged in.");
				return new AuthCheckResult(AuthenticationStatus.NOT_AUTHENTICATED, null);
			}
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Failed to retrieve current login status.", e);
			return new AuthCheckResult(AuthenticationStatus.UNKNOWN, null);
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Failed to parse current login status.", e);
			return new AuthCheckResult(AuthenticationStatus.UNKNOWN, null);
		}
	}

	@Override
	protected void onPostExecute(AuthCheckResult result)
	{
		if (callback != null)
		{
			callback.sendEmptyMessage(result.status.ordinal());
		}
	}
	
	public String getApiKey()
	{
		return apiKey;
	}
}
