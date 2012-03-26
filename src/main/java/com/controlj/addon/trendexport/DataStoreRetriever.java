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

package com.controlj.addon.trendexport;

import com.controlj.addon.trendexport.exceptions.TableNotInDatabaseException;
import com.controlj.addon.trendexport.tables.TrendDataTable;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
import com.controlj.green.addonsupport.xdatabase.DatabaseReadAccess;
import com.controlj.green.addonsupport.xdatabase.QueryTask;
import com.controlj.green.addonsupport.xdatabase.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DataStoreRetriever
{
    private Date lastRecordedDate;
    private String tableName;
    private DynamicDatabase database;
    private TrendDataTable trendDataTable;

    public DataStoreRetriever(String nameFromSource, DynamicDatabase database) throws TableNotInDatabaseException
    {
        this.tableName = nameFromSource;
        this.database = database;
        this.trendDataTable = database.getDataTableByTableName(tableName);
    }

    public Date getLastRecordedDate() throws DatabaseException
    {
        lastRecordedDate = getLastRecordedDateFromDB();
        return lastRecordedDate;
    }

    private Date getLastRecordedDateFromDB() throws DatabaseException
    {
//        ErrorHandler.handleError("Entering GET DATE", new Throwable());
        return database.runQuery(new QueryTask<Date>()
        {
            @Override
            public Date execute(@NotNull DatabaseReadAccess databaseReadAccess) throws DatabaseException
            {
                TrendDataTable table = trendDataTable;
                Result result = databaseReadAccess.execute(database.buildSelect(table.dateColumn).orderBy(table.dateColumn.desc()));

//                ErrorHandler.handleError("Leaving GET DATE", new Throwable());

                if (result.next())
                    return result.get(table.dateColumn);
                else
                    return new Date(0); // no date found, start at the beginning
            }
        });
    }


    public String getTableName()
    {
        return tableName;
    }

    public Integer getNumberOfSamplesToSkip() throws DatabaseException
    {
        return getSamplesToSkip();
    }

    private Integer getSamplesToSkip() throws DatabaseException
    {
        return database.runQuery(new QueryTask<Integer>()
        {
            @Override
            public Integer execute(@NotNull DatabaseReadAccess databaseReadAccess) throws DatabaseException
            {
                Result result;
                TrendDataTable table = trendDataTable;//database.getDataTableByTableName(tableName);
                //if (lastRecordedDate == null)
                result = databaseReadAccess.execute(database.buildSelect(table.dateColumn).where(table.dateColumn.eq(lastRecordedDate)));
                //else
                //result = databaseReadAccess.execute(database.buildSelect(table.dateColumn).where(table.dateColumn.notBefore(lastRecordedDate))); // needs to include the

                int skipCount = 1; // start with 1 bc we need to skip the date itself
                if (lastRecordedDate.equals(new Date(0))) // no need for the epoch time
                    skipCount = 2;

                while (result.next())
                    skipCount++;

                return skipCount;
            }
        });
    }

}


