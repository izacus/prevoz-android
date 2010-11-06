package org.prevoz.android.rideinfo;

import java.text.SimpleDateFormat;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RideInfoActivity extends Activity
{
    public static final String RIDE_ID = RideInfoActivity.class.toString() + ".ride_id";
    public static final int RIDE_DELETED = Activity.RESULT_FIRST_USER;
    
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
	if (ride != null)
	{
	    outState.putBoolean("suspended", true);
	    ride.storeToBundle(outState);
	}
	
	super.onSaveInstanceState(outState);
    }
    
    private void loadRideData()
    {
	loadingDialog = ProgressDialog.show(this, "", getString(R.string.loading));
	
	HTTPHelper.updateSessionCookies(this);
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
	setContentView(R.layout.ride_info_activity);
	
	Resources res = getResources();
	
	// From and to
	TextView fromText = (TextView)findViewById(R.id.rideinfo_from);
	fromText.setText(ride.getFrom());
	
	TextView toText = (TextView)findViewById(R.id.rideinfo_to);
	toText.setText(ride.getTo());
	
	// Time and date
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	
	TextView timeText = (TextView)findViewById(R.id.rideinfo_time);
	timeText.setText(timeFormatter.format(ride.getTime()));
	
	TextView dayText = (TextView)findViewById(R.id.rideinfo_day);
	dayText.setText(LocaleUtil.getDayName(res, ride.getTime()) + ",");
	
	TextView dateText = (TextView)findViewById(R.id.rideinfo_date);
	dateText.setText(LocaleUtil.getFormattedDate(res, ride.getTime()));
	
	// Price and number of people
	TextView priceText = (TextView)findViewById(R.id.rideinfo_price);
	
	if (ride.getPrice() != null)
	{
	    priceText.setText(String.format("%1.1f €", ride.getPrice()));
	}
	else
	{
	    priceText.setText("?");
	}
	
	TextView pplText = (TextView)findViewById(R.id.rideinfo_people);
	pplText.setText(String.valueOf(ride.getPeople()));
	
	TextView pplTagText = (TextView)findViewById(R.id.rideinfo_peopletag);
	pplTagText.setText(LocaleUtil.getStringNumberForm(res, R.array.people_tags, ride.getPeople()));
	
	// Driver and contact
	
	TextView driverText = (TextView)findViewById(R.id.rideinfo_author);
	
	if (ride.getAuthor() == null || ride.getAuthor().trim().length() == 0)
	{
	    driverText.setVisibility(View.GONE);
	}
	else
	{
	    driverText.setVisibility(View.VISIBLE);
	    driverText.setText(ride.getAuthor());
	}
	
	TextView contactText = (TextView)findViewById(R.id.rideinfo_phone);
	contactText.setText(ride.getContact());
	
	// Comment
	TextView commentText = (TextView)findViewById(R.id.rideinfo_comment);
	commentText.setText(ride.getComment());
	
	// Setup button callbacks
	((Button)findViewById(R.id.rideinfo_call)).setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		callAuthor();
	    }
	});
	
	((Button)findViewById(R.id.rideinfo_sms)).setOnClickListener(new OnClickListener()
	{
	    public void onClick(View v)
	    {
		sendSMS();
	    }
	});
	
	// Setup delete button
	if (ride.isAuthor())
	{
	    Button delButton = (Button)findViewById(R.id.rideinfo_delsend);
	    delButton.setText(R.string.delete);
	    delButton.setVisibility(View.VISIBLE);
	    
	    final int id = rideID;
	    
	    delButton.setOnClickListener(new OnClickListener()
	    {
	        public void onClick(View v)
	        {
	            deleteRide(id);
	        }
	    });
	}
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
    
    /**
     * Adds this route to favorites
     */
    private void addToFavorites()
    {
	Database.addFavorite(this, ride.getFrom(), ride.getTo(), ride.getType());
	Toast.makeText(this, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Opens SMS messaging application with authors phone number entered
     */
    private void sendSMS()
    {
	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + ride.getContact()));
	this.startActivity(intent);
    }
    
    /**
     * Opens the dialer with authors phone number entered
     */
    private void callAuthor()
    {
	Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ride.getContact()));
	this.startActivity(intent);
    }
    
    private void deleteRide(int id)
    {
	final ProgressDialog deleteDialog = ProgressDialog.show(this, null, getString(R.string.deleting));
	
	DeleteRideTask deleteRide = new DeleteRideTask();
	Handler callback = new Handler()
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		deleteDialog.dismiss();
		setResult(RIDE_DELETED);
		finish();
	    }
	};
	
	deleteRide.startTask(id, callback);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
	switch(item.getItemId())
	{
	    case MENU_ADD_FAVORITES:
		addToFavorites();
		break;
		
	    default:
		Log.e(this.toString(), "Unknown menu option selected!");
		break;
	}
	
	return false;
    }

}
