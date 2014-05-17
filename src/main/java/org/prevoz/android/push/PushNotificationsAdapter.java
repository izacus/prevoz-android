package org.prevoz.android.push;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.prevoz.android.R;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.util.LocaleUtil;

import java.util.List;

public class PushNotificationsAdapter extends BaseAdapter
{
    private final Context ctx;
    private final List<NotificationSubscription> notifications;

    public PushNotificationsAdapter(Context context, List<NotificationSubscription> notifications)
    {
        this.ctx = context;
        this.notifications = notifications;
    }

    @Override
    public int getCount()
    {
        return notifications.size();
    }

    @Override
    public Object getItem(int position)
    {
        return notifications.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return notifications.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;
        if (v == null)
        {
            v = LayoutInflater.from(ctx).inflate(R.layout.item_notification, parent, false);
        }

        TextView route = (TextView) v.findViewById(R.id.item_push_route);
        TextView date = (TextView) v.findViewById(R.id.item_push_date);

        NotificationSubscription sub = notifications.get(position);
        route.setText(sub.getFrom().toString() + " - " + sub.getTo().toString());
        date.setText(LocaleUtil.localizeDate(ctx.getResources(), sub.getDate()));
        return v;
    }
}
