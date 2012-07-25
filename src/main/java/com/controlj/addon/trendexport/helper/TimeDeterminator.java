package com.controlj.addon.trendexport.helper;

import com.controlj.addon.trendexport.config.Configuration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimeDeterminator
{
    private static final int MILLIS_TO_HOURS = 1000 * 60 * 60;

    // next scheduled time will be in this time plus the interval -> set at calculateNextScheduledCollection()
    private final Calendar scheduledCollection;
    private final Configuration.CollectionMethod collectionMethod;
    private final long intervalInMillis;

    // factory method for itself? O_o
    public static TimeDeterminator getTimeDeterminator(String timeStringToParse)
    {
        // time based or not?

        String[] parts = timeStringToParse.split(":");
        if (timeStringToParse.contains(":"))
        {
            // parse time
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            boolean isAmOrPm = parts[2].equals("AM");

            return new TimeDeterminator(hours, minutes, isAmOrPm);
        }
        else
        {
            return new TimeDeterminator(Integer.parseInt(parts[0]));
        }
    }

    public TimeDeterminator(int intervalInHours)
    {
        this.collectionMethod = Configuration.CollectionMethod.Interval;
        this.intervalInMillis = convertHoursToMs(intervalInHours);
        this.scheduledCollection = GregorianCalendar.getInstance();
    }

    // for specified time (based on 12 hour clock)
    public TimeDeterminator(int hourOfCollection, int minuteOfCollection, boolean isAM)
    {
        this.collectionMethod = Configuration.CollectionMethod.SpecifiedTime;
        this.intervalInMillis = convertHoursToMs(24);  // interval is fixed for 24 hours (once a day)

        // set time for the specified time - set for now in case it well now
        this.scheduledCollection = GregorianCalendar.getInstance();
        this.scheduledCollection.set(Calendar.HOUR, hourOfCollection == 12 ? 0 : hourOfCollection);
        this.scheduledCollection.set(Calendar.MINUTE, minuteOfCollection);
        this.scheduledCollection.set(Calendar.SECOND, 0);
        this.scheduledCollection.set(Calendar.MILLISECOND, 0);
        this.scheduledCollection.set(Calendar.AM_PM, isAM ? Calendar.AM : Calendar.PM);
    }

    private long getIntervalInMillis()
    {
        return intervalInMillis;
    }

    public long getIntervalInHours()
    {
        return convertMsToHours(getIntervalInMillis());
    }

    // needs to calculate the calendar date based on info given
    public Calendar calculateNextScheduledCollection()
    {
        //
        // hopefully this adds dates as well and rolls everything needed
        //

        // add the interval to the current scheduledCollection until the time is valid
        while (!isCurrentCollectionDateValid(this.scheduledCollection))
        {
            this.scheduledCollection.add(Calendar.HOUR_OF_DAY, (int) getIntervalInHours());
        }

        return this.scheduledCollection; // present time in milliseconds
    }

    // valid means that the scheduled collection is scheduled for the future (after now)
    private boolean isCurrentCollectionDateValid(Calendar scheduledCollection)
    {
        return scheduledCollection.after(GregorianCalendar.getInstance());
    }

    public long calculateInitialDelay()
    {
        /*
        * Notes:
        * 1) Specified time - determine next scheduled date.  return the difference the time between the next scheduled date and now.
        * 2) Interval       - if the current scheduled date is before now (i.e. the collection date is not valid)
        * */
        return this.collectionMethod == Configuration.CollectionMethod.Interval ? convertMsToHours(getIntervalInMillis()) : calculateNextScheduledCollection().getTimeInMillis();
    }

    public long calculateInterval()
    {
        /*
        * Notes:
        * 1) Specified time - determine next scheduled date. Calculate the time between then and now.
        * 2) Interval       - Fixed given that the interval is the value given...
        * */

        return this.collectionMethod == Configuration.CollectionMethod.Interval ? getIntervalInMillis() : convertHoursToMs(24);
    }

    public String getNextCollectionTimeForStatus()
    {
        // get formatted string for calculated time here
        String formatPattern = getFormatPattern(this.scheduledCollection);
        return new SimpleDateFormat(formatPattern).format(this.scheduledCollection.getTime());
    }

    private String getFormatPattern(Calendar date)
    {
        if (date.get(Calendar.DAY_OF_MONTH) == GregorianCalendar.getInstance().get(Calendar.DAY_OF_MONTH))
            return "'Today at' h:mm a"; // format: "Today at " + Time of day + "AM/PM"
        else
            return "MM/dd/yy 'at' hh:mm a"; // format "day/month/year at Time + AM/PM
    }

    private long convertMsToHours(long ms)
    {
        return ms / MILLIS_TO_HOURS;
    }

    private long convertHoursToMs(int hours)
    {
        return hours * MILLIS_TO_HOURS;
    }
}

