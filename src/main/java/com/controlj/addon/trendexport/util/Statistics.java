package com.controlj.addon.trendexport.util;

import java.util.Date;

public class Statistics
{
    private Date date;
    private Long elapsedTime;
    private Long samples;

    public Statistics()
    {

    }

    public Statistics(Date singleDate, long time, long numSamples)
    {
        date = singleDate;
        elapsedTime = time;
        samples = numSamples;
    }

    public void setDate(Date date, long collectionDuration)
    {
        this.date = date;
        this.elapsedTime = collectionDuration;
    }

    public void addSamples(long samples)
    {
        this.samples = samples;
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


