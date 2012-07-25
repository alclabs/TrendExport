package com.controlj.addon.trendexport.config;

import com.controlj.addon.trendexport.util.ErrorHandler;
import org.jetbrains.annotations.NotNull;

public class Configuration
{
    public enum CollectionMethod
    {
        Interval, SpecifiedTime
    }

    private long collectionValue;

    private String collectionString;
    private CollectionMethod collectionMethod;
    @NotNull
    private String alarmControlProgramPath = "";
    private ErrorHandler errorHandler;

    public Configuration(String collVal, CollectionMethod method)
    {
        this.collectionString = collVal;
        this.collectionMethod = method;

        this.errorHandler = new ErrorHandler();
    }

    public Configuration(String collVal, CollectionMethod method, @NotNull String alarmPath)
    {
        this.collectionString = collVal;
        this.collectionMethod = method;
        this.alarmControlProgramPath = alarmPath;

        this.errorHandler = new ErrorHandler(alarmPath);
    }

    public CollectionMethod getCollectionMethod()
    {
        return this.collectionMethod;
    }

    // convert time into a value for Date but this is ineffective...
    /*public long getCollectionValue()
    {
        if (!this.collectionString.contains(":"))
            return Long.parseLong(this.collectionString) * 60 * 60 * 1000;
        else
        {
            long time;
            int start = collectionString.indexOf(':');
            int end = collectionString.lastIndexOf(':');
            int hours = Integer.parseInt(collectionString.substring(0, start));
            int minutes = Integer.parseInt(collectionString.substring(start + 1, end));

            // hours -> ms = 60 * 60 * 1000
            time = hours * 3600000 + (minutes * 60000);
            if (collectionString.contains("AM"))
                time *= -1;

            return time;
        }
    }*/

    @NotNull
    public String getAlarmControlProgramPath()
    {
        return alarmControlProgramPath;
    }

    public ErrorHandler getErrorHandler()
    {
        return errorHandler;
    }

    public String getCollectionString()
    {
        return collectionString;
    }

}
