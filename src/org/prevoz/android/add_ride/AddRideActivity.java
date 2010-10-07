package org.prevoz.android.add_ride;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.HTTPHelper;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class AddRideActivity extends Activity
{
    private static AddRideActivity instance;
    public static AddRideActivity getInstance()
    {
	return instance;
    }
    
    private LoginStatus loginStatus;
    
    private class WebViewController extends WebViewClient
    {
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url)
	{
	    if (url.contains("/login/success"))
	    {
		CookieSyncManager.createInstance(AddRideActivity.getInstance());
		CookieManager cookieManager = CookieManager.getInstance();
		
		// Get newly received session cookies in header form
		String cookies = cookieManager.getCookie("http://prevoz.org");
		
		// Store them to HTTP client
		new HTTPHelper(instance).setSessionCookies(cookies);
		checkLoginStatus();
	    }
	    else
	    {
		view.loadUrl(url);
	    }
	    
	    return true;
	}
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	AddRideActivity.instance = this;
	
	loginStatus = LoginStatus.UNKNOWN;
	setContentView(R.layout.add_ride_activity);
	
	// Check if user is logged in
	checkLoginStatus();
    }
    
    /**
     * Request current user's login status 
     */
    private void checkLoginStatus()
    {
	LoginStatusTask checkStatusTask = new LoginStatusTask(this);
	
	// Dispatch response to updateLoginStatus method
	Handler callbackHandler = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		updateLoginStatus(LoginStatus.values()[msg.what]);
	    }
	};
	
	checkStatusTask.start(callbackHandler);
    }
    
    /**
     * Show login dialog if user is not logged in or display ride add status
     * @param status
     */
    private void updateLoginStatus(LoginStatus status)
    {
	
	switch(status)
	{
		case UNKNOWN:
		    Toast.makeText(this, R.string.server_error, Toast.LENGTH_LONG).show();
		    break;
		    
		case LOGGED_IN:
		    ViewFlipper addFlipper = (ViewFlipper)findViewById(R.id.add_flipper);
		    addFlipper.showNext();
		    break;
		    
		case NOT_LOGGED_IN:
		    showWebLogin();
		    break;
	}
    }
    
    /**
     * Shows user the login form
     */
    private void showWebLogin()
    {
	ViewFlipper addFlipper = (ViewFlipper)findViewById(R.id.add_flipper);
	addFlipper.showPrevious();
	
	WebView view = (WebView)findViewById(R.id.webview);
	view.setWebViewClient(new WebViewController());
	view.loadUrl(Globals.LOGIN_URL);
    }
}
