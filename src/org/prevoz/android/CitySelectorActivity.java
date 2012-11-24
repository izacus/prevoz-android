package org.prevoz.android;

import java.util.HashMap;
import java.util.List;

import org.prevoz.android.util.Database;
import org.prevoz.android.util.GPSManager;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;

public class CitySelectorActivity extends SherlockFragmentActivity implements TextWatcher, OnItemClickListener, OnEditorActionListener
{
	private SQLiteDatabase database = null;	
	private ListView cityList;
	private EditText cityText;
	private ImageButton gpsButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.city_selector_activity);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Disable status bar in landscape layout
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		// Prepare view items
		cityList = (ListView) findViewById(R.id.city_list);
		cityList.setOnItemClickListener(this);
		cityText = (EditText) findViewById(R.id.city_text);
		cityText.addTextChangedListener(this);
		cityText.setOnEditorActionListener(this);
		cityText.requestFocus();
		
		gpsButton = (ImageButton)findViewById(R.id.gps_button);
		gpsButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				fillInCurrentLocation(cityText);
			}
		});
		
		// Prepare database connection
		database = Database.getSettingsDatabase(this);
		populateCityList("");
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home)
		{
			setResult(RESULT_CANCELED);
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private void populateCityList(String pattern)
	{
		if (database == null || !database.isOpen())
		{
			database = Database.getSettingsDatabase(this);
		}
		
		List<City> cities = Database.getCitiesStartingWith(database, pattern);
		cityList.setAdapter(new CityListAdapter(this, android.R.layout.simple_list_item_2, cities));

		Log.d(this.toString(), "Populating city list with pattern " + pattern);
	}

	private void fillInCurrentLocation(final TextView view)
	{
		FlurryAgent.logEvent("CitySelector - GPS");
		view.setEnabled(false);
		
		final String currentText = view.getText().toString();
		final String currentHint = view.getHint().toString();
		
		view.setText("");
		view.setHint(getResources().getString(R.string.search_gps_locating));
		
		// Determine location
		final GPSManager gpsManager = new GPSManager();
		Handler callback = new Handler()
		{
			@Override
			public void handleMessage(Message msg) 
			{
				view.setHint(currentHint);
				
				if (msg.what == GPSManager.GPS_PROVIDER_UNAVALABLE)
				{
					Toast.makeText(CitySelectorActivity.this, R.string.search_gps_error, Toast.LENGTH_SHORT).show();
					// Restore old entry
					view.setText(currentText);
					return;
				}
				
				returnCity(gpsManager.getCurrentCity());
			}
		};
		
		gpsManager.findCurrentCity(this, callback);
	}
	

	public void afterTextChanged(Editable s) 
	{
		// Nothing TBD
	}


	public void beforeTextChanged(CharSequence s, 
								  int start, 
								  int count,
								  int after) 
	{
		// Nothing TBD
	}


	public void onTextChanged(CharSequence s, int start, int before, int count) 
	{
		populateCityList(s.toString());
	}


	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	{
		String name = ((TextView)view.findViewById(android.R.id.text1)).getText().toString();
		returnCity(name);
	}

	public boolean onEditorAction(TextView v, 
								  int actionId, 
								  KeyEvent event) 
	{
		if ((cityList.getCount() > 1) || (cityList.getCount() == 0))
		{
			FlurryAgent.logEvent("CitySelector - Selection Canceled");
			setResult(RESULT_CANCELED);
			finish();
		}
		else 
		{
			String name = ((TextView)cityList.getChildAt(0)).getText().toString();
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("city", name);
			FlurryAgent.logEvent("CitySelector - City Selected", params);
			returnCity(name);
		}
		
		return false;
	}
	
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		FlurryAgent.setReportLocation(false);
		FlurryAgent.onStartSession(this, getString(R.string.flurry_apikey));
	}
	
	
	@Override
	protected void onStop() 
	{
		super.onStop();
		database.close();
		FlurryAgent.onEndSession(this);
	}


	private void returnCity(String name) {
		Intent result = new Intent();
		result.putExtra("city", name);
		
		setResult(RESULT_OK, result);
		finish();
	}	
}
