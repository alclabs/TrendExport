package com.controlj.addon.trendexport.config;

import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import com.controlj.green.addonsupport.xdatabase.XDatabase;

import java.io.IOException;

public class ConfigManagerLoader
{
    public ConfigManager loadConnectionInfoFromDataStore()
    {
        ConfigManager manager = new ConfigManager("localhost", 0, "", "", DatabaseType.Derby, "");

        try
        {
            manager.load();
        }
        catch (IOException e)
        {
            ErrorHandler.handleError("Error reading SourceMappings data", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
        }

        return manager;
    }

    public static boolean isConfigured()
    {
        return XDatabase.getXDatabase().canReadDatabaseConnectionInfo("connection");
    }
}
