package com.controlj.addon.trendexport.statistics;

// class is here to maintain a list of stats

import com.controlj.addon.trendexport.util.Logger;
import com.controlj.green.addonsupport.access.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class StatisticsLibrarian
{
    private static final String STATS_DATASTORE_NAME = "TrendExportStats";

    public StatisticsLibrarian()
    {
    }

    public void storeCollectionStatistics(String source, Statistics statistics)
    {
        writeSourceToDataStore(source, statistics);
    }

    private boolean checkSourceExists(String source)
    {
        // look to see if datastore exists
        return false;
    }

    public List<Statistics> getStatisticsForSource(String source)
    {
        checkSourceExists(source);
        return loadFromDataStore(source).getStatisticsList();
    }

    public void removeStatisticsForSource(String source)
    {
        // delete datastore named after source
//        writeToDataStore();
    }

    private void writeSourceToDataStore(String source, final Statistics statistics)
    {
        final String datastore = source.replace(":", "_");
        try
        {
            SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
            connection.runWriteAction(source, new WriteAction()
            {
                @Override
                public void execute(@NotNull WritableSystemAccess access) throws IOException
                {
                    DataStore store = access.getSystemDataStore(datastore);
                    SourceStatsHolder holder = StatisticsLibrarian.this.readLineForStats(store.getReader());

                    // if null, the datastore was just created or nothing is in it...no need to read it first
                    if (holder == null)
                        holder = new SourceStatsHolder();

                    holder.addStatistics(statistics);

                    // serialize here
                    String toWrite = new StatisticsSerializer().serialize(holder.getStatisticsList());
                    PrintWriter writer = store.getWriter();
                    writer.println(toWrite);

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

    public void writeToDataStore()
    {
        /*try
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
                    for (String key : statsMap.keySet())
                        writer.println(key + ";" +
                                statsMap.get(key).getStatisticsList().size() + ";" +
                                statsMap.get(key).toString());

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
        }*/
    }

    public SourceStatsHolder loadFromDataStore(String source)
    {
       /* try
        {
            final String sourceName = source.replace(":", "_");
            SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
            return connection.runReadAction(new ReadActionResult<SourceStatsHolder>()
            {
                @Override
                public SourceStatsHolder execute(@NotNull SystemAccess access) throws Exception
                {
                    DataStore store = access.getSystemDataStore(sourceName);
                    return readLineForStats(store.getReader());
                }
            });
        }
        catch (SystemException e)
        {
            e.printStackTrace();
        }
        catch (ActionExecutionException e)
        {
            e.printStackTrace();
        }   */

        return new SourceStatsHolder();
    }

    private SourceStatsHolder readLineForStats(BufferedReader reader) throws IOException
    {
        String line = reader.readLine();
        if (line == null)
            return null;

        return new StatisticsSerializer().deserialize(line);
    }

    /*public void loadFromDataSore()
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
                            SourceStatsHolder holder = parseFlatStatistics(flatStats);
                            statsMap.put(flatStats[0], holder);

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
    }*/

    /*private SourceStatsHolder parseFlatStatistics(String[] strings)
    {
        SourceStatsHolder holder = new SourceStatsHolder();
        int lengthForEach = (int) Long.parseLong(strings[1]);

        for (int i = 2; i < lengthForEach; i++)
        {
            Date date = new Date(Long.parseLong(strings[i]));
            long duration = Long.parseLong(strings[1 + i + lengthForEach]);
            long samples = Long.parseLong(strings[2 + i + lengthForEach]);

            holder.addStatistics(new Statistics(date, duration, samples));
        }

        return holder;
    }*/
}
