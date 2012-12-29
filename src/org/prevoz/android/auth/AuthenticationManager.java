package org.prevoz.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.actionbarsherlock.R;
import org.prevoz.android.Globals;

import java.util.LinkedList;

/**
 * Login manager singleton, handles authentication
 * @author Jernej Virag
 *
 */
public class AuthenticationManager
{
	// Singelton methods
	private volatile static AuthenticationManager instance = null;
	
	public static AuthenticationManager getInstance()
	{
		if (instance == null)
		{
			instance = new AuthenticationManager();
		}
		
		return instance;
	}

	private AuthenticationStatus currentStatus = AuthenticationStatus.UNKNOWN;
	private LinkedList<Handler> loginStatusQueue = new LinkedList<Handler>();
	private LinkedList<Handler> callbackQueue = new LinkedList<Handler>();
	
	private volatile boolean loginCheckInProgress = false;
	private String username;

	private AuthenticationManager()
	{
		// Nothing TBD
	};
	
	
	/**
	 * @param authCallback 	Callback which is called after full authentication status is determined
	 * @return 				Status of current user authentication
	 */
	public synchronized void getAuthenticationStatus(final Activity context, 
													 final Handler authCallback)
	{


		if (currentStatus != AuthenticationStatus.UNKNOWN)
		{
			if (authCallback != null)
			{
				authCallback.sendEmptyMessage(currentStatus.ordinal());
			}
			
			return;
		}

        AccountManager mgr = AccountManager.get(context);
        if (mgr.getAccountsByType(context.getString(R.string.acc_type)).length < 1)
        {
            clearAuthCookies(context);

            if (authCallback != null)
            {
                authCallback.sendEmptyMessage(AuthenticationStatus.NOT_AUTHENTICATED.ordinal());
            }

            return;
        }

		// Add callback to waiting authentication queue
		if (authCallback != null)
		{
			loginStatusQueue.add(authCallback);
		}
		
		if (!loginCheckInProgress)
		{
			loginCheckInProgress = true;
			
			Handler callback = new Handler()
			{
				@Override
				public void handleMessage(Message msg)
				{
					
					AuthenticationStatus status = AuthenticationStatus.values()[msg.what];
					authenticationStatusReceived(context, status, true);
				}
			};
			
			// Request new authenticated status
			new AuthenticationCheckTask().execute(callback);
		}
	};
	
	private void authenticationStatusReceived(final Activity context, 
											  final AuthenticationStatus status, 
											  final boolean apikeyLogin)
	{
		
		Log.i(this.toString(), "Received authentication status " + status);
		
		// Retry authentication with apikey if the status is negative
		if (status == AuthenticationStatus.NOT_AUTHENTICATED && apikeyLogin)
		{
            AccountManager mgr = AccountManager.get(context);
            Account[] accounts = mgr.getAccountsByType(context.getString(R.string.acc_type));
            if (accounts.length == 0)
                return;

            Account account = accounts[0];
            final String username = account.name;
            final String apikey = mgr.getPassword(account);
            Log.i(this.toString(), "Attempting login with apikey " + apikey);

            Handler cback = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    AuthenticationManager.this.username = username;
                    authenticationStatusReceived(context, AuthenticationStatus.values()[msg.what], false);
                }
            };

            ApiKeyLoginTask apiKeyLogin = new ApiKeyLoginTask(apikey);
            apiKeyLogin.execute(cback);
		}
		
		CookieSyncManager.createInstance(context).sync();
		currentStatus = status;
		
		Log.i(this.toString(), "Setting authentication status to " + currentStatus);
		loginCheckInProgress = false;
		
		while(!loginStatusQueue.isEmpty())
		{
			Handler callback = loginStatusQueue.poll();
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
		clearAuthState(context);
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
		LogoutTask logoutTask = new LogoutTask();
		logoutTask.execute();
		
		try
		{
			if (logoutTask.get() == Globals.REQUEST_SUCCESS)
			{
				clearAuthState(context);
				currentStatus = AuthenticationStatus.NOT_AUTHENTICATED;
			}
		}
		catch (Exception e)
		{
			Log.e(this.toString(), "Logout task failed.", e);
		}
	}
	
	//public void clearAuthCo
	public void clearAuthCookies(Context context) 
	{
		// Clear stored login cookies
		CookieSyncManager.createInstance(context);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		CookieSyncManager.getInstance().sync();
	}
	
	protected void clearAuthState(Context context)
	{
		clearAuthCookies(context);
		// Clear API key and reset authentication status
		AccountManager mgr = AccountManager.get(context);
        Account[] accounts = mgr.getAccountsByType(context.getString(R.string.acc_type));
        for (Account acc : accounts)
        {
            mgr.removeAccount(acc, null, null);
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
		// Run authentication check to retrieve the API key
		AuthenticationCheckTask authCheck = new AuthenticationCheckTask();
		authCheck.execute();
		
		// We SHOULD be authenticated now, skip API key storage if we're not
		// This is not a fatal error
		try
		{
            AuthenticationCheckTask.AuthCheckResult result = authCheck.get();
			if (result.status == AuthenticationStatus.AUTHENTICATED)
			{
                AccountManager mgr = AccountManager.get(context);

                // Check if account already exists
                Account account = null;
                for (Account acc : mgr.getAccountsByType(context.getString(R.string.acc_type)))
                {
                    if (acc.name.equals(result.username))
                    {
                        account = acc;
                        break;
                    }
                }

                if (account == null)
                {
                    account = new Account(result.username, context.getString(R.string.acc_type));
                    mgr.addAccountExplicitly(account, authCheck.getApiKey(), null);
                }
                else
                {
                    mgr.setPassword(account, authCheck.getApiKey());
                }

                this.username = result.username;
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

    public String getUsername()
    {
        return username;
    }
}
