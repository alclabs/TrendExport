package com.controlj.addon.trendexport.Config;

import com.controlj.addon.trendexport.util.Logger;
import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import com.controlj.green.addonsupport.xdatabase.XDatabase;

import java.io.IOException;

public class ConfigManagerLoader
{
    public ConfigManager loadConnectionInfoFromDataStore()
    {
        ConfigManager manager = new ConfigManager("localhost", 3306, "root", "0000", DatabaseType.MySQL, "trendexport_schema");

        try
        {
            manager.load();
        }
        catch (IOException e)
        {
            Logger.println("Error reading SourceMappings data", e);
        }

        return manager;
    }

    public static boolean isConfigured()
    {
        return XDatabase.getXDatabase().canReadDatabaseConnectionInfo("connection");
    }
}
