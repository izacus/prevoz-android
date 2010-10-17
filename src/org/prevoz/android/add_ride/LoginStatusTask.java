package org.prevoz.android.add_ride;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.HTTPHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.util.Log;

public class LoginStatusTask implements Runnable
{
    private Activity context;
    
    private Handler callback = null;
    private ProgressDialog statusDialog = null;
    
    public LoginStatusTask(Activity context)
    {
	this.context = context;
    }
    
    public void start(Handler callback)
    {
	this.callback = callback;
	
	Log.i(this.toString(), "Requesting login status...");
	
	statusDialog = ProgressDialog.show(context, null, context.getResources().getString(R.string.checking_login_status));
	
	Thread thread = new Thread(this);
	thread.setDaemon(true);
	thread.start();
    }
    
    public void run()
    {
	try
	{
	    String response = HTTPHelper.httpGet(Globals.API_URL + "/accounts/status/", null);
	    
	    JSONObject jsonObj = new JSONObject(response);
	    boolean isLoggedIn = jsonObj.getBoolean("is_authenticated");
		
	    if (isLoggedIn)
	    {
		callback.sendEmptyMessage(LoginStatus.LOGGED_IN.ordinal());
		Log.i(this.toString(), "User is logged in.");
	    }
	    else
	    {
		callback.sendEmptyMessage(LoginStatus.NOT_LOGGED_IN.ordinal());
		Log.i(this.toString(), "User is not logged in.");
	    }
	}
	// On error send unknown login status to the handler
	catch (IOException e)
	{
	    callback.sendEmptyMessage(LoginStatus.UNKNOWN.ordinal());
	    Log.e(this.toString(), "Error while retrieving login status.", e);
	}
	catch (JSONException e)
	{
	    callback.sendEmptyMessage(LoginStatus.UNKNOWN.ordinal());
	    Log.e(this.toString(), "Error while retrieving login status.", e);
	}
	
	// Hide progress dialog
	hideDialog();
    }

    
    private void hideDialog()
    {
	Runnable hideDialog = new Runnable()
	{
	    public void run()
	    {
		statusDialog.dismiss();
	    }
	};
	
	context.runOnUiThread(hideDialog);
    }
}
