package org.prevoz.android.c2dm;

import java.util.List;

import org.prevoz.android.R;
import org.prevoz.android.search.SearchResultsActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class NotificationsActivity extends Activity implements OnItemClickListener {

	private ListView list;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notifications_activity);
		((TextView)findViewById(R.id.title_bar)).setText(R.string.notify_title);
		
		list = (ListView) findViewById(R.id.notifications_list);
		list.setEmptyView(findViewById(R.id.empty_list));
		
		List<NotifySubscription> subscriptions = NotificationManager.getInstance(getApplicationContext()).getNotificationSubscriptions(this);
		NotificationListAdapter adapter = new NotificationListAdapter(this, subscriptions);
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	{
		NotifySubscription subscription = (NotifySubscription) list.getAdapter().getItem(position);
		
		// Start new activity with search results
		Intent intent = new Intent(this, SearchResultsActivity.class);
		Bundle dataBundle = new Bundle();
		dataBundle.putString("from", subscription.getFrom());
		dataBundle.putString("to", subscription.getTo());
		dataBundle.putLong("when", subscription.getDate().getTimeInMillis());
		intent.putExtras(dataBundle);
		this.startActivity(intent);
	}

}
