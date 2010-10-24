package org.prevoz.android;

import java.io.IOException;

import org.prevoz.android.add_ride.AddRideActivity;
import org.prevoz.android.my_rides.MyRidesActivity;
import org.prevoz.android.search.SearchActivity;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.TabsUtil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends TabActivity 
{
    private static MainActivity instance = null;
    
    public static MainActivity getInstance()
    {
	return instance;
    }
    
    private static final int MENU_ABOUT = 0;
    private static final int MENU_LOGOUT = 1;
    
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
        
        // Check for updates
        final UpdateCheckTask updateCheckTask = new UpdateCheckTask();
        Handler callback = new Handler()
        {
	    @Override
	    public void handleMessage(Message msg)
	    {
		showUpdateNotify(updateCheckTask);
	    }
        };
        
        updateCheckTask.checkForUpdate(this, callback);
    }
    
    private void showUpdateNotify(UpdateCheckTask task)
    {
	final UpdateCheckTask updateTask = task;
	
	if (task.hasNewVersion())
	{
	    AlertDialog dialog = new AlertDialog.Builder(this).create();
	    dialog.setTitle("Nova različica");
	    dialog.setMessage(task.getDescripton());
	    
	    dialog.setButton("V redu", new OnClickListener()
	    {
	        public void onClick(DialogInterface dialog, int which)
	        {
	            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateTask.getUrl()));
	            startActivity(myIntent);
	        }
	    });
	    
	    dialog.setButton2("Prekliči", new OnClickListener()
	    {
	        public void onClick(DialogInterface dialog, int which)
	        {
	            return;
	        }
	    });
	    
	    dialog.show();
	}
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
	menu.add(1, MENU_LOGOUT, 1, "Odjavi").setIcon(android.R.drawable.ic_menu_delete);
	
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
	       
	   case MENU_LOGOUT:
	       try
	       {
		   HTTPHelper.httpGet(Globals.API_URL + "/accounts/logout/");
	       } 
	       catch (IOException e)
	       {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
	       }
	       
	       CookieSyncManager.createInstance(this);
	       CookieManager cookieManager = CookieManager.getInstance();
	       cookieManager.removeAllCookie();
	       CookieSyncManager.getInstance().sync();
	       
	       Toast.makeText(this, "Odjava uspešna.", Toast.LENGTH_SHORT).show();
	       return false;
	      
	   default:
	       return super.onOptionsItemSelected(item);
	}
    }
}