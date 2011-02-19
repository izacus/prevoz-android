package org.prevoz.android.auth;

import java.io.IOException;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.HTTPHelper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class LogoutTask extends AsyncTask<Void, Void, Integer>
{
	private Context context = null;
	private Toast statusToast = null;
	
	public LogoutTask(Context context)
	{
		this.context = context;
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

	@Override
	protected void onPreExecute()
	{
		statusToast = Toast.makeText(context, context.getString(R.string.logout_progress), Toast.LENGTH_LONG);
		statusToast.show();
	}

	@Override
	protected void onPostExecute(Integer result)
	{
		statusToast.cancel();
	}
}
