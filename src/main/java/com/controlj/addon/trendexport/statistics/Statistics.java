package com.controlj.addon.trendexport.statistics;

import java.util.Date;

public class Statistics
{
    private final Date date;
    private final long elapsedTime;
    private final long samples;

    public Statistics(Date singleDate, long time, long numSamples)
    {
        date = singleDate;
        elapsedTime = time;
        samples = numSamples;
    }

    public Date getDate()
    {
        return date;
    }

    public Long getElapsedTime()
    {
        return elapsedTime;
    }

    public Long getSamples()
    {
        return samples;
    }
}
