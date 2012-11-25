package org.prevoz.android;

public class City implements Comparable<City>
{
	private String displayName;
	private String countryCode;
	
	public City(String displayName, String countryCode) 
	{
		this.displayName = displayName;
		this.countryCode = countryCode;
	}

	public String getDisplayName() 
	{
		return displayName;
	}

	public String getCountryCode() 
	{
		return countryCode;
	}
	
	@Override
	public String toString()
	{
		return getDisplayName() + (countryCode.equals("SI") ? "" : " (" + getCountryCode() + ")");
	}

	@Override
	public int compareTo(City another) 
	{
		return getDisplayName().compareTo(another.getDisplayName());
	}
	
}