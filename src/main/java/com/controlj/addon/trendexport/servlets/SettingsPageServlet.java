package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.config.Configuration;
import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.addon.trendexport.util.Logger;
import com.controlj.addon.trendexport.util.ScheduledTrendCollector;
import com.controlj.green.addonsupport.AddOnInfo;
import com.controlj.green.addonsupport.Version;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.xdatabase.DatabaseConnectionException;
import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SettingsPageServlet extends HttpServlet
{

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("application/json");
        JSONObject responseObject = new JSONObject();

        try
        {
            ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();

            if (req.getParameterMap().isEmpty())
            {
                responseObject = getResponseObject(manager);
            }
            else if (req.getParameter("action").contains("connect"))
            {
                ConfigManager newManager = createConfigManagerFromRequest(req);
                responseObject.put("result", attemptConnection(newManager));
            }
            else if (req.getParameter("action").contains("save"))
            {
                try
                {
                    // test alarm if there is one
                    ConfigManager testManager = createConfigManagerFromRequest(req);

                    saveConfiguration(testManager);
                    responseObject.put("result", "Settings Saved");
                }
                catch (NumberFormatException e)
                {
                    responseObject.put("result", "Time is not valid. Set the time or interval correctly.");
                }
            }
            else if (req.getParameter("action").contains("testAlarm"))
                testAlarmProgram(req.getParameter("alarmPath"), responseObject);
        }
        catch (Exception e)
        {
            ErrorHandler.handleError("Configuration exception", e);
            resp.sendError(500, "Unable to process request.");
        }
        finally
        {
            if (responseObject != null)
                resp.getWriter().print(responseObject);
        }
    }

    private JSONObject testAlarmProgram(final String alarmPath, JSONObject responseObject) throws JSONException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        Boolean result = false;
        try
        {
            result = connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadActionResult<Boolean>()
            {
                @Override
                public Boolean execute(@NotNull SystemAccess access) throws UnresolvableException
                {
                    Location alarmLocation = access.resolveGQLPath(alarmPath);

                    try
                    {
                        return alarmLocation.hasChild(AlarmHandler.TrendExportAlarm.Test.getParameterRefName()) &&
                               alarmLocation.hasChild(AlarmHandler.TrendExportAlarm.CollectionFailure.getParameterRefName()) &&
                               alarmLocation.hasChild(AlarmHandler.TrendExportAlarm.HistorianDisabled.getParameterRefName()) &&
                               alarmLocation.hasChild(AlarmHandler.TrendExportAlarm.DatabaseWriteFailure.getParameterRefName());
                    }
                    catch (Exception e)
                    {
                        return false;
                    }
                }
            });
        }
        catch (Exception e)
        {
            if (e.getCause() instanceof UnresolvableException)
                result = false;
        }

        if (result != null && result)
        {
            AlarmHandler alarmHandler = new AlarmHandler(alarmPath);
            responseObject.put("result", "Please check WebCTRL's alarms to see if an alarm has been triggered from this add-on.");
            alarmHandler.triggerAlarm(AlarmHandler.TrendExportAlarm.Test);
        }
        else
            responseObject.put("result", "No alarm control program exists at this location.");

        return responseObject;
    }

    private DatabaseType getDatabaseType(String dbType)
    {
        dbType = dbType.toLowerCase();

        if (dbType.contains("mysql"))
            return DatabaseType.MySQL;
        else if (dbType.contains("postgresql"))
            return DatabaseType.PostgreSQL;
        else if (dbType.contains("oracle"))
            return DatabaseType.Oracle;
        else if (dbType.contains("sql server"))
            return DatabaseType.SQLServer;
        else
            return DatabaseType.Derby;
    }

    private String attemptConnection(ConfigManager manager)
    {
        try
        {
            manager.getCurrentConnectionInfo().tryConnection(1);
            return "Connection Successful!";
        }
        catch (DatabaseConnectionException e)
        {
            return "Connection Failed!\n" + e.getLocalizedMessage();
        }
        catch (IOException e)
        {
            return "Connection Failed (Unable to reach configuration)";
        }
    }

    private ConfigManager createConfigManagerFromRequest(HttpServletRequest req)
    {
        Logger.setDebugMode(req.getParameter("debugMode"));

        String dbType = req.getParameter("dbType");
        DatabaseType databaseType = getDatabaseType(dbType);
        String host = req.getParameter("host");
        int port = Integer.valueOf(req.getParameter("port"));
        if (port < 0)
            throw new IllegalArgumentException("Port cannot be negative");

        String instance = req.getParameter("instance");
        String user = req.getParameter("user");
        String pass = req.getParameter("pass");
        String method = req.getParameter("collMethod");
        String value = req.getParameter("collValue");
        String alarmPath = req.getParameter("alarmPath");

        Configuration.CollectionMethod collectionMethod;
        if (method.contains("interval"))
            collectionMethod = Configuration.CollectionMethod.Interval;
        else
            collectionMethod = Configuration.CollectionMethod.SpecifiedTime;

        Configuration configuration;
        if (!alarmPath.isEmpty())
            configuration = new Configuration(value, collectionMethod, alarmPath);
        else
            configuration = new Configuration(value, collectionMethod);

        return new ConfigManager(host, port, user, pass, databaseType, instance, configuration);
    }

    private void saveConfiguration(ConfigManager newConfigManager)
            throws IOException
    {
        newConfigManager.save();
        ScheduledTrendCollector.restartCollector(newConfigManager);
    }

    private JSONObject getResponseObject(ConfigManager manager)
            throws IOException, JSONException
    {
        JSONObject responseObject = new JSONObject();
        Version apiVersion = AddOnInfo.getAddOnInfo().getApiVersion();
        responseObject.put("api_version", apiVersion.getMajorVersionNumber() + "." +
                apiVersion.getMinorVersionNumber() + "." +
                apiVersion.getUpdateVersionNumber());

        responseObject.put("debugMode", Logger.getisDebugEnabled());
        responseObject.put("dbType", manager.getCurrentConnectionInfo().getType().toString().toLowerCase());
        responseObject.put("host", manager.getCurrentConnectionInfo().getHost());
        responseObject.put("port", manager.getCurrentConnectionInfo().getPort());
        responseObject.put("instance", manager.getCurrentConnectionInfo().getInstance());
        responseObject.put("user", manager.getCurrentConnectionInfo().getUser());
        responseObject.put("pass", manager.getCurrentConnectionInfo().getPasswd());

        responseObject.put("collectionType", manager.getConfiguration().getCollectionMethod());
        responseObject.put("collectionValue", manager.getConfiguration().getCollectionString());

        String alarmPath = manager.getConfiguration().getAlarmControlProgramPath();
        responseObject.put("alarmPath", alarmPath);

        return responseObject;
    }
}
