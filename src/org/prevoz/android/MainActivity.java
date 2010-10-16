package org.prevoz.android;

import org.prevoz.android.add_ride.AddRideActivity;
import org.prevoz.android.my_rides.MyRidesActivity;
import org.prevoz.android.search.SearchActivity;
import org.prevoz.android.util.TabsUtil;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class MainActivity extends TabActivity 
{
    private static MainActivity instance = null;
    
    public static MainActivity getInstance()
    {
	return instance;
    }
    
    private TabHost tabHost;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        MainActivity.instance = this;
        
        // Set custom titlebar
        setContentView(R.layout.main_activity);

        // Prepare tabs for use
        initTabs();
    }
    
    private void initTabs()
    {
	tabHost = getTabHost();
	
	Intent myRidesIntent = new Intent(this, MyRidesActivity.class);
	TabsUtil.addNativeLookingTab(this, tabHost, "my_rides", getString(R.string.my_rides_tab), R.drawable.friends_tab, myRidesIntent);
	
	Intent searchIntent = new Intent(this, SearchActivity.class);
	TabsUtil.addNativeLookingTab(this, tabHost, "search", getString(R.string.search_tab), R.drawable.search_tab, searchIntent);
	
	Intent addRideIntent = new Intent(this, AddRideActivity.class);
	TabsUtil.addNativeLookingTab(this, tabHost, "add_ride", getString(R.string.add_tab), R.drawable.friends_tab, addRideIntent);
	
	tabHost.setCurrentTab(0);
    }
    
    public void switchToSearch()
    {
	tabHost.setCurrentTab(1);
    }
}