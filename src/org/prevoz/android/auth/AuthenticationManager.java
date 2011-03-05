package org.prevoz.android.auth;

import java.util.LinkedList;

import org.prevoz.android.Globals;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.CookieManager;
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
	
	private static final String PREF_API_KEY = "org.prevoz.apikey";
	
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
	public AuthenticationStatus getAuthenticationStatus(Activity context, boolean forceLogin)
	{
		return getAuthenticationStatus(context, forceLogin, null);
	};
	
	/**
	 * @param forceLogin	Forces authentication check and user to login with credentials
	 * @param authCallback 	Callback which is called after full authentication status is determined
	 * @return 				Status of current user authentication
	 */
	public synchronized AuthenticationStatus getAuthenticationStatus(final Activity context, 
																	 final boolean forceLogin, 
																	 final Handler authCallback)
	{
		if (currentStatus != AuthenticationStatus.UNKNOWN)
		{
			if (authCallback != null)
			{
				authCallback.sendEmptyMessage(currentStatus.ordinal());
			}
			
			return currentStatus;
		}
		
		Handler callback = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				
				AuthenticationStatus status = AuthenticationStatus.values()[msg.what];
				authenticationStatusReceived(context, status, authCallback, forceLogin, true);
			}
		};
		
		// Request new authenticated status
		new AuthenticationCheckTask().execute(callback);
		return AuthenticationStatus.UNKNOWN;
	};
	
	private void authenticationStatusReceived(final Activity context, 
											  final AuthenticationStatus status, 
											  final Handler callback, 
											  final boolean forceLogin,
											  final boolean apikeyLogin)
	{
		
		// Retry authentication with apikey if the status is negative
		if (status == AuthenticationStatus.NOT_AUTHENTICATED && apikeyLogin)
		{
			SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_API_KEY, 0);
			
			if (sharedPrefs.contains(PREF_API_KEY))
			{
				String apikey = sharedPrefs.getString(PREF_API_KEY, "");
				
				Handler cback = new Handler() 
				{
					@Override
					public void handleMessage(Message msg)
					{
						authenticationStatusReceived(context, AuthenticationStatus.values()[msg.what], callback, forceLogin, false);
					}
				};
				
				ApiKeyLoginTask apiKeyLogin = new ApiKeyLoginTask(apikey);
				apiKeyLogin.execute(cback);
				
				return;
			}
		}
		
		CookieSyncManager.createInstance(context).sync();
		
		// Force userlogin if he's not authenticated
		if (currentStatus == AuthenticationStatus.NOT_AUTHENTICATED && forceLogin)
		{
			requestLogin(context, callback);
			return;
		}
		
		currentStatus = status;
		
		Log.i(this.toString(), "Setting authentication status to " + currentStatus);
		
		if (callback != null)
		{
			callback.sendEmptyMessage(status.ordinal());
		}
	}
	
	/**
	 * Shows login form to log user in
	 * @param context		
	 * @param loginCallback	callback to be called after the login procedure is complete
	 */
	public void requestLogin(Activity context, Handler loginCallback)
	{
		callbackQueue.add(loginCallback);
		requestLogin(context);
	}
	
	/**
	 * Shows login form to log user in
	 * @param context		
	 */
	public void requestLogin(Activity context)
	{
		Intent intent = new Intent(context, LoginActivity.class);
		context.startActivity(intent);
		Log.d(this.toString(), "Starting authentication process...");
	}
	
	/**
	 * Logs user out and clears all login cookies
	 * @param context
	 */
	public void requestLogout(Activity context)
	{
		LogoutTask logoutTask = new LogoutTask(context);
		logoutTask.execute();
		
		try
		{
			if (logoutTask.get() == Globals.REQUEST_SUCCESS)
			{
				// Clear stored login cookies
				CookieSyncManager.createInstance(context);
				CookieManager cookieManager = CookieManager.getInstance();
				cookieManager.removeAllCookie();
				CookieSyncManager.getInstance().sync();
			}
		}
		catch (Exception e)
		{
			Log.e(this.toString(), "Logout task failed.", e);
		}
	}
	
	/**
	 * After Login activity completes, send out new login status to all waiting activities
	 * @param status new login status
	 */
	protected void notifyLoginResult(Context context, AuthenticationStatus status)
	{
		Log.i(this.toString(), "Authentication complete, new authentication status is " + status);
		currentStatus = status;
		
		// Retrieve API key after successful login
		if (status == AuthenticationStatus.AUTHENTICATED)
		{
			getApiKey(context);
		}
		
		while(callbackQueue.size() > 0)
		{
			Handler handler = callbackQueue.poll();
			handler.sendEmptyMessage(currentStatus.ordinal());
		}
	}
	
	private void getApiKey(Context context)
	{
		// Run authentication check  to retrieve the API key
		AuthenticationCheckTask authCheck = new AuthenticationCheckTask();
		authCheck.execute();
		
		// We SHOULD be authenticated now, skip API key storage if we're not
		// This is not a fatal error
		try
		{
			if (authCheck.get() == AuthenticationStatus.AUTHENTICATED)
			{
				// Store API key to shared preferences
				Log.i(this.toString(), "Storing api key " + authCheck.getApiKey() + " to preferences.");
				SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_API_KEY, 0);
				SharedPreferences.Editor editor = sharedPrefs.edit();
				editor.remove(PREF_API_KEY).putString(PREF_API_KEY, authCheck.getApiKey());
				editor.commit();
			}
			else
			{
				Log.e(this.toString(), "Cannot retrieve API key, use is not authenticated anymore!");
			}
		}
		catch (Exception e)
		{
			Log.e(this.toString(), "Failure when trying to retrieve API key!", e);
		}
	}
}
