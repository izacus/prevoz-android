package org.prevoz.android.my_rides;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.add_ride.AddRideActivity;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.search.SearchResultAdapter.SearchResultViewWrapper;
import org.prevoz.android.search.SearchResults;
import org.prevoz.android.util.SectionedAdapter;
import org.prevoz.android.util.SectionedAdapterUtil;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.flurry.android.FlurryAgent;

public class MyRidesActivity extends FragmentActivity implements LoaderCallbacks<SearchResults> 
{
	private static final int MENU_LOGOUT = 0;
	
	private ViewFlipper loadingFlipper;
	private ListView ridesList;
	private Button addRideButton;
	private Boolean authenticated = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.myrides_activity);
		
		// Set ride title
		((TextView)findViewById(R.id.title_bar)).setText(R.string.cd_my_rides);
		
		loadingFlipper = (ViewFlipper)findViewById(R.id.myrides_flipper);
		loadingFlipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
		loadingFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		ridesList = (ListView) findViewById(R.id.myrides_list);
		ridesList.setEmptyView(findViewById(R.id.myrides_norides));
		ridesList.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, 
									View view,
									int position, 
									long id) 
			{
				FlurryAgent.logEvent("MyRides - Show Ride");
				SearchResultViewWrapper viewWrapper = (SearchResultViewWrapper)view.getTag();
				Intent intent = new Intent(MyRidesActivity.this, RideInfoActivity.class);
				intent.putExtra(RideInfoActivity.RIDE_ID, viewWrapper.getRideId());
				startActivityForResult(intent, 1);
			};
		});
		
		
		addRideButton = (Button) findViewById(R.id.addride_button);
		addRideButton.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				FlurryAgent.logEvent("MyRides - Add ride");
				Intent intent = new Intent(MyRidesActivity.this, AddRideActivity.class);
				startActivity(intent);
			}
		});
		
		Handler authHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				authenticationStatusReceived(AuthenticationStatus.values()[msg.what]);
			}
		};
		
		// Page display tracking
		FlurryAgent.onPageView();
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
							authenticated = true;
							reload();
						}
					}
				};
				
				AuthenticationManager.getInstance().requestLogin(this, loginHandler);
				break;
			
			case AUTHENTICATED:
				authenticated = true;
				reload();
				break;
		}
	}
	
	

	@Override
	protected void onResume() 
	{
		super.onResume();
		if (authenticated)
		{
			reload();
		}
	}

	private void reload()
	{
		Log.d(this.toString(), "Reloading....");
		loadingFlipper.setDisplayedChild(0);
		Loader<SearchResults> loader = getSupportLoaderManager().initLoader(Globals.LOADER_MYRIDES, null, this);
		loader.forceLoad();
	}
	
	public Loader<SearchResults> onCreateLoader(int id, Bundle args) 
	{
		return new MyRidesLoader(this);
	}

	public void onLoadFinished(Loader<SearchResults> loader, SearchResults results) 
	{
		SectionedAdapter adapter = SectionedAdapterUtil.buildAdapterWithResults(this, results);
		ridesList.setAdapter(adapter);
		loadingFlipper.setDisplayedChild(1);
	}

	public void onLoaderReset(Loader<SearchResults> arg0) 
	{
		// Nothing TBD
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(Menu.NONE, MENU_LOGOUT, Menu.NONE, getString(R.string.logout)).setIcon(android.R.drawable.ic_delete);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == MENU_LOGOUT)
		{
			FlurryAgent.logEvent("MyRides - Logout");
			AuthenticationManager.getInstance().requestLogout(this);
			Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
			finish();
			
			return true;
		}
		else
		{
			Log.e(this.toString(), "Tried to open non-existed menu item.");
		}
		
		return false;
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
