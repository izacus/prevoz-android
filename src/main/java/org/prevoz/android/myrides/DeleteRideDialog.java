package org.prevoz.android.myrides;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.events.Events;
import org.prevoz.android.util.LocaleUtil;

import de.greenrobot.event.EventBus;
import eu.inmite.android.lib.dialogs.BaseDialogFragment;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class DeleteRideDialog extends BaseDialogFragment
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
    protected Builder build(Builder builder)
    {
        final RestRide ride = getArguments().getParcelable(ARG_RIDE);
        builder.setTitle(ride.fromCity + " - " + ride.toCity)
                .setMessage(getString(R.string.ride_delete_message, LocaleUtil.getDayName(getResources(), ride.date).toLowerCase(LocaleUtil.getLocale()), LocaleUtil.getFormattedTime(ride.date)))
                .setPositiveButton(R.string.ride_delete_ok, new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        deleteRide(ride.id);
                    }
                })
                .setNegativeButton(R.string.ride_delete_cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dismiss();
                    }
                });

        return builder;
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
