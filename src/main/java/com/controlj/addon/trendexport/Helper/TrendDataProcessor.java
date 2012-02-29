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
    private Insert lastHoleInsert;

    public TrendDataProcessor(Database database, TrendDataTable trendDataTable, int numberOfSamplesToSkip)
    {
        db = database;
        table = trendDataTable;
        samplesToSkip = numberOfSamplesToSkip;
    }

    @Override
    public void processStart(@NotNull Date date, @Nullable TrendSample sample)
    {

        // insert data based on type
        /*Insert insert = null;
        if (sample == null)
        {
            /*insert = db.buildInsert(table.dateColumn.set(date),
                    table.typeColumn.set(0),
                    table.valueColumn.set(null));
        }
        else if (sample instanceof TrendDigitalSample)
        {
            boolean value = ((TrendDigitalSample) sample).getState();
            insert = db.buildInsert(table.dateColumn.set(sample.getTime()),
                    table.typeColumn.set(0),
                    table.valueColumn.set(value));

        }
        else if (sample instanceof TrendAnalogSample)
        {
            float value = sample.getSpecialValue();
            insert = db.buildInsert(table.dateColumn.set(sample.getTime()),
                    table.typeColumn.set(0),
                    table.valueColumn.set(value));
        }
        else if (sample instanceof TrendEquipmentColorSample)
        {
            short value = (short) ((TrendEquipmentColorSample) sample).value().getValue();
            // insert (needs to be short)
            insert = db.buildInsert(table.dateColumn.set(sample.getTime()),
                    table.typeColumn.set(0),
                    table.valueColumn.set(value));
        }

        try
        {
            if (insert != null)
                db.execute(insert);
        }
        catch (DatabaseException e)
        {
            e.printStackTrace();
        }    */
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
            if (lastHoleInsert != null)
            {
                db.execute(lastHoleInsert);
                lastHoleInsert = null;
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
        // insert data based on type
        /*Insert insert = null;
        if (sample == null)
        {
            insert = db.buildInsert(table.dateColumn.set(date),
                    table.typeColumn.set(3),
                    table.valueColumn.set(null));
        }
        else if (sample instanceof TrendDigitalSample)
        {
            boolean value = ((TrendDigitalSample) sample).getState();
            insert = db.buildInsert(table.dateColumn.set(sample.getTime()),
                    table.typeColumn.set(3),
                    table.valueColumn.set(value));
        }
        else if (sample instanceof TrendAnalogSample)
        {
            float value = sample.getSpecialValue();
            insert = db.buildInsert(table.dateColumn.set(sample.getTime()),
                    table.typeColumn.set(3),
                    table.valueColumn.set(value));
        }
        else if (sample instanceof TrendEquipmentColorSample)
        {
            short value = (short) ((TrendEquipmentColorSample) sample).value().getValue();
            // insert (needs to be short)
            insert = db.buildInsert(table.dateColumn.set(sample.getTime()),
                    table.typeColumn.set(3),
                    table.valueColumn.set(value));
        }

        try
        {
            db.execute(insert);
        }
        catch (DatabaseException e)
        {
            e.printStackTrace();
        } */
    }

    @Override
    public void processHole(@NotNull Date startDate, @NotNull Date endDate)
    {
        lastHoleInsert = table.buildInsertForHole(new Date(endDate.getTime() - startDate.getTime()));
    }
}
