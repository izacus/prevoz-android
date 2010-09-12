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
    
    public Route(String from, String to)
    {
	super();
	this.from = from;
	this.to = to;
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
	return from + " - " + to;
    }
    
    
}
