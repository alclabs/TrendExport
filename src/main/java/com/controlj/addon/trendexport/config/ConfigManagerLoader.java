package com.controlj.addon.trendexport.config;

import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import com.controlj.green.addonsupport.xdatabase.XDatabase;

public class ConfigManagerLoader
{
    public ConfigManager loadConnectionInfoFromDataStore()
    {
//        ConfigManager manager = new ConfigManager("localhost", 3306, "root", "0000", DatabaseType.MySQL, "trendexport_schema");
        ConfigManager manager = new ConfigManager("", 0, "", "", DatabaseType.Derby, "");

        try
        {
            manager.load();
        }
        catch (Exception e)
        {
            ErrorHandler.handleError("Error reading SourceMappings data", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
        }

        return manager;
    }

    public static boolean isConfigured()
    {
        try {
            return XDatabase.getXDatabase().canReadDatabaseConnectionInfo("connection");
        }
        catch (NullPointerException n)
        {
            return false;
        }
    }
}
