package com.controlj.addon.trendexport.config;

import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import com.controlj.green.addonsupport.xdatabase.XDatabase;

import java.io.IOException;

public class ConfigManagerLoader
{
    public ConfigManager loadConnectionInfoFromDataStore() throws Exception
    {
        ConfigManager manager = new ConfigManager("", 0, "", "", DatabaseType.Derby, "");

        try
        {
            manager.load();
        }
        catch (IOException e)
        {
            ErrorHandler.handleError("Error loading config file: Default configuration being used", e);
        }
        finally
        {
            return manager;
        }
    }

    public static boolean isConfigured()
    {
        try
        {
            return XDatabase.getXDatabase().canReadDatabaseConnectionInfo("connection");
        }
        catch (NullPointerException n)
        {
            return false;
        }
    }
}
