package org.prevoz.android.push;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.prevoz.android.PrevozFragment;
import org.prevoz.android.R;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.ui.DividerItemDecoration;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.ViewUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import rx.android.schedulers.AndroidSchedulers;

public class PushFragment extends PrevozFragment
{
    public static final String DIALOG_ARG_SUB = "subscription";

    @InjectView(R.id.notifications_list)
    protected RecyclerView notificationList;

    @InjectView(R.id.empty_view)
    protected View emptyView;


    // Used for dialog callback due to unvieldy interface
    private NotificationSubscription clickedSubscription;

    private PushNotificationsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View views = inflater.inflate(R.layout.fragment_notifications, container, false);
        ButterKnife.inject(this, views);

        pushManager.getSubscriptions()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(notifications -> {
                        ViewUtils.setupEmptyView(notificationList, emptyView, "Niste prijavljeni na nobena obvestila.");
                        adapter = new PushNotificationsAdapter(getActivity(), notifications, item -> onItemClicked(item));

                        notificationList.setAdapter(adapter);
                        notificationList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
                        LinearLayoutManager llm = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                        notificationList.setLayoutManager(llm);
                        updateEmptyView();
                    });


        return views;
    }

    protected void onItemClicked(NotificationSubscription item)
    {
        clickedSubscription = item;
        new AlertDialog.Builder(getActivity(), R.style.Prevoz_Theme_Dialog)
                        .setTitle(String.format("%s - %s", item.getFrom().toString(), item.getTo().toString()))
                        .setMessage(String.format("Ali se res želite odjaviti od obveščanja v %s?", LocaleUtil.getNotificationDayName(getResources(), item.getDate()).toLowerCase()))
                        .setNegativeButton("Prekliči", null)
                        .setPositiveButton("Odjavi", (dialog, which) -> {
                            if (clickedSubscription != null)
                            {
                                pushManager.setSubscriptionStatus(getActivity(), clickedSubscription.getFrom(), clickedSubscription.getTo(), clickedSubscription.getDate(), false);
                            }
                        }).show();
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

    private void updateEmptyView() {
        notificationList.setVisibility(adapter.getItemCount() == 0 ? View.INVISIBLE : View.VISIBLE);
        emptyView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
    }

    public void onEventMainThread(Events.NotificationSubscriptionStatusChanged e)
    {
        pushManager.getSubscriptions()
                   .observeOn(AndroidSchedulers.mainThread())
                   .subscribe(notifications -> {
                       adapter.setData(notifications);
                       updateEmptyView();
                   });

    }
}
