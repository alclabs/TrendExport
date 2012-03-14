package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.DataCollector;
import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.helper.TrendSourceTypeAndPathResolver;
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

        if (nodeLookupStrings.contains("0123thisIsNotValid"))
            result = "Please add a real source from the Add or Remove Tab";

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
            {
                removeSource(nodeLookupStrings, synchronizer);
            }
            else if (actionToAttempt.contains("collectData"))
                collectData(nodeLookupStrings, synchronizer);
            else if (actionToAttempt.contains("enableSource") || actionToAttempt.contains("disableSource"))
                enableSource(nodeLookupStrings, synchronizer, actionToAttempt.contains("enableSource"));


            resp.getWriter().print(writeResult(result, nodeLookupStrings));
        }
        catch (DatabaseException e)
        {
            resp.getWriter().print(writeResult("Action not successful", nodeLookupStrings));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e, AlarmHandler.TrendExportAlarm.CollectionDatabaseCommError);
        }
        catch (SystemException e)
        {
            resp.getWriter().print(writeResult("Action not successful", nodeLookupStrings));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
        }
        catch (ActionExecutionException e)
        {
            resp.getWriter().print(writeResult("Action not successful", nodeLookupStrings));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e);
        }
        catch (DatabaseVersionMismatchException e)
        {
            resp.getWriter().print(writeResult("Action not successful", nodeLookupStrings));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed (Database version mismatch)",
                    e, AlarmHandler.TrendExportAlarm.CollectionDatabaseCommError);
        }
        catch (UpgradeException e)
        {
            resp.getWriter().print(writeResult("Action not successful", nodeLookupStrings));
            ErrorHandler.handleError("AddOrRemoveSerlvet Failed!", e,
                    AlarmHandler.TrendExportAlarm.CollectionDatabaseCommError);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (synchronizer != null)
                synchronizer.disconnect();
        }
    }

    private JSONObject writeResult(String message, List<String> nodeLookupStrings)
    {
        JSONObject response = new JSONObject();

        try
        {
            StringBuilder builder = new StringBuilder();
            for (String singleLookup : nodeLookupStrings)
            {
                if (singleLookup.contains("DBID:"))
                    builder.append(singleLookup);
                else
                    builder.append(TrendSourceTypeAndPathResolver.getPersistentLookupString(singleLookup));
                builder.append(";;");
            }

            response.put("result", message);
            response.put("lookups", builder.toString());
        }
        catch (JSONException e)
        {
            Logger.println("JSON Failed to write in AddRemoveServlet", e);
        }
        catch (ActionExecutionException e)
        {
            e.printStackTrace();
        }
        catch (SystemException e)
        {
            e.printStackTrace();
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
                    String referencePath = TrendSourceTypeAndPathResolver.getReferencePath(startLoc);
                    String fullDisplayPath = TrendSourceTypeAndPathResolver.getFullDisplayPath(startLoc, new StringBuilder()).toString();

                    synchronizer.addSourceAndTableName(referencePath, startLoc.getDisplayName(), fullDisplayPath, tableName, type);
                }
            }
        });


    }

    private void removeSource(final List<String> nodeLookups, final DBAndSchemaSynchronizer synchronizer)
            throws DatabaseVersionMismatchException, UpgradeException, DatabaseException, SystemException, ActionExecutionException
    {
        // resolve any DBIDS to GQL reference paths as well
        for (String referencePath : nodeLookups)
        {
            String temp = referencePath;
            if (temp.contains("DBID:"))
                temp = resolveLookupStringToReferencePath(temp);

            synchronizer.removeSource(temp, false);
        }
    }

    private String resolveLookupStringToReferencePath(final String lookupString) throws SystemException, ActionExecutionException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        return connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadActionResult<String>()
        {
            @Override
            public String execute(@NotNull SystemAccess access) throws Exception
            {
                Location location = access.getTree(SystemTree.Geographic).resolve(lookupString);
                return TrendSourceTypeAndPathResolver.getReferencePath(location);
            }
        });
    }

    private JSONObject enableSource(List<String> referenceList, DBAndSchemaSynchronizer synchronizer, boolean enable) throws JSONException
    {
        JSONObject object = new JSONObject();

        for (String referencePath : referenceList)
        {
            try
            {
                synchronizer.enableOrDisableSource(referencePath, enable);
            }
            catch (DatabaseException e)
            {
                ErrorHandler.handleError("Error enabling source: " + referencePath, e);
                object.put("result", "Error enabling/disabling source: " + referencePath);
                return object;
//                e.printStackTrace();
            }
            catch (ActionExecutionException e)
            {
                e.printStackTrace();
            }
            catch (SystemException e)
            {
                e.printStackTrace();
            }
        }

        object.put("result", "Success!");
        //require reverse lookup to get persistent lookup string for updating tree....
        //object.put("nodeLookupString", )
        return object; // needs to be results if successful or failure
    }

    private DBAndSchemaSynchronizer initializeSynchronizer() throws IOException
    {
        ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
        return DBAndSchemaSynchronizer.getSynchronizer(manager.getCurrentConnectionInfo());
    }
}
