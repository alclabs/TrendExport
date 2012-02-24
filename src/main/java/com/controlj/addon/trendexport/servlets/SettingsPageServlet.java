package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.Config.ConfigManager;
import com.controlj.addon.trendexport.Config.ConfigManagerLoader;
import com.controlj.addon.trendexport.Config.Configuration;
import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.addon.trendexport.util.ScheduledTrendCollector;
import com.controlj.green.addonsupport.access.ActionExecutionException;
import com.controlj.green.addonsupport.access.SystemException;
import com.controlj.green.addonsupport.access.WriteAbortedException;
import com.controlj.green.addonsupport.xdatabase.DatabaseConnectionException;
import com.controlj.green.addonsupport.xdatabase.DatabaseType;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;

public class SettingsPageServlet extends HttpServlet
{

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("application/json");
        ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
        JSONObject responseObject = new JSONObject();

        try
        {
            if (req.getParameterMap().isEmpty())
                responseObject = getResponseObject(manager);
            else if (req.getParameter("action").contains("connect"))
            {
                ConfigManager newManager = createConfigManagerFromRequest(req);
                responseObject.put("result", attemptConnection(newManager));
            }
            else if (req.getParameter("action").contains("save"))
                saveConfiguration(createConfigManagerFromRequest(req));
            else if (req.getParameter("action").contains("testAlarm"))
                testAlarmProgram(manager, req.getParameter("alarmPath"));

            responseObject.put("result", "Success!");
        }
        catch (Exception e)
        {
            try
            {
                responseObject.put("result", "Action Failed");
                ErrorHandler.handleError("Settings Servlet - failed", e);
            }
            catch (JSONException e1)
            {
                ErrorHandler.handleError("Settings Servlet - JSON Failed", e1);
            }
        }
        finally
        {
            if (responseObject != null)
                resp.getWriter().print(responseObject);
        }
    }

    private void testAlarmProgram(ConfigManager manager, String alarmPath)
    {
        ErrorHandler.handleError("Testing Alarm...", new Exception("Just a test"), AlarmHandler.TrendExportAlarm.Test);
    }

    private DatabaseType getDatabaseType(String dbType)
    {
        dbType = dbType.toLowerCase();

        if (dbType.contains("mysql"))
            return DatabaseType.MySQL;
        else if (dbType.contains("postgresql "))
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
            manager.getCurrentConnectionInfo().tryConnection(5000);
            return "Connection Successful!";
        }
        catch (DatabaseConnectionException e)
        {
            return "Connection Failed!";
        }
        catch (IOException e)
        {
            return "Connection Failed (Unable to reach configuration)";
        }
    }

    private ConfigManager createConfigManagerFromRequest(HttpServletRequest req) throws SystemException, IOException, ActionExecutionException, WriteAbortedException
    {
        String dbType = req.getParameter("dbType");
        DatabaseType databaseType = getDatabaseType(dbType);
        String host = req.getParameter("host");
        int port = Integer.valueOf(req.getParameter("port"));
        String instance = req.getParameter("instance");
        String user = req.getParameter("user");
        String pass = req.getParameter("pass");
        String method = req.getParameter("collMethod");
        String value = req.getParameter("collValue");
        String alarmPath = req.getParameter("alarmPath");

        long time = 0;
        if (value.contains(":"))
        {
            int start = value.indexOf(':');
            int hours = Integer.parseInt(value.substring(0, start));
            int minutes = Integer.parseInt(value.substring(start + 1, value.lastIndexOf(':'))) * 60;
            // parse for time instead

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR, hours);
            cal.set(Calendar.MINUTE, minutes);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (value.contains("AM"))
                cal.set(Calendar.AM_PM, Calendar.AM);
            else
            {
                cal.set(Calendar.AM_PM, Calendar.PM);
                time = 12 * 3600000; // hours * 60 * 60 * 1000 = ms)
            }

            time += cal.getTimeInMillis();
        }
        else
        {
            time = Long.parseLong(value);
            if (time < 1 || time > 1440)
                throw new IllegalArgumentException("Time must be within 1 and 1440 hours");
        }

        Configuration.CollectionMethod collectionMethod;
        if (method.contains("interval"))
            collectionMethod = Configuration.CollectionMethod.Interval;
        else
            collectionMethod = Configuration.CollectionMethod.SpecifiedTime;

        Configuration configuration;
        if (!alarmPath.isEmpty())
            configuration = new Configuration(time, collectionMethod, alarmPath);
        else
            configuration = new Configuration(time, collectionMethod);

        return new ConfigManager(host, port, user, pass, databaseType, instance, configuration);
    }

    private void saveConfiguration(ConfigManager newConfigManager)
            throws SystemException, IOException, ActionExecutionException, WriteAbortedException
    {
        newConfigManager.save();
        ScheduledTrendCollector.restartCollector(newConfigManager);
    }

    private JSONObject getResponseObject(ConfigManager manager) throws IOException, JSONException
    {
        JSONObject responseObject = new JSONObject();
        responseObject.put("dbType", manager.getCurrentConnectionInfo().getType().toString().toLowerCase());
        responseObject.put("host", manager.getCurrentConnectionInfo().getHost());
        responseObject.put("port", manager.getCurrentConnectionInfo().getPort());
        responseObject.put("instance", manager.getCurrentConnectionInfo().getInstance());
        responseObject.put("user", manager.getCurrentConnectionInfo().getUser());
        responseObject.put("pass", manager.getCurrentConnectionInfo().getPasswd());

        responseObject.put("collectionType", manager.getConfiguration().getCollectionMethod());
        responseObject.put("collectionValue", manager.getConfiguration().getCollectionValue());

        String alarmPath = manager.getConfiguration().getAlarmControlProgramPath();
        responseObject.put("alarmPath", alarmPath);

        return responseObject;
    }
}
