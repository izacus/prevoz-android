package org.prevoz.android.events;

import java.util.Calendar;

/**
 * Created by jernej on 26/01/14.
 */
public class Events
{
    public static class NewSearchEvent
    {
        public final String from;
        public final String to;
        public final Calendar date;

        public NewSearchEvent(String from, String to, Calendar date)
        {
            this.from = from;
            this.to = to;
            this.date = date;
        }
    }
}
