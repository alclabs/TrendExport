package com.controlj.addon.trendexport.statistics;

import java.util.Date;

public class StatisticsAccumulator
{
    private Date date;
    private long elapsedTime;
    private long samples;

    public StatisticsAccumulator()
    {
        date = new Date(0);
        elapsedTime = 0;
        samples = 0;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public long getElapsedTime()
    {
        return elapsedTime;
    }

    public void addElapsedTime(long elapsedTime)
    {
        this.elapsedTime += elapsedTime;
    }

    public long getSamples()
    {
        return samples;
    }

    public void addSamples(long samples)
    {
        this.samples += samples;
    }
}
