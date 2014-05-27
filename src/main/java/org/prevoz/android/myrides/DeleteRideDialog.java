package org.prevoz.android.myrides;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;
import de.greenrobot.event.EventBus;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.events.Events;
import org.prevoz.android.util.LocaleUtil;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.util.Calendar;

public class DeleteRideDialog extends DialogFragment
{
    private static String ARG_RIDE = "ride";

    public static DeleteRideDialog newInstance(RestRide ride)
    {
        Bundle params = new Bundle();
        params.putParcelable(ARG_RIDE, ride);
        DeleteRideDialog dialog = new DeleteRideDialog();
        dialog.setArguments(params);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final RestRide ride = getArguments().getParcelable(ARG_RIDE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Calendar date = Calendar.getInstance(LocaleUtil.getLocale());
        date.setTime(ride.date);

        builder.setTitle(ride.fromCity + " - " + ride.toCity)
               .setMessage(getString(R.string.ride_delete_message, LocaleUtil.getDayName(getResources(), date).toLowerCase(LocaleUtil.getLocale()), LocaleUtil.getFormattedTime(date)))
               .setPositiveButton(R.string.ride_delete_ok, new DialogInterface.OnClickListener()
               {
                   @Override
                   public void onClick(DialogInterface dialog, int which)
                   {
                       deleteRide(ride.id);
                   }
               })
               .setNegativeButton(R.string.ride_delete_cancel, null);


        return builder.create();
    }

    private void deleteRide(final Long id)
    {
        dismiss();

        final Activity context = getActivity();
        final ProgressDialog deleteDialog = new ProgressDialog(getActivity());
        deleteDialog.setMessage(context.getString(R.string.ride_delete_progress));
        deleteDialog.show();

        ApiClient.getAdapter().deleteRide(String.valueOf(id), new Callback<Response>()
        {
            @Override
            public void success(Response response, Response response2)
            {
                deleteDialog.dismiss();
                Toast.makeText(context, R.string.ride_delete_success, Toast.LENGTH_SHORT).show();
                EventBus.getDefault().post(new Events.RideDeleted(id));
            }

            @Override
            public void failure(RetrofitError error)
            {
                deleteDialog.dismiss();
                Toast.makeText(context, R.string.ride_delete_failure, Toast.LENGTH_SHORT).show();
            }
        });
    }
}