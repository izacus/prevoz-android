package org.prevoz.android.push;

import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.widget.ListView;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import org.prevoz.android.R;
import org.prevoz.android.model.NotificationSubscription;

import java.util.List;

@EFragment(R.layout.fragment_notifications)
public class PushFragment extends Fragment
{
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
}
