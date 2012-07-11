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
    private static Map<String, Statistics> statsMap;
    private static final StatisticsCollector STATISTICS_COLLECTOR = new StatisticsCollector();
    private static final Lock lock = new ReentrantLock(true);
    private static final String STATS_DATASTORE_NAME = "TrendExportStats";

    public static StatisticsCollector getStatisticsCollector()
    {
        return STATISTICS_COLLECTOR;
    }

    private StatisticsCollector()
    {
        statsMap = new HashMap<String, Statistics>();
        statsMap.put("global", new Statistics());
    }

    public void writeStats(String source, Statistics statistics)
    {
        lock.lock();

        statsMap.put(source, statistics);
        writeToDataStore();

        lock.unlock();
    }

    public Statistics getStatisticsForSource(String source)
    {
        if (!statsMap.containsKey(source))
            statsMap.put(source, new Statistics());

        return statsMap.get(source);
    }

    public void removeStatisticsForSource(String source)
    {
        statsMap.remove(source);
        // search datastore and remove individual stats or just rewrite whole thing?
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

//                    calculateGlobalStatistics();

                    // for each key, write the line for the stats map
                    for (String key : statsMap.keySet())
                        writer.println(key + ";" + statsMap.get(key).toString());

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

    private Statistics calculateGlobalStatistics()
    {
        Statistics globalStats = statsMap.get("global");
        for (String key : statsMap.keySet())
        {
            if (key.equals("global"))
                continue;

            Statistics stats = statsMap.get(key);

        }

        return null;
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
                            Statistics stats = parseFlatStatistics(flatStats);
                            statsMap.put(flatStats[0], stats);

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

    private Statistics parseFlatStatistics(String[] strings) throws Exception
    {
        int start = 2;
        int sizeOfEachCollection = Integer.parseInt(strings[1]);

        // read in size of date collection
        List<Date> dates = convertLongsToDates(getListFromFlatStats(strings, sizeOfEachCollection, start));

        // read in size of elapsed times collection
        start = 2 + sizeOfEachCollection;
        List<Long> elapsedTimes = getListFromFlatStats(strings, sizeOfEachCollection, start);

        // read in size of samples collection
        start = 2 + (sizeOfEachCollection * 2);
        List<Long> samplesCollection = getListFromFlatStats(strings, sizeOfEachCollection, start);

        return new Statistics(dates, elapsedTimes, samplesCollection);
    }

    private List<Long> getListFromFlatStats(String[] data, int sizeOfEachCollection, int start) throws Exception
    {
        int i = 0; // used for debugging mainly :/
        try
        {
            List<Long> tempList = new ArrayList<Long>(sizeOfEachCollection);
            for (i = start; i < start + sizeOfEachCollection; i++)
            {
                Long l = Long.parseLong(data[i]);
                tempList.add(l);
            }

            return tempList;
        }
        catch (NumberFormatException e)
        {
            Logger.println("Error parsing statistics datastore at index: " + i, e);
            throw new Exception("Error parsing statistics datastore at " + i, e);
        }
    }

    private List<Date> convertLongsToDates(List<Long> longs)
    {
        List<Date> dates = new ArrayList<Date>(longs.size());
        for (Long l : longs)
            dates.add(new Date(l));

        return dates;
    }

    public long getTotalSamples()
    {
        long sum = 0;
        for (String key : statsMap.keySet())
        {
            if (!key.equals("global"))
                sum += statsMap.get(key).getTotalSamples();
        }

        return sum;
    }

    public long getMostRecentTotal()
    {
        long sum = 0;
        for (String key : statsMap.keySet())
        {
            if (key.equals("global"))
                continue;


            sum += statsMap.get(key).getLatestSampleCollection();
        }

        return sum;
    }
}
