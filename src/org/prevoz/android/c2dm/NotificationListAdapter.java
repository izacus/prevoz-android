package org.prevoz.android.c2dm;

import java.util.List;

import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class NotificationListAdapter extends ArrayAdapter<NotifySubscription> 
{
	private class NotificationWrapper
	{
		public NotificationWrapper(View relation, View date)
		{
			this.relation = (TextView) relation;
			this.date = (TextView) date;
		}
		
		public TextView relation;
		public TextView date;
	}
	
	
	private Activity context;
	
	public NotificationListAdapter(Activity context, List<NotifySubscription> subscriptions)
	{
		super(context, R.layout.notification_list_item, R.id.relation, subscriptions);
		this.context = context;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View view;
		if (convertView == null)
		{
			LayoutInflater inflater = context.getLayoutInflater();
			view = inflater.inflate(R.layout.notification_list_item, null);
			
			NotificationWrapper wrapper = new NotificationWrapper(view.findViewById(R.id.relation),
																  view.findViewById(R.id.date));
			view.setTag(wrapper);
		}
		else
		{
			view = convertView;
		}
		
		NotifySubscription item = getItem(position);
		NotificationWrapper wrapper = (NotificationWrapper) view.getTag();
		wrapper.relation.setText(item.getFrom() + " - " + item.getTo());
		wrapper.date.setText(LocaleUtil.getDayName(context.getResources(), item.getDate()) + ", " +
							 LocaleUtil.getFormattedDate(context.getResources(), item.getDate()));
		
		return view;
	}
	
}
