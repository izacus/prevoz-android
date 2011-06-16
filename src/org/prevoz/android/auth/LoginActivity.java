package org.prevoz.android.auth;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.add_ride.AddRideActivity_obsolete;
import org.prevoz.android.util.HTTPHelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager.BadTokenException;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginActivity extends Activity
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
				HTTPHelper.updateSessionCookies(AddRideActivity_obsolete.getInstance());

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
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_view);
		
		// Load login view
		WebView view = (WebView)findViewById(R.id.loginView);
		view.setWebViewClient(new WebViewController(this));
		view.loadUrl(Globals.LOGIN_URL);
	}

	/**
	 * Send not authenticated status if user pressed the back button to cancel login
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == Activity.RESULT_CANCELED)
		{
			AuthenticationManager.getInstance().notifyLoginResult(this, AuthenticationStatus.NOT_AUTHENTICATED);
		}
	}
}
