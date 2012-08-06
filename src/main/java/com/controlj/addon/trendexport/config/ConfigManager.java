package com.controlj.addon.trendexport.config;

// Manages config object from Xdatabase datastore for persistence

import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.xdatabase.DatabaseConnectionInfo;
import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import com.controlj.green.addonsupport.xdatabase.XDatabase;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ConfigManager
{
    private DatabaseConnectionInfo currentConnectionInfo;
    private Configuration configuration;

    public ConfigManager(String location, int port, String user, String password, DatabaseType type, String instance)
    {
        currentConnectionInfo = XDatabase.getXDatabase().newDatabaseConnectionInfo();
        currentConnectionInfo.setType(type);
        currentConnectionInfo.setHost(location);
        currentConnectionInfo.setPort(port);
        currentConnectionInfo.setUser(user);
        currentConnectionInfo.setPasswd(password);
        currentConnectionInfo.setInstance(instance);

        configuration = new Configuration("12", Configuration.CollectionMethod.Interval);
    }

    public ConfigManager(String location, int port, String user, String passwd, DatabaseType type, String instance, Configuration config)
    {
        currentConnectionInfo = XDatabase.getXDatabase().newDatabaseConnectionInfo();
        currentConnectionInfo.setType(type);
        currentConnectionInfo.setHost(location);
        currentConnectionInfo.setPort(port);
        currentConnectionInfo.setUser(user);
        currentConnectionInfo.setPasswd(passwd);
        currentConnectionInfo.setInstance(instance);

        configuration = config;
    }

    public void load() throws Exception
    {
        try
        {
            currentConnectionInfo = XDatabase.getXDatabase().readDatabaseConnectionInfo("connection");
        }
        catch (IOException e)
        {
            configuration = new Configuration("12", Configuration.CollectionMethod.Interval);
        }

        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        configuration = connection.runReadAction(new ReadActionResult<Configuration>()
        {
            @Override
            public Configuration execute(@NotNull SystemAccess systemAccess) throws IOException
            {
                DataStore store = systemAccess.getSystemDataStore("TrendExportConfig");
                BufferedReader reader = store.getReader();

//                    long timeInterval = Long.valueOf(reader.readLine());
                String timeInterval = reader.readLine();
                String collMethod = reader.readLine();

                if (collMethod == null || timeInterval == null)
                    throw new IOException();

                Configuration.CollectionMethod method;
                if (collMethod.contains("Interval"))
                    method = Configuration.CollectionMethod.Interval;
                else
                    method = Configuration.CollectionMethod.SpecifiedTime;


                String alarmPath = reader.readLine();
                if (alarmPath == null || alarmPath.isEmpty())
                    return new Configuration(timeInterval, method);
                else
                    return new Configuration(timeInterval, method, alarmPath);
            }
        });
    }

    public void save() throws IOException
    {
        XDatabase.getXDatabase().saveDatabaseConnectionInfo("connection", getCurrentConnectionInfo());

        try
        {
            SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
            connection.runWriteAction("TRENDEXP_CONFIG", new WriteAction()
            {
                @Override
                public void execute(@NotNull WritableSystemAccess access) throws IOException
                {
                    DataStore store = access.getSystemDataStore("TrendExportConfig");
                    PrintWriter writer = store.getWriter();

                    writer.println(ConfigManager.this.configuration.getCollectionString());
                    writer.println(ConfigManager.this.configuration.getCollectionMethod());
                    writer.println(ConfigManager.this.configuration.getAlarmControlProgramPath());

                    writer.flush();
                    writer.close();
                }
            });
        }
        catch (Exception e)
        {
            throw new IOException("Saving configuration failed.", e);
        }
    }

    public DatabaseConnectionInfo getCurrentConnectionInfo() throws IOException
    {
        if (currentConnectionInfo == null)
            if (XDatabase.getXDatabase().canReadDatabaseConnectionInfo("connection"))
                currentConnectionInfo = XDatabase.getXDatabase().readDatabaseConnectionInfo("connection");

        return currentConnectionInfo;
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }
}
