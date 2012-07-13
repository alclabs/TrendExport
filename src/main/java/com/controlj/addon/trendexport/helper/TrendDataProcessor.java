package com.controlj.addon.trendexport.helper;

import com.controlj.addon.trendexport.tables.TrendDataTable;
import com.controlj.addon.trendexport.util.Statistics;
import com.controlj.green.addonsupport.access.trend.TrendProcessor;
import com.controlj.green.addonsupport.access.trend.TrendSample;
import com.controlj.green.addonsupport.xdatabase.Database;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
import com.controlj.green.addonsupport.xdatabase.Insert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

// Notes:
// 1. Planned Improvement - add inserts to list and execute(list.toArray());


public class TrendDataProcessor implements TrendProcessor
{
    private final Database db;
    private final TrendDataTable table;
    private int samplesToSkip;
    private Insert holeStartInsert;
    private Insert holeEndInsert;
    private long samplesWritten;
    private Statistics statistics, globalStatistics;

    public TrendDataProcessor(Database database, TrendDataTable trendDataTable, int numberOfSamplesToSkip, Statistics statistics, Statistics globalStats)
    {
        db = database;
        table = trendDataTable;
        samplesToSkip = numberOfSamplesToSkip;
        samplesWritten = 0;
        this.statistics = statistics;
        globalStatistics = globalStats;
    }

    @Override
    public void processStart(@NotNull Date date, @Nullable TrendSample sample)
    {
    }

    @Override
    public void processData(@NotNull TrendSample sample)
    {
        // insert data based on type
        if (samplesToSkip > 0)
        {
            samplesToSkip--;
            return;
        }

        try
        {
            if (holeStartInsert != null)
            {
                db.execute(holeStartInsert);
                holeStartInsert = null;
            }

            if (holeEndInsert != null)
            {
                db.execute(holeEndInsert);
                holeEndInsert = null;
            }

            Insert insert = table.buildInsertForData(sample); samplesWritten++;
            db.execute(insert);
        }
        catch (DatabaseException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void processEnd(@NotNull Date date, @Nullable TrendSample sample)
    {
        statistics.addSamples(samplesWritten);
        globalStatistics.addSamples(samplesWritten);
    }

    @Override
    public void processHole(@NotNull Date startDate, @NotNull Date endDate)
    {
        holeStartInsert = table.buildInsertForHole(startDate, true);
        holeEndInsert = table.buildInsertForHole(endDate, false);
    }
}
