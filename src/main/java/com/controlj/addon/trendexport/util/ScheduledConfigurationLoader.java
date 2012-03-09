/*
* Copyright (c) 2011 Automated Logic Corporation
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/

package com.controlj.addon.trendexport.util;

import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.Configuration;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ScheduledConfigurationLoader
{
    // creates the delay and interval based on the current configuration type

    private final ConfigManager manager;

    public ScheduledConfigurationLoader(ConfigManager cm)
    {
        this.manager = cm;
    }

    public long calculateDelay()
    {
        if (manager.getConfiguration().getCollectionMethod() == Configuration.CollectionMethod.Interval)
            return 0;
        else
            return calculateDateDelay();
    }

    public long calculateInterval()
    {
        if (manager.getConfiguration().getCollectionMethod() == Configuration.CollectionMethod.Interval)
            return manager.getConfiguration().getCollectionValue();
        else
            return 24;
    }

    private long calculateDateDelay()
    {
        // time of collection of this day - schedule will handle the 24hrs...only recalculate on (re)initialize
        long rawTimeMilliseconds = manager.getConfiguration().getCollectionValue();
        long rawTimeMinutes = rawTimeMilliseconds / 60000; // convert to minutes

        int hours = (int) (rawTimeMinutes / 60);
        if (hours > 12)
            hours /= 12;

        int minutes = (int) (rawTimeMinutes % 60);

        // Time of collection
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date timeOfCollection = calendar.getTime();

        // Current time
        Date currentTime = new Date();

        // calculate initial delay
        if (currentTime.before(timeOfCollection))
            return timeOfCollection.getTime() - currentTime.getTime();
        else
        {
            // the current time is after the requested collection time so we need to increment the calendar by one day and wait
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            return calendar.getTimeInMillis() - currentTime.getTime();
        }
    }
}
