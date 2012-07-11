package com.controlj.addon.trendexport.util;

// class is here to maintain a list of stats

import com.controlj.green.addonsupport.access.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StatisticsCollector
{
    private static final int STORED_COLLECTION_LIMIT = 5;

    // global map of every collection containing the source that was collected as well as the statistics
    private static List<Map<String, Statistics>> globalStats;
    private static final StatisticsCollector STATISTICS_COLLECTOR = new StatisticsCollector();
    private static final Lock lock = new ReentrantLock(true);
    private static final String STATS_DATASTORE_NAME = "TrendExportStats";

    public static StatisticsCollector getStatisticsCollector()
    {
        return STATISTICS_COLLECTOR;
    }

    private StatisticsCollector()
    {
        globalStats = new ArrayList<Map<String, Statistics>>();
    }

    public void storeCollectionStatistics(Map<String, Statistics> collectionStats)
    {
        // check and fix length of collection list - keeps size of globalStats of collections to STORED_COLLECTION_LIMIT
        checkAndCorrectStorageLimit();
        globalStats.add(collectionStats);
    }

    private void checkAndCorrectStorageLimit()
    {
        if (globalStats.size() < STORED_COLLECTION_LIMIT)
            return;

        globalStats.remove(0);
    }

    public List<Statistics> getStatisticsForSource(String source)
    {
        // search through each collection and search for the source
        List<Statistics> results = new ArrayList<Statistics>();
        for (Map<String, Statistics> statsMapping : globalStats)
        {
            if (statsMapping.containsKey(source))
                results.add(statsMapping.get(source));
        }

        return results;
    }

    public List<Statistics> getGlobalStatistics()
    {
        // returns the earliest found date, duration of entire collection (add all in Map), total number of samples (by adding)
        // and possibly which sources were collected

        List<Statistics> allStats = new ArrayList<Statistics>(globalStats.size());
        for (Map<String, Statistics> singleCollection : globalStats)
        {
            Date earlyDate = new Date();
            long sumDuration = 0, sumSamples = 0;

            for (Statistics s : singleCollection.values())
            {
                // save only the earliest date
                if (s.getDate().before(earlyDate))
                    earlyDate = s.getDate();

                sumDuration += s.getElapsedTime();
                sumSamples += s.getSamples();
            }

            allStats.add(new Statistics(earlyDate, sumDuration, sumSamples));
        }


        return allStats;
    }

    public void removeStatisticsForSource(String source)
    {
        // search everything; if found, remove object; write new results
        for (Map<String, Statistics> statsMapping : globalStats)
            statsMapping.remove(source);

        writeToDataStore();
    }

    public void writeToDataStore()
    {
        try
        {
            SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
            connection.runWriteAction(STATS_DATASTORE_NAME, new WriteAction()
            {
                @Override
                public void execute(@NotNull WritableSystemAccess access) throws IOException
                {
                    DataStore store = access.getSystemDataStore(STATS_DATASTORE_NAME);
                    PrintWriter writer = store.getWriter();

                    // for each key, write the line for the stats map
                    for (Map<String, Statistics> collectionStats : globalStats)
                    {
                        writer.print(collectionStats.size() + ";");
                        for (String key : collectionStats.keySet())
                            writer.println(key + ";" + collectionStats.get(key).toString());
                    }

                    writer.flush();
                    writer.close();
                }
            });
        }
        catch (SystemException e)
        {
            Logger.println("Error in System when writing Statistics DataStore", e);
        }
        catch (WriteAbortedException e)
        {
            Logger.println("Write Aborted when writing Statistics DataStore", e);
        }
        catch (ActionExecutionException e)
        {
            Logger.println("Action Execution exception when writing Statistics DataStore", e);
        }
    }

    public void loadFromDataSore()
    {
        try
        {
            SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
            connection.runReadAction(new ReadAction()
            {
                @Override
                public void execute(@NotNull SystemAccess access) throws Exception
                {
                    DataStore store = access.getSystemDataStore(STATS_DATASTORE_NAME);
                    try
                    {

                        BufferedReader reader = store.getReader();
                        String line = reader.readLine();

                        while (line != null)
                        {
                            String[] flatStats = line.split(";");
                            Map<String, Statistics> statsMap = parseFlatStatistics(flatStats);
                            globalStats.add(statsMap);

                            line = reader.readLine();
                        }
                    }
                    catch (IOException e)
                    {
                        Logger.println("Error reading stats file", e);
                        throw new IOException(e);
                    }
                }
            });
        }
        catch (ActionExecutionException e)
        {
            Logger.println("Error Loading stats file!", e);
        }
        catch (SystemException e)
        {
            Logger.println("Error Loading stats file!", e);
        }
    }

    private Map<String, Statistics> parseFlatStatistics(String[] strings) throws Exception
    {
        Map<String, Statistics> singleCollectionMap = new HashMap<String, Statistics>();
        int sizeOfEachCollection = Integer.parseInt(strings[0]);

        for (int i = 1; i < sizeOfEachCollection; i += 4)
        {
            String source   = strings[i];
            long date       = Long.parseLong(strings[i+1]);
            long duration   = Long.parseLong(strings[i+2]);
            long samples    = Long.parseLong(strings[i+3]);

            singleCollectionMap.put(source, new Statistics(new Date(date), duration, samples));
        }

        return singleCollectionMap;
    }
}
