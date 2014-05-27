package org.prevoz.android.myrides;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;
import org.androidannotations.annotations.EFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

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
        builder.setTitle(ride.fromCity.toString() + " - " + ride.toCity.toString())
               .setMessage(String.format("Ali želite izbirsati prevoz v %s ob %s?", "petek", "12:00"))
               .setPositiveButton("Izbriši", new DialogInterface.OnClickListener()
               {
                   @Override
                   public void onClick(DialogInterface dialog, int which)
                   {
                       deleteRide(ride.id);
                   }
               })
               .setNegativeButton("Prekliči", null);


        return builder.create();
    }

    private void deleteRide(Long id)
    {
        dismiss();

        final Activity context = getActivity();
        final ProgressDialog deleteDialog = new ProgressDialog(getActivity());
        deleteDialog.setMessage("Brišem...");
        deleteDialog.show();

        ApiClient.getAdapter().deleteRide(String.valueOf(id), new Callback<Response>()
        {
            @Override
            public void success(Response response, Response response2)
            {
                deleteDialog.dismiss();
                Toast.makeText(context, "Prevoz izbrisan.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void failure(RetrofitError error)
            {
                deleteDialog.dismiss();
                Toast.makeText(context, "Prevoza ni bilo mogoče izbrisati.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
