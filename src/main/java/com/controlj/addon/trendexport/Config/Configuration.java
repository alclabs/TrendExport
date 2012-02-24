package com.controlj.addon.trendexport.Config;

import com.controlj.addon.trendexport.util.ErrorHandler;
import org.jetbrains.annotations.NotNull;

public class Configuration
{
    public enum CollectionMethod
    {
        Interval, SpecifiedTime
    }

    private long collectionValue;
    private CollectionMethod collectionMethod;
    @NotNull private String alarmControlProgramPath = "";
    private ErrorHandler errorHandler;

    public Configuration(long collVal, CollectionMethod method)
    {
        this.collectionValue = collVal;
        this.collectionMethod = method;

        this.errorHandler = new ErrorHandler();
    }

    public Configuration(long collVal, CollectionMethod method, @NotNull String alarmPath)
    {
        this.collectionValue = collVal;
        this.collectionMethod = method;
        this.alarmControlProgramPath = alarmPath;

        this.errorHandler = new ErrorHandler(alarmPath);
    }

    public CollectionMethod getCollectionMethod()
    {
        return this.collectionMethod;
    }

    public long getCollectionValue()
    {
        return this.collectionValue;
    }

    @NotNull public String getAlarmControlProgramPath()
    {
        return alarmControlProgramPath;
    }

    public ErrorHandler getErrorHandler()
    {
        return errorHandler;
    }
}
