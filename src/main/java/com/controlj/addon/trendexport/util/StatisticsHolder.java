package com.controlj.addon.trendexport.util;

import java.util.ArrayList;
import java.util.List;

public class StatisticsHolder
{
    private List<Statistics> statisticsList;
    private static final int STORED_COLLECTION_LIMIT = 5;

    public StatisticsHolder()
    {
        statisticsList = new ArrayList<Statistics>();
    }

    public List<Statistics> getStatisticsList()
    {
        return statisticsList;
    }

    public void addStatistics(Statistics s)
    {
        maintainListLength();
        statisticsList.add(s);
    }

    private void maintainListLength()
    {
        while (statisticsList.size() > STORED_COLLECTION_LIMIT)
            statisticsList.remove(0);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(statisticsList.size());

        for (Statistics s : statisticsList)
            builder.append(s.toString()).append(";");

        return builder.toString();
    }
}
