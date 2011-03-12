package org.prevoz.android;

import java.util.Timer;
import java.util.TimerTask;

import org.prevoz.android.add_ride.AddRideActivity;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;
import org.prevoz.android.my_rides.MyRidesActivity;
import org.prevoz.android.search.SearchActivity;
import org.prevoz.android.util.Database;
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
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class OldMainActivity extends TabActivity
{
	private static OldMainActivity instance = null;

	public static OldMainActivity getInstance()
	{
		return instance;
	}

	private static final String UPDATE_NOTIFY_KEY = "updatenotifyshown";
	
	private static final int MENU_ABOUT = 0;
	private static final int MENU_LOGOUT = 1;
	private static final int MENU_LOGIN = 2;

	private boolean updateNotifyShown = false;
	
	private TabHost tabHost;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
		{	
			updateNotifyShown = savedInstanceState.getBoolean(UPDATE_NOTIFY_KEY);
		}
		
		OldMainActivity.instance = this;

		// Set custom titlebar
		setContentView(R.layout.main_activity);

		// Prepare tabs for use
		initTabs();
		
		// Check for updates
		if (!updateNotifyShown)
		{
			final UpdateCheckTask updateCheckTask = new UpdateCheckTask();
			final Handler callback = new Handler()
			{
				@Override
				public void handleMessage(Message msg)
				{
					showUpdateNotify(updateCheckTask);
				}
			};
	
			final OldMainActivity mainActivity = this;
			TimerTask ttask = new TimerTask()
			{
				@Override
				public void run()
				{
					updateCheckTask.checkForUpdate(mainActivity, callback);
				}
			};
	
			Timer t = new Timer(true);
			t.schedule(ttask, 1000);
		}
		
		// Check for current authentication status
		AuthenticationManager.getInstance().getAuthenticationStatus(this, false);
	}

	private void showUpdateNotify(UpdateCheckTask task)
	{
		if (updateNotifyShown)
			return;
		
		final UpdateCheckTask updateTask = task;

		if (task.hasNewVersion())
		{
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setTitle(getString(R.string.new_version));
			dialog.setMessage(task.getDescripton());

			dialog.setButton(getString(R.string.ok), new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateTask.getUrl()));
					startActivity(myIntent);
				}
			});

			dialog.setButton2(getString(R.string.cancel), new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					return;
				}
			});

			dialog.show();
			updateNotifyShown = true;
		}
	}

	private void initTabs()
	{
		tabHost = getTabHost();

		Intent myRidesIntent = new Intent(this, MyRidesActivity.class);
		TabsUtil.addNativeLookingTab(this, tabHost, "my_rides",
				getString(R.string.my_rides_tab), R.drawable.myrides_tab,
				myRidesIntent);

		Intent searchIntent = new Intent(this, SearchActivity.class);
		TabsUtil.addNativeLookingTab(this, tabHost, "search",
				getString(R.string.search_tab), R.drawable.search_tab,
				searchIntent);

		Intent addRideIntent = new Intent(this, AddRideActivity.class);
		TabsUtil.addNativeLookingTab(this, tabHost, "add_ride",
				getString(R.string.add_tab), R.drawable.add_tab, addRideIntent);

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
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		
		if (menu.findItem(MENU_ABOUT) == null)
		{
			menu.add(Menu.NONE , MENU_ABOUT, 0, getString(R.string.about)).setIcon(android.R.drawable.ic_menu_info_details);
		}
		
		
		if (AuthenticationManager.getInstance().getAuthenticationStatus(this, false) == AuthenticationStatus.AUTHENTICATED)
		{
			if (menu.findItem(MENU_LOGOUT) == null)
			{
				// Add logout button
				menu.add(Menu.NONE, MENU_LOGOUT, 1, getString(R.string.logout)).setIcon(android.R.drawable.ic_menu_delete);
				// Remove login button
				menu.removeItem(MENU_LOGIN);
			}
		}
		else
		{
			if (menu.findItem(MENU_LOGIN) == null)
			{
				menu.add(Menu.NONE, MENU_LOGIN, 1, getString(R.string.login)).setIcon(android.R.drawable.ic_menu_upload);
				menu.removeItem(MENU_LOGOUT);
			}
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case MENU_ABOUT:
			final Dialog newDialog = new Dialog(this);
			newDialog.setTitle(getString(R.string.app_name) + " "
					+ getString(R.string.app_version));

			TextView text = new TextView(this);
			text.setText(getString(R.string.about_text));
			text.setPadding(10, 10, 10, 10);
			text.setTextColor(Color.WHITE);

			newDialog.setContentView(text);
			newDialog.setCanceledOnTouchOutside(true);
			newDialog.show();
			return false;

		case MENU_LOGOUT:
			AuthenticationManager.getInstance().requestLogout(this);
			Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
			return false;
			
		case MENU_LOGIN:
			// Request authentication manager to log user in
			AuthenticationManager.getInstance().requestLogin(this);
			return false;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean(UPDATE_NOTIFY_KEY, updateNotifyShown);
	}
}