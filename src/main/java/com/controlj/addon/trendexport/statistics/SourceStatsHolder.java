package com.controlj.addon.trendexport.statistics;

import java.util.ArrayList;
import java.util.List;

public class SourceStatsHolder
{
    // Purpose is to hold onto and limit the amount of information about each collection

    private List<Statistics> statisticsList;
    private static final int STORED_COLLECTION_LIMIT = 5;

    public SourceStatsHolder()
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
}
