package org.prevoz.android.push;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.prevoz.android.R;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.ViewUtils;

import java.util.List;

import de.greenrobot.event.EventBus;

@EFragment(R.layout.fragment_notifications)
public class PushFragment extends Fragment
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
        new MaterialDialog.Builder(getActivity())
                          .title(item.getFrom().toString() + " - " + item.getTo().toString())
                          .titleColorRes(R.color.prevoztheme_color_dark)
                          .content(String.format("Ali se res želite odjaviti od obveščanja v %s?", LocaleUtil.getNotificationDayName(getResources(), item.getDate()).toLowerCase()))
                          .positiveText("Odjavi")
                          .negativeText("Prekliči")
                          .callback(new MaterialDialog.SimpleCallback() {
                              @Override
                              public void onPositive(MaterialDialog materialDialog) {
                                  if (clickedSubscription != null)
                                  {
                                      pushManager.setSubscriptionStatus(getActivity(), clickedSubscription.getFrom(), clickedSubscription.getTo(), clickedSubscription.getDate(), false);
                                  }
                              }
                          })
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
}
