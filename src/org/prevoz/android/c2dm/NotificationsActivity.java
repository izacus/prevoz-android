package org.prevoz.android.c2dm;

import java.util.List;

import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.search.SearchResultsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;

public class NotificationsActivity extends SherlockActivity implements OnItemClickListener {

	private ListView list;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notifications_activity);
		getSupportActionBar().setTitle(R.string.notify_title);
		
		list = (ListView) findViewById(R.id.notifications_list);
		list.setEmptyView(findViewById(R.id.empty_list));
		list.setOnItemClickListener(this);
		
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	{
		NotifySubscription subscription = (NotifySubscription) list.getAdapter().getItem(position);
		
		// Start new activity with search results
		Intent intent = new Intent(this, SearchResultsActivity.class);
		Bundle dataBundle = new Bundle();
		dataBundle.putString("from", subscription.getFrom().getDisplayName());
        dataBundle.putString("fromCountry", subscription.getFrom().getCountryCode());
		dataBundle.putString("to", subscription.getTo().getDisplayName());
        dataBundle.putString("toCountry", subscription.getTo().getCountryCode());
		dataBundle.putLong("when", subscription.getDate().getTimeInMillis());
		intent.putExtras(dataBundle);
		this.startActivity(intent);
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		// Reload notifications list on resume
		List<NotifySubscription> subscriptions = NotificationManager.getInstance(getApplicationContext()).getNotificationSubscriptions(this);
		NotificationListAdapter adapter = new NotificationListAdapter(this, subscriptions);
		list.setAdapter(adapter);
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.getItemId() == android.R.id.home)
		{
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
