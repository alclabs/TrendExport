package com.controlj.addon.trendexport.helper;

import com.controlj.addon.trendexport.tables.TrendDataTable;
import com.controlj.green.addonsupport.access.trend.*;
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


    public TrendDataProcessor(Database database, TrendDataTable trendDataTable, int numberOfSamplesToSkip)
    {
        db = database;
        table = trendDataTable;
        samplesToSkip = numberOfSamplesToSkip;
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

            Insert insert = table.buildInsertForData(sample);
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
    }

    @Override
    public void processHole(@NotNull Date startDate, @NotNull Date endDate)
    {
        holeStartInsert = table.buildInsertForHole(startDate, true);
        holeEndInsert = table.buildInsertForHole(endDate, false);
    }
}
