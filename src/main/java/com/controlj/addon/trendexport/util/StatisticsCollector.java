package com.controlj.addon.trendexport.util;

// class is here to maintain a list of stats

import com.controlj.green.addonsupport.access.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsCollector
{
    private static final StatisticsCollector STATISTICS_COLLECTOR = new StatisticsCollector();
    private static final String STATS_DATASTORE_NAME = "TrendExportStats";

    // global map of every collection containing the source that was collected as well as the statistics
    private final Map<String, StatisticsHolder> globalStats;

    public static StatisticsCollector getStatisticsCollector()
    {
        return STATISTICS_COLLECTOR;
    }

    private StatisticsCollector()
    {
        globalStats = new HashMap<String, StatisticsHolder>();
        globalStats.put("global", new StatisticsHolder());
    }

    public void storeCollectionStatistics(String source, Statistics statistics)
    {
        sourceExists(source);

        StatisticsHolder holder = globalStats.get(source);
        holder.addStatistics(statistics);
        globalStats.put(source, holder);
    }

    private void sourceExists(String source)
    {
        if (!globalStats.containsKey(source))
            globalStats.put(source, new StatisticsHolder());
    }

    public List<Statistics> getStatisticsForSource(String source)
    {
        sourceExists(source);
        return globalStats.get(source).getStatisticsList();
    }

    public void removeStatisticsForSource(String source)
    {
        globalStats.remove(source);
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
                    for (String key : globalStats.keySet())
                        writer.println(key + ";" +
                                globalStats.get(key).getStatisticsList().size() + ";" +
                                globalStats.get(key).toString());

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
                            StatisticsHolder holder = parseFlatStatistics(flatStats);
                            globalStats.put(flatStats[0], holder);

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

    private StatisticsHolder parseFlatStatistics(String[] strings) throws Exception
    {
        StatisticsHolder holder = new StatisticsHolder();
        int lengthForEach = (int) Long.parseLong(strings[1]);

        for (int i = 2; i < lengthForEach; i++)
        {
            Date date = new Date(Long.parseLong(strings[i]));
            long duration = Long.parseLong(strings[1 + i + lengthForEach]);
            long samples = Long.parseLong(strings[2 + i + lengthForEach]);

            holder.addStatistics(new Statistics(date, duration, samples));
        }

        return holder;
    }
}
