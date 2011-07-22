package org.prevoz.android.auth;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.HTTPHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.WindowManager.BadTokenException;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginActivity extends FragmentActivity
{

	private class WebViewController extends WebViewClient
	{
		private ProgressDialog loadingDialog;
		private Activity context;

		public WebViewController(Activity context)
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
			if (loadingDialog == null || !loadingDialog.isShowing())
			{
				try
				{
					loadingDialog = ProgressDialog.show(context, null,
							context.getString(R.string.loading));
				}
				// Is thrown if this activity finishes and this event is triggered
				catch (BadTokenException e)
				{
					// Ignore the action
					return;
				}
			}

			Log.i(this.toString(), "Page started " + url);

			if (url.contains("/login/success"))
			{
				HTTPHelper.updateSessionCookies(context);

				if (loadingDialog != null && loadingDialog.isShowing())
				{
					try
					{
						loadingDialog.dismiss();
					}
					catch (IllegalArgumentException e)
					{
						loadingDialog = null;
					}
				}
				
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
			try
			{
				if (loadingDialog != null && loadingDialog.isShowing())
				{
					loadingDialog.dismiss();
				}
			}
			catch (IllegalArgumentException e)
			{
				loadingDialog = null;
			};
		}

	}
	
	private WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_view);
		
		// Load login view
		webView = (WebView)findViewById(R.id.loginView);
		webView.setWebViewClient(new WebViewController(this));
		webView.loadUrl(Globals.LOGIN_URL);
		
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
}
