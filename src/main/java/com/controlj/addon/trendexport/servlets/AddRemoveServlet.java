package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.DataCollector;
import com.controlj.addon.trendexport.helper.TrendTableNameGenerator;
import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.addon.trendexport.util.Logger;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
import com.controlj.green.addonsupport.xdatabase.DatabaseVersionMismatchException;
import com.controlj.green.addonsupport.xdatabase.UpgradeException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddRemoveServlet extends HttpServlet
{
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("application/json");

        // get parameters
        String actionToAttempt = req.getParameter("action");
        List<String> nodeLookupStrings = sanitizeLookupStrings(req.getParameter("nodeLookupString"));

        DBAndSchemaSynchronizer synchronizer = null;
        String result = "Success!";

        try
        {
            synchronizer = initializeSynchronizer();
            synchronizer.connect();

            // perform action
            if (actionToAttempt.contains("addSource"))
            {
                String tableName = req.getParameter("tableName");
                if (TrendTableNameGenerator.isTableNameValid(tableName))
                    addSource(nodeLookupStrings, synchronizer, tableName);
                else
                    result = "Table Name is not valid";
            }
            else if (actionToAttempt.contains("removeSource"))
                removeSource(nodeLookupStrings, synchronizer, req.getParameter("keepData"));
            else if (actionToAttempt.contains("collectData"))
                collectData(nodeLookupStrings, synchronizer);

            resp.getWriter().print(writeResult(result));
        }
        catch (DatabaseException e)
        {
            resp.getWriter().print(writeResult("Action not successful"));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e, AlarmHandler.TrendExportAlarm.CollectionDatabaseCommError);
        }
        catch (SystemException e)
        {
            resp.getWriter().print(writeResult("Action not successful"));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
        }
        catch (ActionExecutionException e)
        {
            resp.getWriter().print(writeResult("Action not successful"));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e);
        }
        catch (DatabaseVersionMismatchException e)
        {
            resp.getWriter().print(writeResult("Action not successful"));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed (Database version mismatch)",
                    e, AlarmHandler.TrendExportAlarm.CollectionDatabaseCommError);
        }
        catch (UpgradeException e)
        {
            resp.getWriter().print(writeResult("Action not successful"));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e,
                    AlarmHandler.TrendExportAlarm.CollectionDatabaseCommError);
        }
        finally
        {
            if (synchronizer != null)
                synchronizer.disconnect();
        }
    }

    private JSONObject writeResult(String message)
    {
        JSONObject response = new JSONObject();

        try
        {
            response.put("result", message);
        }
        catch (JSONException e)
        {
            Logger.println("JSON Failed to write in AddRemoveServlet", e);
        }

        return response;
    }

    private List<String> sanitizeLookupStrings(String input)
    {
        List<String> collection = new ArrayList<String>();
        String[] split = input.split(";;");

        Collections.addAll(collection, split);

        return collection;
    }

    private void collectData(List<String> sources, DBAndSchemaSynchronizer synchronizer)
    {
        for (String source : sources)
        {
            try
            {
                synchronizer.connect();
                DataCollector.collectDataForSource(source, synchronizer);
            }
            catch (SystemException e)
            {
                ErrorHandler.handleError("AddRemove - System Error", e);
            }
            catch (ActionExecutionException e)
            {
                ErrorHandler.handleError("AddRemove - ActionExecution Error", e);
            }
            catch (DatabaseVersionMismatchException e)
            {
                e.printStackTrace();
            }
            catch (UpgradeException e)
            {
                e.printStackTrace();
            }
            catch (DatabaseException e)
            {
                e.printStackTrace();
            }
            finally {
                if (synchronizer != null)
                    synchronizer.disconnect();
            }
        }
    }

    private void addSource(final List<String> nodeLookups, final DBAndSchemaSynchronizer synchronizer, final String tableName)
            throws SystemException, ActionExecutionException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
        {
            @Override
            public void execute(@NotNull SystemAccess access) throws Exception
            {
                for (String nodeLookupString : nodeLookups)
                {
                    Location startLoc = access.getTree(SystemTree.Geographic).resolve(nodeLookupString);
                    TrendSource trendSource = startLoc.getAspect(TrendSource.class);
                    TrendSource.Type type = trendSource.getType();

                    synchronizer.addSourceAndTableName(nodeLookupString, startLoc.getDisplayName(), startLoc.getDisplayPath(), tableName, type);
                }
            }
        });


    }

    private void removeSource(List<String> nodeLookups, DBAndSchemaSynchronizer synchronizer, String keepData)
            throws DatabaseVersionMismatchException, UpgradeException, DatabaseException
    {
        for (String nodeLookupString : nodeLookups)
            synchronizer.removeSource(nodeLookupString, keepData.equals("true"));
    }

    private DBAndSchemaSynchronizer initializeSynchronizer() throws IOException
    {
        ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
        return new DBAndSchemaSynchronizer(manager.getCurrentConnectionInfo());
    }
}
