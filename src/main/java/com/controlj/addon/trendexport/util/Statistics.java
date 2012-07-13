package com.controlj.addon.trendexport.util;

import java.util.Date;

public class Statistics
{
    private Date date;
    private long elapsedTime;
    private long samples;

    public Statistics()
    {
        date = new Date();
        elapsedTime = 0;
        samples = 0;
    }

    public Statistics(Date singleDate, long time, long numSamples)
    {
        date = singleDate;
        elapsedTime = time;
        samples = numSamples;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public void setElapsedTime(long time)
    {
        elapsedTime = time;
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

    @Override
    public String toString()
    {
        return date.getTime() + ";" + elapsedTime + ";" + samples;
    }
}


