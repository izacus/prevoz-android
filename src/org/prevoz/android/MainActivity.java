package org.prevoz.android;

import org.prevoz.android.add_ride.AddRideActivity;
import org.prevoz.android.my_rides.MyRidesActivity;
import org.prevoz.android.search.SearchActivity;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.TabsUtil;

import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TextView;

public class MainActivity extends TabActivity 
{
    private static MainActivity instance = null;
    
    public static MainActivity getInstance()
    {
	return instance;
    }
    
    private static final int MENU_ABOUT = 0;
    
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
	TabsUtil.addNativeLookingTab(this, tabHost, "my_rides", getString(R.string.my_rides_tab), R.drawable.myrides_tab, myRidesIntent);
	
	Intent searchIntent = new Intent(this, SearchActivity.class);
	TabsUtil.addNativeLookingTab(this, tabHost, "search", getString(R.string.search_tab), R.drawable.search_tab, searchIntent);
	
	Intent addRideIntent = new Intent(this, AddRideActivity.class);
	TabsUtil.addNativeLookingTab(this, tabHost, "add_ride", getString(R.string.add_tab), R.drawable.add_tab, addRideIntent);
	
	if (Database.getFavorites(this).size() > 0)
	{
	    tabHost.setCurrentTab(0);
	}
	else
	{
	    tabHost.setCurrentTab(1);
	}
    }
    
    public void switchToSearch()
    {
	tabHost.setCurrentTab(1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	super.onCreateOptionsMenu(menu);
	menu.add(0, MENU_ABOUT, 0, getString(R.string.about)).setIcon(android.R.drawable.ic_menu_info_details);
	
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
	switch(item.getItemId())
	{
	   case MENU_ABOUT:
	       final Dialog newDialog = new Dialog(this);
	       newDialog.setTitle(getString(R.string.app_name) + " " + getString(R.string.app_version));
	       
	       TextView text = new TextView(this);
	       text.setText(getString(R.string.about_text));
	       text.setPadding(10, 10, 10, 10);
	       text.setTextColor(Color.WHITE);
	       
	       newDialog.setContentView(text);
	       newDialog.setCanceledOnTouchOutside(true);
	       newDialog.show();
	       return false;
	      
	   default:
	       return super.onOptionsItemSelected(item);
	}
    }
}