package org.prevoz.android.push;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.widget.ListView;
import com.googlecode.androidannotations.annotations.*;
import de.greenrobot.event.EventBus;
import org.prevoz.android.R;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.NotificationSubscription;

import java.util.List;

@EFragment(R.layout.fragment_notifications)
public class PushFragment extends Fragment
{
    public static final String DIALOG_ARG_SUB = "subscription";

    @ViewById(R.id.notifications_list)
    protected ListView notificationList;

    @Bean
    protected PushManager pushManager;

    @AfterViews
    protected void initFragment()
    {
        List<NotificationSubscription> notifications = pushManager.getSubscriptions();
        notificationList.setAdapter(new PushNotificationsAdapter(getActivity(), notifications));
    }

    @ItemClick(R.id.notifications_list)
    protected void itemClick(NotificationSubscription item)
    {
        Bundle args = new Bundle();
        args.putParcelable(DIALOG_ARG_SUB, item);
        UnsubscribeNotificationDialog dialog = new UnsubscribeNotificationDialog_();
        dialog.setArguments(args);
        dialog.show(getActivity().getSupportFragmentManager(), "UnsubscribeDialog");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    public void onEventMainThread(Events.NotificationSubscriptionStatusChanged e)
    {
        List<NotificationSubscription> notifications = pushManager.getSubscriptions();
        notificationList.setAdapter(new PushNotificationsAdapter(getActivity(), notifications));
    }
}
