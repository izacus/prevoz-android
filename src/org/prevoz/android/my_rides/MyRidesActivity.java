package org.prevoz.android.my_rides;

import org.prevoz.android.R;
import org.prevoz.android.SectionedAdapter;
import org.prevoz.android.add_ride.AddRideActivity;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.search.SearchResults;
import org.prevoz.android.search.SearchResultAdapter.SearchResultViewWrapper;
import org.prevoz.android.util.SectionedAdapterUtil;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MyRidesActivity extends FragmentActivity implements LoaderCallbacks<SearchResults> 
{

	private ListView ridesList;
	private Button addRideButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.myrides_activity);
		
		ridesList = (ListView) findViewById(R.id.myrides_list);
		ridesList.setEmptyView(findViewById(R.id.myrides_norides));
		ridesList.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, 
									View view,
									int position, 
									long id) 
			{
				SearchResultViewWrapper viewWrapper = (SearchResultViewWrapper)view.getTag();
				Intent intent = new Intent(MyRidesActivity.this, RideInfoActivity.class);
				intent.putExtra(RideInfoActivity.RIDE_ID, viewWrapper.getRideId());
				startActivityForResult(intent, 1);
				getSupportLoaderManager().initLoader(1, null, MyRidesActivity.this).forceLoad();
			};
		});
		
		
		addRideButton = (Button) findViewById(R.id.addride_button);
		addRideButton.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent intent = new Intent(MyRidesActivity.this, AddRideActivity.class);
				startActivity(intent);
				getSupportLoaderManager().initLoader(1, null, MyRidesActivity.this).forceLoad();
			}
		});
		
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
		SectionedAdapter adapter = SectionedAdapterUtil.buildAdapterWithResults(this, results);
		ridesList.setAdapter(adapter);
	}

	public void onLoaderReset(Loader<SearchResults> arg0) 
	{
		// Nothing TBD
	}
}
