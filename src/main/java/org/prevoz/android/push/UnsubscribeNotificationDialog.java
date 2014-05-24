package org.prevoz.android.push;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.prevoz.android.model.NotificationSubscription;

@EFragment
public class UnsubscribeNotificationDialog extends DialogFragment
{
    @Bean
    protected PushManager pushManager;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final NotificationSubscription sub = getArguments().getParcelable(PushFragment.DIALOG_ARG_SUB);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(sub.getFrom().toString() + " - " + sub.getTo().toString())
               .setMessage("Ali se res želite odjaviti od obveščanja?")
               .setPositiveButton("Odjavi", new DialogInterface.OnClickListener()
               {
                   @Override
                   public void onClick(DialogInterface dialog, int which)
                   {
                       pushManager.setSubscriptionStatus(sub.getFrom(), sub.getTo(), sub.getDate(), false);
                   }
               })
               .setNegativeButton("Prekliči", null);


        return builder.create();
    }
}
