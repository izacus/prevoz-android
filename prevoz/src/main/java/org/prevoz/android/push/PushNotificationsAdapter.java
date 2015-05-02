package org.prevoz.android.push;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.util.LocaleUtil;

import java.util.List;

public class PushNotificationsAdapter extends RecyclerView.Adapter<PushNotificationsAdapter.PushNotificationsHolder>
{
    public interface PushItemClickedListener {
        void onNotificationClicked(NotificationSubscription item);
    }

    private final Context ctx;
    private final PushItemClickedListener listener;

    private List<NotificationSubscription> notifications;


    public PushNotificationsAdapter(Context context, List<NotificationSubscription> notifications, PushItemClickedListener listener)
    {
        this.ctx = context;
        this.notifications = notifications;
        this.listener = listener;
    }


    @Override
    public PushNotificationsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_notification, parent, false);
        return new PushNotificationsHolder(v);
    }

    @Override
    public void onBindViewHolder(PushNotificationsHolder holder, int position) {
        NotificationSubscription sub = notifications.get(position);
        holder.route.setText(sub.getFrom().toString() + " - " + sub.getTo().toString());
        holder.date.setText(LocaleUtil.localizeDate(ctx.getResources(), sub.getDate()));
    }

    @Override
    public long getItemId(int position)
    {
        return notifications.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setData(List<NotificationSubscription> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    class PushNotificationsHolder extends RecyclerView.ViewHolder {

        @NonNull
        final TextView route;
        @NonNull
        final TextView date;
        @NonNull
        final View card;

        public PushNotificationsHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.item_push_card);
            route = (TextView) itemView.findViewById(R.id.item_push_route);
            date = (TextView) itemView.findViewById(R.id.item_push_date);

            card.setOnClickListener(v -> listener.onNotificationClicked(notifications.get(getAdapterPosition())));
        }
    }
}
