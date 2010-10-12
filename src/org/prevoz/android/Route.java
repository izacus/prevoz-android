package org.prevoz.android;

/**
 * Carries basic route information
 * @author Jernej Virag
 *
 */
public class Route
{
    private String from;
    private String to;
    private RideType type;
    
    public Route(String from, String to, RideType type)
    {
	super();
	this.from = from;
	this.to = to;
	this.type = type;
    }

    public String getFrom()
    {
        return from;
    }

    public String getTo()
    {
        return to;
    }

    @Override
    public String toString()
    {
	return from + " - " + to + " of type " + type;
    }

    public RideType getType()
    {
        return type;
    }
}
