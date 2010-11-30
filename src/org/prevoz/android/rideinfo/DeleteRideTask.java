package org.prevoz.android.rideinfo;

import java.io.IOException;

import org.prevoz.android.Globals;
import org.prevoz.android.util.HTTPHelper;

import android.os.Handler;
import android.util.Log;

public class DeleteRideTask implements Runnable
{
	private int rideID;
	private Handler callback;

	public DeleteRideTask()
	{

	}

	public void startTask(int rideID, Handler callback)
	{
		this.rideID = rideID;
		this.callback = callback;

		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	public void run()
	{
		try
		{
			String response = HTTPHelper.httpGet(Globals.API_URL
					+ "/carshare/delete/" + rideID + "/");
			Log.i(this.toString(), response);
		}
		catch (IOException e)
		{
		}

		callback.sendEmptyMessage(Globals.REQUEST_SUCCESS);
	}
}
