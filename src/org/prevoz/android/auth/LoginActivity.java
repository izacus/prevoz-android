package org.prevoz.android.auth;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.HTTPHelper;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.flurry.android.FlurryAgent;

public class LoginActivity extends SherlockFragmentActivity
{

	private class WebViewController extends WebViewClient
	{
		private SherlockFragmentActivity context;

		public WebViewController(SherlockFragmentActivity context)
		{
			this.context = context;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			Log.d(this.toString(), "Loading URL " + url);

			view.loadUrl(url);

			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			context.setSupportProgressBarIndeterminateVisibility(true);
			view.setEnabled(false);

			Log.i(this.toString(), "Page started " + url);
			if (url.contains("/login/success"))
			{
				HTTPHelper.updateSessionCookies(context);
				context.setSupportProgressBarIndeterminateVisibility(false);
				view.setEnabled(true);
				// Notify all threads of successful login
				AuthenticationManager.getInstance().notifyLoginResult(LoginActivity.this, AuthenticationStatus.AUTHENTICATED);
				// Close this activity
				context.finish();
			}

			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			context.setSupportProgressBarIndeterminateVisibility(false);
			view.setEnabled(true);
		}
	}
	
	private WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.login_view);
		
		this.setSupportProgressBarIndeterminateVisibility(false);
		// Load login view
		webView = (WebView)findViewById(R.id.loginView);
		webView.setWebViewClient(new WebViewController(this));
		webView.loadUrl(HTTPHelper.getUrlPrefix() + Globals.LOGIN_URL);
		
		if (savedInstanceState != null)
		{
			webView.restoreState(savedInstanceState);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		webView.saveState(outState);
	}

	@Override
	public void onBackPressed() 
	{
		AuthenticationManager.getInstance().notifyLoginResult(this, AuthenticationStatus.NOT_AUTHENTICATED);
		super.onBackPressed();
	}
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		FlurryAgent.setReportLocation(false);
		FlurryAgent.onStartSession(this, getString(R.string.flurry_apikey));
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
}
