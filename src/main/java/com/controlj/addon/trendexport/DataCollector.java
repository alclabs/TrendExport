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

import com.controlj.addon.trendexport.exceptions.SourceMappingNotFoundException;
import com.controlj.addon.trendexport.exceptions.TableNotInDatabaseException;
import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import com.controlj.green.addonsupport.access.trend.TrendData;
import com.controlj.green.addonsupport.access.trend.TrendRangeFactory;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataCollector
{
    private static final Lock lock = new ReentrantLock(true);
    private static final AtomicReference<String> tableName = new AtomicReference<String>("");
    private static final AtomicBoolean isBusy = new AtomicBoolean();

    public static void collectData(DBAndSchemaSynchronizer synchronizer, Collection<String> referencePaths)
    {
        try
        {
            isBusy.set(true);

            synchronizer.connect();

            for (String source : referencePaths)
            {
                try
                {
                    collectDataForSource(source, synchronizer);
                }
                catch (SourceMappingNotFoundException e)
                {
                    ErrorHandler.handleError("Source mapping missing", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
                }
                catch (SystemException e)
                {
                    ErrorHandler.handleError("Unexpected system exception", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
                }
                catch (TrendException e)
                {
                    ErrorHandler.handleError("Trend Source exception", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
                }
                catch (TableNotInDatabaseException e)
                {
                    ErrorHandler.handleError("Table not found in collection database", e, AlarmHandler.TrendExportAlarm.DatabaseWriteFailure);
                }
                catch (DatabaseException e)
                {
                    ErrorHandler.handleError("Database error exception", e, AlarmHandler.TrendExportAlarm.DatabaseWriteFailure);
                }
            }
        }
        catch (DatabaseException e)
        {
            ErrorHandler.handleError("Cannot connect to collection database", e, AlarmHandler.TrendExportAlarm.DatabaseWriteFailure);
        }
        finally
        {
            synchronizer.disconnect();
            isBusy.set(false);
        }
    }

    public static void collectDataForSource(String source, DBAndSchemaSynchronizer synchronizer)
            throws SourceMappingNotFoundException, TableNotInDatabaseException, TrendException, DatabaseException, SystemException
    {
        lock.lock(); isBusy.set(true);
        tableName.set(synchronizer.getSourceMappings().getTableNameFromSource(source));

        copyLatestTrendHistory(source, synchronizer);

        lock.unlock(); isBusy.set(false);
    }

    public static String getTableName()
    {
        return tableName.get();
    }

    public static boolean isCollectorBusy()
    {
        return isBusy.get();
    }

    private static void copyLatestTrendHistory(final String nodeLookupString, final DBAndSchemaSynchronizer synchronizer)
            throws DatabaseException, TrendException, TableNotInDatabaseException, SourceMappingNotFoundException, SystemException
    {
        try
        {
            SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
            connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
            {
                @Override
                public void execute(@NotNull SystemAccess systemAccess)
                        throws DatabaseException, NoSuchAspectException, TrendException, TableNotInDatabaseException, UnresolvableException, SourceMappingNotFoundException
                {
                    Location startLoc = systemAccess.resolveGQLPath(nodeLookupString);
                    TrendSource trendSource = startLoc.getAspect(TrendSource.class);
                    if (! trendSource.isHistorianEnabled())
                        ErrorHandler.handleError("Trend Historian Disabled", new Exception(), AlarmHandler.TrendExportAlarm.HistorianDisabled);

                    // start using the retriever
                    DataStoreRetriever retriever = synchronizer.getRetrieverForTrendSource(nodeLookupString);
                    Date startDate = retriever.getLastRecordedDate();
                    int numberOfSamplesToSkip = retriever.getNumberOfSamplesToSkip();
                    TrendData trendData = trendSource.getTrendData(TrendRangeFactory.byDateRange(startDate, new Date()));

                    synchronizer.insertTrendSamples(nodeLookupString, trendData, numberOfSamplesToSkip);
                }
            });
        }
        catch (ActionExecutionException e)
        {
            isBusy.set(false);
            tableName.set("Error Collecting Data!");

            if (e.getCause() instanceof DatabaseException)
                throw new DatabaseException("Database Error", e);
            else if (e.getCause() instanceof NoSuchAspectException)
                throw new DatabaseException("Database Error", e);
            else if (e.getCause() instanceof TrendException)
                throw new TrendException("WebCTRL Database Error", e);
            else if (e.getCause() instanceof TableNotInDatabaseException)
                throw new TableNotInDatabaseException("Database Error", e);
            else if (e.getCause() instanceof SourceMappingNotFoundException)
                throw new SourceMappingNotFoundException("Source Mapping not found");
        }
    }
}
