package org.prevoz.android;

public enum RideType
{
	SHARE("ponujeni prevozi"), SEEK("iskalci prevoza");

	private String displayName;

	private RideType(String name)
	{
		this.displayName = name;
	}

	public String toString()
	{
		return this.displayName;
	}
}
