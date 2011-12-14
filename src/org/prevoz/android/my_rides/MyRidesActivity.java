package org.prevoz.android.my_rides;

import org.prevoz.android.R;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;
import org.prevoz.android.search.SearchResults;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.Toast;

public class MyRidesActivity extends FragmentActivity implements LoaderCallbacks<SearchResults> 
{

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.myrides_activity);
		
		Handler authHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				authenticationStatusReceived(AuthenticationStatus.values()[msg.what]);
			}
		};
		
		AuthenticationManager.getInstance().getAuthenticationStatus(this, authHandler);
	}
	
	private void authenticationStatusReceived(AuthenticationStatus status)
	{
		Log.i(this.toString(), "Received authentication status: " + status);
		
		
		switch(status)
		{
			case UNKNOWN:
				Toast.makeText(this, R.string.network_error, Toast.LENGTH_LONG).show();
				finish();
				break;
				
			case NOT_AUTHENTICATED:				
				Log.i(this.toString(), "Opening user login request...");
				
				Handler loginHandler = new Handler()
				{
					@Override
					public void handleMessage(Message msg) 
					{
						AuthenticationStatus status = AuthenticationStatus.values()[msg.what];
						if (status != AuthenticationStatus.AUTHENTICATED)
						{
							finish();
						}
						else
						{
							showView();
						}
					}
				};
				
				AuthenticationManager.getInstance().requestLogin(this, loginHandler);
				break;
			
			case AUTHENTICATED:
				showView();
				break;
		}
	}

	private void showView()
	{
		// TODO: fix
		getSupportLoaderManager().initLoader(1, null, this).forceLoad();
	}
	
	public Loader<SearchResults> onCreateLoader(int id, Bundle args) 
	{
		return new MyRidesLoader(this);
	}

	public void onLoadFinished(Loader<SearchResults> loader, SearchResults results) 
	{
		Log.e(this.toString(), "Got results.");
		// TODO Show rides	
	}

	public void onLoaderReset(Loader<SearchResults> arg0) 
	{
		// Nothing TBD
	}
}
