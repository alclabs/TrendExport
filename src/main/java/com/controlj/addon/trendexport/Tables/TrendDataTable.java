/*
* Copyright (c) 2011 Automated Logic Corporation
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/

package com.controlj.addon.trendexport.tables;

import com.controlj.green.addonsupport.access.trend.TrendAnalogSample;
import com.controlj.green.addonsupport.access.trend.TrendDigitalSample;
import com.controlj.green.addonsupport.access.trend.TrendSample;
import com.controlj.green.addonsupport.xdatabase.AllowNull;
import com.controlj.green.addonsupport.xdatabase.DatabaseSchema;
import com.controlj.green.addonsupport.xdatabase.Insert;
import com.controlj.green.addonsupport.xdatabase.TableSchema;
import com.controlj.green.addonsupport.xdatabase.column.DateColumn;
import com.controlj.green.addonsupport.xdatabase.column.IntColumn;
import com.controlj.green.addonsupport.xdatabase.dsl.Column;

import java.util.Date;

public class TrendDataTable
{
    private final DatabaseSchema db;
    private final String tableName;
    private TableSchema tableSchema;
    public final IntColumn id;
    public final DateColumn dateColumn;
    public final Column typeColumn;
    public final Column valueColumn;

    public TrendDataTable(DatabaseSchema db, String tableName, short type)
    {
        this.db = db;
        this.tableSchema = db.addTable(tableName);
        this.tableName = tableName;

        id = tableSchema.addIntColumn("ID");
        tableSchema.setAutoGenerate(id);
        tableSchema.setPrimaryKey(id);

        dateColumn = tableSchema.addDateColumn("DateStamp");
        typeColumn = tableSchema.addShortColumn("TrendType");

        switch (type)
        {
            case 1:
                valueColumn = tableSchema.addFloatColumn("TrendData", AllowNull.TRUE);
                break;
            case 2:
                valueColumn = tableSchema.addBoolColumn("TrendData", AllowNull.TRUE);
                break;
            case 3: // Eq Color (no longer supported :(  )
                valueColumn = tableSchema.addShortColumn("TrendData", AllowNull.TRUE);
                break;
            default:
                throw new RuntimeException("Unhandled data type of " + type);
        }

        // does not work...unknown reason :(
        tableSchema.addIndex("Date_IDX_" + tableName, dateColumn);
    }

    public TableSchema getTableSchema()
    {
        return tableSchema;
    }

    public String getTableName()
    {
        return tableName;
    }

    public Insert buildInsertForData(TrendSample sample)
    {
        /*if (sample instanceof TrendEquipmentColorSample)
        {
            short value = (short) ((TrendEquipmentColorSample) sample).value().getValue();
            return db.buildInsert(
                    dateColumn.set(sample.getTime()),
                    typeColumn.set(1),
                    valueColumn.set(value));
        }
        else*/
        if (sample instanceof TrendDigitalSample)
        {
            boolean value = ((TrendDigitalSample) sample).getState();
            return db.buildInsert(
                    dateColumn.set(sample.getTime()),
                    typeColumn.set(1),
                    valueColumn.set(value));
        }
        else //(sample instanceof TrendAnalogSample)
        {
            float value = ((TrendAnalogSample) sample).floatValue();
            return db.buildInsert(
                    dateColumn.set(sample.getTime()),
                    typeColumn.set(1),
                    valueColumn.set(value));
        }
    }

    public Insert buildInsertForHole(Date date, boolean isStart)
    {
        int value = 3; // values are: 1 for data, 2 for hole start, and 3 for hole end
        if (isStart)
            value = 2;

        return db.buildInsert(
                typeColumn.set(value),
                dateColumn.set(date),
                valueColumn.set(null));
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrendDataTable that = (TrendDataTable) o;
        return that.tableSchema.getName().equals(tableSchema.getName());
    }

    @Override
    public int hashCode()
    {
        return tableSchema.getName().hashCode();
    }
}
