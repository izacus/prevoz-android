package org.prevoz.android.auth;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.util.HTTPHelper;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class ApiKeyLoginTask extends AsyncTask<Handler, Void, AuthenticationStatus>
{
	private String apikey;
	private Handler callback = null;
	
	public ApiKeyLoginTask(String apiKey)
	{
		this.apikey = apiKey;
	}

	@Override
	protected AuthenticationStatus doInBackground(Handler... params)
	{
		// Check if callback for authentication status is passed
		if (params != null && params.length > 0)
		{
			callback = params[0];
		}
		
		Log.i(this.toString(), "Requesting apikey login...");
		
		try
		{
			HashMap<String, String> paramMap = new HashMap<String, String>();
			paramMap.put("apikey", apikey);
			
			String response = HTTPHelper.httpPost(Globals.API_URL + "/accounts/login/apikey", paramMap, true);
			Log.d(this.toString(), response);
			
			JSONObject jsonObj = new JSONObject(response);
			boolean isLoggedIn = jsonObj.getBoolean("is_authenticated");
			
			if (isLoggedIn)
			{
				Log.i(this.toString(), "Apikey login succeeded!");
				
				
				
				return AuthenticationStatus.AUTHENTICATED;
			}
			else
			{
				return AuthenticationStatus.NOT_AUTHENTICATED;
			}
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Apikey login request failed!", e);
			return AuthenticationStatus.NOT_AUTHENTICATED;
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Filed to parse apikey login response!", e);
			return AuthenticationStatus.NOT_AUTHENTICATED;
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
