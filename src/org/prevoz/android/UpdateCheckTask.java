package org.prevoz.android;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.util.HTTPHelper;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

public class UpdateCheckTask implements Runnable
{
	private Activity context;
	private Handler callback;

	private boolean hasNewVersion = false;
	private String message = "";
	private String url = "";

	public UpdateCheckTask()
	{

	}

	public void checkForUpdate(Activity context, Handler callback)
	{
		this.context = context;
		this.callback = callback;
		Thread checkThread = new Thread(this);
		checkThread.setDaemon(true);
		checkThread.run();
	}

	public void run()
	{
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("platform", "android");
		params.put("version", context.getString(R.string.app_version));

		String getParams = HTTPHelper.buildGetParams(params);

		try
		{
			String response = HTTPHelper.httpGet(Globals.API_URL
					+ "/updatecheck", getParams);
			JSONObject responseJSON = new JSONObject(response);

			if (responseJSON.has("is_latest"))
			{
				hasNewVersion = !responseJSON.getBoolean("is_latest");

				if (responseJSON.has("url"))
					url = responseJSON.getString("url");

				if (responseJSON.has("description"))
					message = responseJSON.getString("description");
			}

		}
		catch (IOException e)
		{
			// This check should not be intrusive, so do nothing.
			Log.e(this.toString(), "Error while checking for new version.", e);
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Error while parsing new version.", e);
		}

		callback.sendEmptyMessage(0);
	}

	public boolean hasNewVersion()
	{
		return hasNewVersion;
	}

	public String getUrl()
	{
		return url;
	}

	public String getDescripton()
	{
		return message;
	}
}
