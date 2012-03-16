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

package com.controlj.addon.trendexport.util;

import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.DataCollector;
import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledTrendCollector implements ServletContextListener
{
    private static final AtomicReference<ScheduledTrendCollector> collectorRef = new AtomicReference<ScheduledTrendCollector>();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    @Nullable private ScheduledFuture<?> collectionHandler;
    private static Date nextCollectionDate;

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        collectorRef.set(this);

        ConfigManager configManager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
        initializeAndScheduleCollector(configManager);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        if (collectionHandler != null)
            collectionHandler.cancel(false);
        executor.shutdown();
        collectorRef.set(null);
    }

    // take in configuration so that it can inject to the initialization
    public static void restartCollector(ConfigManager manager)
    {
        ScheduledTrendCollector theCollector = collectorRef.get();
        if (theCollector != null)
        {
            if (theCollector.collectionHandler != null)
                theCollector.collectionHandler.cancel(false);
            theCollector.initializeAndScheduleCollector(manager);
        }
    }

    private void initializeAndScheduleCollector(final ConfigManager manager)
    {
        if (collectionHandler != null && !collectionHandler.isCancelled())
            collectionHandler.cancel(false);

        final DBAndSchemaSynchronizer synchronizer;
        try
        {
            synchronizer = DBAndSchemaSynchronizer.getSynchronizer(manager.getCurrentConnectionInfo());
        }
        catch (IOException e)
        {
            ErrorHandler.handleError("Error initializing Database Synchronizer", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
            return;
        }

        final ScheduledConfigurationLoader loader = new ScheduledConfigurationLoader(manager);
        long initialDelay = loader.calculateDelay();
        final long interval = loader.calculateInterval();

        nextCollectionDate = loader.getScheduledCalendarDate().getTime();

        collectionHandler = executor.scheduleAtFixedRate(new Runnable() {
                @Override public void run()
                {
                    DataCollector.collectData(synchronizer);
                    nextCollectionDate = loader.getScheduledCalendarDate().getTime();
                }
            }, initialDelay, interval, TimeUnit.MILLISECONDS);
    }

    public static Date getNextCollectionDate()
    {
        return nextCollectionDate;
    }

    public static String getTableName()
    {
        return DataCollector.getTableName();
    }
}

