package org.prevoz.android.auth;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.CookieSyncManager;

/**
 * Login manager singleton, handles authentication
 * @author Jernej Virag
 *
 */
public class AuthenticationManager
{
	// Singelton methods
	private static AuthenticationManager instance = null;
	
	public static AuthenticationManager getInstance()
	{
		if (instance == null)
		{
			instance = new AuthenticationManager();
		}
		
		return instance;
	}
	
	private AuthenticationStatus currentStatus = AuthenticationStatus.UNKNOWN;
	private LinkedList<Handler> callbackQueue = new LinkedList<Handler>();
	
	private AuthenticationManager()
	{
		// Nothing TBD
	};
	
	/**
	 * @param forceLogin	Forces authentication check and user to login with credentials
	 * @return 			    Status of current user authentication
	 */
	public AuthenticationStatus getAuthenticationStatus(Context context, boolean forceLogin)
	{
		return getAuthenticationStatus(context, forceLogin, null);
	};
	
	/**
	 * @param forceLogin	Forces authentication check and user to login with credentials
	 * @param authCallback 	Callback which is called after full authentication status is determined
	 * @return 				Status of current user authentication
	 */
	public synchronized AuthenticationStatus getAuthenticationStatus(Context context, boolean forceLogin, Handler authCallback)
	{
		if (currentStatus != AuthenticationStatus.UNKNOWN)
		{
			if (authCallback != null)
			{
				authCallback.sendEmptyMessage(currentStatus.ordinal());
			}
			
			return currentStatus;
		}
		
		final Context outsideContext = context;
		final Handler outsideCallback = authCallback;
		
		Handler callback = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				CookieSyncManager.createInstance(outsideContext).sync();
				currentStatus = AuthenticationStatus.values()[msg.what];
				
				if (outsideCallback != null)
				{
					outsideCallback.sendEmptyMessage(msg.what);
				}
			}
		};
		
		// Request new authenticated status
		new AuthenticationCheckTask().execute(callback);
		return AuthenticationStatus.UNKNOWN;
	};
	
	public void requestLogin(Activity context, Handler loginCallback)
	{
		callbackQueue.add(loginCallback);
		requestLogin(context);
	}
	
	public void requestLogin(Activity context)
	{
		Intent intent = new Intent(context, LoginActivity.class);
		context.startActivity(intent);
		Log.d(this.toString(), "Starting authentication process...");
	}
	
	/**
	 * After Login activity completes, send out new login status to all waiting activities
	 * @param status new login status
	 */
	protected void notifyLoginResult(AuthenticationStatus status)
	{
		Log.i(this.toString(), "Authentication complete, new authentication status is " + status);
		
		currentStatus = status;
		
		while(callbackQueue.size() > 0)
		{
			Handler handler = callbackQueue.poll();
			handler.sendEmptyMessage(currentStatus.ordinal());
		}
	}
}
