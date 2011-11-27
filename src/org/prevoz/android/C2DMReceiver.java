package org.prevoz.android;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.c2dm.C2DMBaseReceiver;

public class C2DMReceiver extends C2DMBaseReceiver  
{
	public C2DMReceiver()
	{
		super("gandalfar@gmail.com");
	}

	
	
	@Override
	public void onRegistered(Context context, String registrationId)
			throws IOException {
		// TODO Auto-generated method stub
		super.onRegistered(context, registrationId);
	}



	@Override
	public void onUnregistered(Context context) {
		// TODO Auto-generated method stub
		super.onUnregistered(context);
	}



	@Override
	public void onError(Context context, String error) 
	{
		Log.e(this.toString(), "C2DM error: " + error);
	}

	@Override
	protected void onMessage(Context context, Intent intent) 
	{
		Log.i(this.toString(), "C2DM Message received.");
	}
}
