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

    public static void collectData(DBAndSchemaSynchronizer synchronizer)
    {
        try
        {
            synchronizer.connect();
            Collection<TrendPathAndDBTableName> sources = synchronizer.getSourceMappings().getSourcesAndTableNames();

            for (TrendPathAndDBTableName source : sources)
            {
                //collectDataForSource(source.getTrendSourceReferencePath(), synchronizer);
            }
        }
        catch (Exception e)
        {
            ErrorHandler.handleError("Data Collection Failed!", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
        }
        finally
        {
            synchronizer.disconnect();
        }
    }

    public static void collectDataForSource(String source, DBAndSchemaSynchronizer synchronizer) throws SystemException, ActionExecutionException
    {
        lock.lock();
        copyLatestTrendHistory(source, synchronizer);
        lock.unlock();
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
//                    ErrorHandler.handleError("Entering COPY", new Throwable());
                    Location startLoc = systemAccess.resolveGQLPath(nodeLookupString);
                    TrendSource trendSource = startLoc.getAspect(TrendSource.class);


                    // start using the retriever
                    DataStoreRetriever retriever = synchronizer.getRetrieverForTrendSource(nodeLookupString);
                    Date startDate = retriever.getLastRecordedDate();
                    int numberOfSamplesToSkip = retriever.getNumberOfSamplesToSkip();
                    TrendData trendData = trendSource.getTrendData(TrendRangeFactory.byDateRange(startDate, new Date()));

//                    ErrorHandler.handleError("Entering INSERT", new Throwable());

                    synchronizer.insertTrendSamples(nodeLookupString, trendData, numberOfSamplesToSkip);

//                    ErrorHandler.handleError("Leaving INSERT", new Throwable());

                }
                catch (Exception e)
                {
                    ErrorHandler.handleError("Collector Failure", e);
                }
            }
        });
    }
}
