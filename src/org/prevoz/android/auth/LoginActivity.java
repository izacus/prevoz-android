package org.prevoz.android.auth;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import com.github.rtyley.android.sherlock.android.accounts.SherlockAccountAuthenticatorActivity;
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

public class LoginActivity extends SherlockAccountAuthenticatorActivity
{

	private class WebViewController extends WebViewClient
	{
		private SherlockAccountAuthenticatorActivity context;

		public WebViewController(SherlockAccountAuthenticatorActivity context)
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

                Intent resultIntent = new Intent();
                resultIntent.putExtra(AccountManager.KEY_ACCOUNT_NAME, AuthenticationManager.getInstance().getUsername());
                resultIntent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.acc_type));
                resultIntent.putExtra(AccountManager.KEY_AUTHTOKEN, getString(R.string.acc_type));

                context.setAccountAuthenticatorResult(resultIntent.getExtras());
                context.setResult(Activity.RESULT_OK, resultIntent);
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

        // Check if account exists first
        AccountManager mgr = AccountManager.get(this);
        if (mgr.getAccountsByType(getString(R.string.acc_type)).length > 0)
        {
            Toast.makeText(this, getString(R.string.account_exists_error), Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

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
