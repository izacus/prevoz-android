package org.prevoz.android.push;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;
import de.greenrobot.event.EventBus;
import eu.inmite.android.lib.dialogs.ISimpleDialogListener;
import eu.inmite.android.lib.dialogs.SimpleDialogFragment;
import org.androidannotations.annotations.*;
import org.prevoz.android.R;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.ViewUtils;

import java.util.List;

@EFragment(R.layout.fragment_notifications)
public class PushFragment extends Fragment implements ISimpleDialogListener
{
    public static final String DIALOG_ARG_SUB = "subscription";

    @ViewById(R.id.notifications_list)
    protected ListView notificationList;

    @ViewById(R.id.empty_view)
    protected View emptyView;

    @Bean
    protected PushManager pushManager;

    // Used for dialog callback due to unvieldy interface
    private NotificationSubscription clickedSubscription;

    @AfterViews
    protected void initFragment()
    {
        ViewUtils.setupEmptyView(notificationList, emptyView, "Niste prijavljeni na nobena obvestila.");
        List<NotificationSubscription> notifications = pushManager.getSubscriptions();
        notificationList.setAdapter(new PushNotificationsAdapter(getActivity(), notifications));
    }

    @ItemClick(R.id.notifications_list)
    protected void itemClick(NotificationSubscription item)
    {
        clickedSubscription = item;
        SimpleDialogFragment.createBuilder(getActivity(), getActivity().getSupportFragmentManager())
                            .setTitle(item.getFrom().toString() + " - " + item.getTo().toString())
                            .setMessage(String.format("Ali se res želite odjaviti od obveščanja v %s?", LocaleUtil.getNotificationDayName(getResources(), item.getDate()).toLowerCase()))
                            .setPositiveButtonText("Odjavi")
                            .setNegativeButtonText("Prekliči")
                            .setTargetFragment(this, 0)

                            .show();
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

    @Override
    public void onPositiveButtonClicked(int requestCode)
    {
        if (clickedSubscription != null)
        {
            pushManager.setSubscriptionStatus(clickedSubscription.getFrom(), clickedSubscription.getTo(), clickedSubscription.getDate(), false);
        }
    }

    @Override
    public void onNegativeButtonClicked(int requestCode)
    {
        clickedSubscription = null;
    }
}
