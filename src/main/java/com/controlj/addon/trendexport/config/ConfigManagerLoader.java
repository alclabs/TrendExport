package com.controlj.addon.trendexport.config;

import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import com.controlj.green.addonsupport.xdatabase.XDatabase;

public class ConfigManagerLoader
{
    public ConfigManager loadConnectionInfoFromDataStore() throws Exception
    {
        ConfigManager manager = new ConfigManager("", 0, "", "", DatabaseType.Derby, "");
        manager.load();
        return manager;
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
