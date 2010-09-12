package org.prevoz.android.rideinfo;

import java.text.SimpleDateFormat;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RideInfoActivity extends Activity
{
    public static final String RIDE_ID = RideInfoActivity.class.toString() + ".ride_id";
    private static final int MENU_ADD_FAVORITES = Menu.FIRST;
    
    private static RideInfoActivity instance = null;
    
    private int rideID;
    private Ride ride = null;
    private ProgressDialog loadingDialog = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	RideInfoActivity.instance = this;
	
	// Create an empty layout to speed up loading transition
	LinearLayout layout = new LinearLayout(this);
	setContentView(layout);
	
	// Get ride ID
	if (getIntent().getExtras() != null)
	{
	    rideID = getIntent().getExtras().getInt(RIDE_ID);
	    Log.d(this.toString(), "Requesting ride info for id " + rideID);
	}
	
	
	if (savedInstanceState != null && 
	    savedInstanceState.containsKey("suspended") && 
	    savedInstanceState.getBoolean("suspended"))
	{
	    ride = new Ride(savedInstanceState);
	    rideID = ride.getId();
	    showRide(ride);
	}
	else
	{
	    loadRideData();
	}
    }
    
    @Override
    /**
     * Called when activity is about to be killed
     */
    protected void onSaveInstanceState(Bundle outState)
    {
	outState.putBoolean("suspended", true);
	ride.storeToBundle(outState);
	super.onSaveInstanceState(outState);
    }
    
    private void loadRideData()
    {
	loadingDialog = ProgressDialog.show(this, "", getString(R.string.loading));
	
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
	this.ride = ride;
	setContentView(R.layout.ride_info);
	
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
	
	Debug.stopMethodTracing();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	super.onCreateOptionsMenu(menu);
	// Do not display menu if ride is not loaded yet
	if (ride == null)
	    return false;
	
	// Add favorite option
	menu.add(0, MENU_ADD_FAVORITES, 0, getString(R.string.add_to_favorites)).setIcon(android.R.drawable.ic_menu_add);
	
	return true;
    }
    
    private void AddToFavorites()
    {
	Database.addFavorite(this, ride.getFrom(), ride.getTo());
	Toast.makeText(this, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
	switch(item.getItemId())
	{
	    case MENU_ADD_FAVORITES:
		AddToFavorites();
		break;
		
	    default:
		Log.e(this.toString(), "Unknown menu option selected!");
		break;
	}
	
	return false;
    }

}
