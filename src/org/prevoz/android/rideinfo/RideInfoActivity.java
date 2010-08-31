package org.prevoz.android.rideinfo;

import java.text.SimpleDateFormat;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class RideInfoActivity extends Activity
{
    public static final String RIDE_ID = RideInfoActivity.class.toString() + ".ride_id";
    
    private static RideInfoActivity instance = null;
    
    private int rideID;
    
    private ProgressDialog loadingDialog = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	RideInfoActivity.instance = this;
	
	Resources res = getResources();
	loadingDialog = ProgressDialog.show(this, "", res.getString(R.string.loading));
	
	setContentView(R.layout.ride_info);
	
	// Get ride ID
	if (getIntent().getExtras() != null)
	{
	    rideID = getIntent().getExtras().getInt(RIDE_ID);
	    Log.d(this.toString(), "Requesting ride info for id " + rideID);
	}
	
	loadRideData();
    }
    
    private void loadRideData()
    {
	final LoadInfoTask loadInfo = new LoadInfoTask();
	
	Handler handler = new Handler()
	{

	    @Override
	    public void handleMessage(Message msg)
	    {
		switch(msg.what)
		{
			case Globals.REQUEST_SUCCESS:
			    showRide(loadInfo.getResult());
			break;
			
			case Globals.REQUEST_ERROR_SERVER:
			    Toast.makeText(RideInfoActivity.instance, R.string.server_error, Toast.LENGTH_LONG).show();
			break;
			
			case Globals.REQUEST_ERROR_NETWORK:
			    Toast.makeText(RideInfoActivity.instance, R.string.network_error, Toast.LENGTH_LONG).show();
			break;
		}
		
		loadingDialog.dismiss();
	    }
	};
	
	loadInfo.loadInfo(rideID, handler);
    }
    
    private void showRide(Ride ride)
    {
	Resources res = getResources();
	
	// From and to
	TextView fromText = (TextView)findViewById(R.id.fromText);
	fromText.setText(ride.getFrom());
	
	TextView toText = (TextView)findViewById(R.id.toText);
	toText.setText(ride.getTo());
	
	// Time and date
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	
	TextView timeText = (TextView)findViewById(R.id.timeText);
	timeText.setText(timeFormatter.format(ride.getTime()));
	
	TextView dayText = (TextView)findViewById(R.id.dayText);
	dayText.setText(LocaleUtil.getDayName(res, ride.getTime()) + ",");
	
	TextView dateText = (TextView)findViewById(R.id.dateText);
	dateText.setText(LocaleUtil.getFormattedDate(res, ride.getTime()));
	
	// Price and number of people
	TextView priceText = (TextView)findViewById(R.id.priceText);
	
	if (ride.getPrice() != null)
	{
	    priceText.setText(String.format("%1.1f", ride.getPrice()));
	}
	else
	{
	    priceText.setText("?");
	}
	
	TextView pplText = (TextView)findViewById(R.id.personsText);
	pplText.setText(String.valueOf(ride.getPeople()));
	
	// Driver and contact
	TextView driverText = (TextView)findViewById(R.id.driverText);
	driverText.setText(ride.getAuthor());
	
	TextView contactText = (TextView)findViewById(R.id.contactText);
	contactText.setText(ride.getContact());
	
	// Comment
	TextView commentText = (TextView)findViewById(R.id.detailsText);
	commentText.setText(ride.getComment());
    }

}
