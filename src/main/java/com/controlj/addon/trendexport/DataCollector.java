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
import com.controlj.addon.trendexport.helper.TrendPathAndDBTableName;
import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import com.controlj.green.addonsupport.access.trend.TrendData;
import com.controlj.green.addonsupport.access.trend.TrendRangeFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataCollector
{
    private static final Lock lock = new ReentrantLock(true);
    private static String tableName = "";
    private static boolean isBusy;

    public static void collectData(DBAndSchemaSynchronizer synchronizer)
    {
        try
        {
            isBusy = true;

            synchronizer.connect();
            Collection<TrendPathAndDBTableName> sources = synchronizer.getSourceMappings().getSourcesAndTableNames();

            for (TrendPathAndDBTableName source : sources)
            {
                collectDataForSource(source.getTrendSourceReferencePath(), synchronizer);
            }
        }
        catch (Exception e)
        {
            ErrorHandler.handleError("Data Collection Failed!", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
        }
        finally
        {
            synchronizer.disconnect();
            isBusy = false;
        }
    }

    public static void collectDataForSource(String source, DBAndSchemaSynchronizer synchronizer) throws SystemException, ActionExecutionException, SourceMappingNotFoundException
    {

        lock.lock();

        tableName = synchronizer.getSourceMappings().getTableNameFromSource(source);
        isBusy = true;
        copyLatestTrendHistory(source, synchronizer);
        lock.unlock();

        isBusy = false;
    }

    public static String getTableName()
    {
        return tableName;
    }

    public static boolean isCollectorBusy()
    {
        return isBusy;
    }

    private static void copyLatestTrendHistory(final String nodeLookupString, final DBAndSchemaSynchronizer synchronizer)
            throws SystemException, ActionExecutionException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
        {
            @Override
            public void execute(@NotNull SystemAccess systemAccess) throws Exception
            {
                try
                {
//                    Location startLoc = systemAccess.getTree(SystemTree.Geographic).resolve(nodeLookupString);
                    Location startLoc = systemAccess.resolveGQLPath(nodeLookupString);
                    TrendSource trendSource = startLoc.getAspect(TrendSource.class);


                    // start using the retriever
                    DataStoreRetriever retriever = synchronizer.getRetrieverForTrendSource(nodeLookupString);
                    Date startDate = retriever.getLastRecordedDate();
                    int numberOfSamplesToSkip = retriever.getNumberOfSamplesToSkip();
                    TrendData trendData = trendSource.getTrendData(TrendRangeFactory.byDateRange(startDate, new Date()));

                    synchronizer.insertTrendSamples(nodeLookupString, trendData, numberOfSamplesToSkip);
                }
                catch (Exception e) //todo - not needed?
                {
                    ErrorHandler.handleError("Collector Failure", e);
                }
            }
        });
    }
}
