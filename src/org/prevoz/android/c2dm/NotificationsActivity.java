package org.prevoz.android.c2dm;

import java.util.List;

import org.prevoz.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class NotificationsActivity extends Activity {

	private ListView list;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notifications_activity);
		
		list = (ListView) findViewById(R.id.notifications_list);
		list.setEmptyView(findViewById(R.id.empty_list));
		
		List<NotifySubscription> subscriptions = NotificationManager.getInstance().getNotificationSubscriptions(this);
		NotificationListAdapter adapter = new NotificationListAdapter(this, subscriptions);
		list.setAdapter(adapter);
	}

}
