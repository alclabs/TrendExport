package com.controlj.addon.trendexport.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Statistics
{
    private static final int MAX_LIST_LENGTH = 5;
    private List<Date> dateCollection;
    private List<Long> elapsedTimeList;
    private List<Long> samplesList;

    public Statistics()
    {
        this.dateCollection = new ArrayList<Date>();
        this.elapsedTimeList = new ArrayList<Long>();
        this.samplesList = new ArrayList<Long>();
    }

    public Statistics(List<Date> dates, List<Long> elapsedTimes, List<Long> samples)
    {
        this.dateCollection = dates;
        this.elapsedTimeList = elapsedTimes;
        this.samplesList = samples;
    }

    public void addStatistics(Date date, long collectionDuration)
    {
        dateCollection = checkListConstraint(dateCollection);
        dateCollection.add(date);

        elapsedTimeList = checkListConstraint(elapsedTimeList);
        elapsedTimeList.add(collectionDuration);
    }

    public void addSamples(long samples)
    {
        samplesList = checkListConstraint(samplesList);
        samplesList.add(samples);
    }

    public List<Date> getDates()
    {
        return dateCollection;
    }

    public List<Long> getCollectionDurationsList()
    {
        return elapsedTimeList;
    }

    public List<Long> getSampleCollection()
    {
        return samplesList;
    }

    public long getLatestSampleCollection()
    {
        return getSampleCollection().get(getCurrentNumberOfRecords() - 1);
    }

    public long getTotalDuration()
    {
        return sumOfList(elapsedTimeList);
    }

    public long getTotalSamples()
    {
        return sumOfList(elapsedTimeList);
    }

    // checks the length and removes the first element until it fits under the length of the max size
    private List checkListConstraint(List list)
    {
        if ((list.size() + 1) > MAX_LIST_LENGTH)
            list.remove(0); // remove first element

        return list;
    }

    private long sumOfList(List<Long> list)
    {
        long sum = 0;
        for (Long l : list)
            sum += l;

        return sum;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(dateCollection.size()).append(";");
        for (Date d : dateCollection)
            builder.append(d.getTime()).append(";");

//        builder.append(elapsedTimeList.size()).append(";");
        for (Long l : elapsedTimeList)
            builder.append(l).append(";");

//        builder.append(samplesList.size()).append(";");
        for (Long l : samplesList)
            builder.append(l).append(";");

        return builder.toString();
    }

    public int getCurrentNumberOfRecords()
    {
        return dateCollection.size();
    }
}


