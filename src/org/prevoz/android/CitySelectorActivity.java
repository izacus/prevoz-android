package org.prevoz.android;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
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

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;

import roboguice.inject.InjectView;

public class CitySelectorActivity extends RoboSherlockActivity implements TextWatcher, OnItemClickListener, OnEditorActionListener
{
    private static final String LOG_TAG = "Prevoz.CitySelector";
    private SQLiteDatabase database = null;
	
	@InjectView(R.id.city_list)
	private ListView cityList;
	@InjectView(R.id.city_text)
	private EditText cityText;
	@InjectView(R.id.gps_button)
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
		cityList.setOnItemClickListener(this);
		
		cityText.addTextChangedListener(this);
		cityText.setOnEditorActionListener(this);
		cityText.requestFocus();
		
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

		Log.d(LOG_TAG, "Populating city list with pattern " + pattern);
	}

	private void fillInCurrentLocation(final TextView view)
	{
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
				
				City city = gpsManager.getCurrentCity();
				returnCity(city.getDisplayName(), city.getCountryCode());
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
		City city = (City)(view.getTag());
		returnCity(city.getDisplayName(), city.getCountryCode());
	}

	public boolean onEditorAction(TextView v, 
								  int actionId, 
								  KeyEvent event) 
	{
		if ((cityList.getCount() > 1) || (cityList.getCount() == 0))
		{
			setResult(RESULT_CANCELED);
			finish();
		}
		else 
		{
			City city = (City)(cityList.getChildAt(0).getTag());
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("city", city.getDisplayName());
			returnCity(city.getDisplayName(), city.getCountryCode());
		}
		
		return false;
	}
	
	
	@Override
	protected void onStart() 
	{
		super.onStart();
	}
	
	
	@Override
	protected void onStop() 
	{
		super.onStop();
		database.close();
	}


	private void returnCity(String name, String country) 
	{
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromInputMethod(cityText.getWindowToken(), 0);
		Intent result = new Intent();
		result.putExtra("city", name);
		result.putExtra("country", country);
		setResult(RESULT_OK, result);
		finish();
	}
}
