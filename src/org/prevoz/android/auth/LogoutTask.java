package org.prevoz.android.auth;

import java.io.IOException;

import org.prevoz.android.Globals;
import org.prevoz.android.util.HTTPHelper;

import android.os.AsyncTask;
import android.util.Log;

public class LogoutTask extends AsyncTask<Void, Void, Integer>
{
	public LogoutTask()
	{
	}
	
	@Override
	protected Integer doInBackground(Void... params)
	{
		try
		{
			HTTPHelper.httpGet(Globals.API_URL + "/accounts/logout/");
			
			return Globals.REQUEST_SUCCESS;
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Error while logging out.", e);
		}
		
		return Globals.REQUEST_ERROR_NETWORK;
	}
}
