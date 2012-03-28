package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.DataCollector;
import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.exceptions.SourceMappingNotFoundException;
import com.controlj.addon.trendexport.exceptions.TableNotInDatabaseException;
import com.controlj.addon.trendexport.helper.TrendSourceTypeAndPathResolver;
import com.controlj.addon.trendexport.helper.TrendTableNameGenerator;
import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.addon.trendexport.util.Logger;
import com.controlj.addon.trendexport.util.ScheduledTrendCollector;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
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

            try
            {
                synchronizer.connect();
            }
            catch (DatabaseException e)
            {
                ErrorHandler.handleError("Database connection error", e, AlarmHandler.TrendExportAlarm.DatabaseWriteFailure);
                resp.sendError(500, "Error processing request");
                return;
            }

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
            {
                collectData(nodeLookupStrings, synchronizer);
            }
            else if (actionToAttempt.contains("enableSource") || actionToAttempt.contains("disableSource"))
            {
                enableSource(nodeLookupStrings, synchronizer, actionToAttempt.contains("enableSource"));
            }
            else if (actionToAttempt.contains("getCollectorStatus"))
            {
                result = "Next automatic collection time:    " + ScheduledTrendCollector.getNextCollectionDate();
                if (DataCollector.isCollectorBusy())
                    result = "Running Collection for table:    " + ScheduledTrendCollector.getTableName();
            }

            resp.getWriter().print(writeResult(result, nodeLookupStrings));
        }
//        catch (TrendException e)
//        {
        // not able to retrieve from WebCTRL
//            ErrorHandler.handleError("AddOrRemoveSerlvet error", e);
//            resp.sendError(500, "WebCTRL Database collection error.");
//        }
//        catch (DatabaseException e)
//        {
//            resp.getWriter().print(writeResult("Action not successful", nodeLookupStrings)); // todo: needed?
//            ErrorHandler.handleError("AddOrRemoveSerlvet error", e);
//            resp.sendError(500, "Database communications error.  Make sure the database server is running.");
//        }
        catch (Exception e)
        {
            // resp.getWriter().print(writeResult("Action not successful", nodeLookupStrings)); // todo: needed?
            ErrorHandler.handleError("AddOrRemoveSerlvet error", e);
            resp.sendError(500, "Error processing request");
        }
        finally
        {
            if (synchronizer != null)
                synchronizer.disconnect();
        }
    }

    private JSONObject writeResult(String message, List<String> nodeLookupStrings) throws SystemException, UnresolvableException
    {
        JSONObject response = new JSONObject();

        try
        {
            StringBuilder builder = new StringBuilder();
            response.put("result", message);

            for (String singleLookup : nodeLookupStrings)
            {
                if (singleLookup.contains("DBID:"))
                    builder.append(singleLookup);
                else
                    builder.append(TrendSourceTypeAndPathResolver.getPersistentLookupString(singleLookup));
                builder.append(";;");
            }
            response.put("lookups", builder.toString());

//            response.put("collectorStatus", DataCollector.isCollectorBusy() + ":" + DataCollector.getTableName());
        }
        catch (JSONException e)
        {
            Logger.println("JSON Failed to write in AddRemoveServlet", e);
        }

        return response;
    }

    private List<String> sanitizeLookupStrings(String input)
    {
        if (input.isEmpty())
            return Collections.emptyList();

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
            catch (SourceMappingNotFoundException e)
            {
                ErrorHandler.handleError("SourceMapping not found", e);
            }
            catch (SystemException e)
            {
                ErrorHandler.handleError("", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
            }
            catch (TrendException e)
            {
                ErrorHandler.handleError("Unable to retrieve TrendSource data from WebCTRL database", e, AlarmHandler.TrendExportAlarm.CollectionFailure);
            }
            catch (TableNotInDatabaseException e)
            {
                ErrorHandler.handleError("Table not present in database", e, AlarmHandler.TrendExportAlarm.DatabaseWriteFailure);
            }
            catch (DatabaseException e)
            {
                ErrorHandler.handleError("Database communication error", e, AlarmHandler.TrendExportAlarm.DatabaseWriteFailure);
            }
        }
    }

    private void addSource(final List<String> nodeLookups, final DBAndSchemaSynchronizer synchronizer, final String tableName)
            throws DatabaseException, UnresolvableException, NoSuchAspectException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        try
        {
            connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
            {
                @Override
                public void execute(@NotNull SystemAccess access) throws DatabaseException, UnresolvableException, NoSuchAspectException
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
        catch (ActionExecutionException e)
        {
            if (e.getCause() instanceof DatabaseException)
                throw (DatabaseException) e.getCause();
            else if (e.getCause() instanceof UnresolvableException)
                throw (UnresolvableException) e.getCause();
            else if (e.getCause() instanceof NoSuchAspectException)
                throw (NoSuchAspectException) e.getCause();
            else
                throw new DatabaseException("Unexpected error", e); // todo
        }
        catch (SystemException e)
        {
            throw new DatabaseException("Unexpected system error", e); // todo
        }
    }

    private void removeSource(final List<String> nodeLookups, final DBAndSchemaSynchronizer synchronizer)
            throws DatabaseException, UnresolvableException
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

    private String resolveLookupStringToReferencePath(final String lookupString) throws UnresolvableException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        try
        {
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
        catch (ActionExecutionException e)
        {
            if (e.getCause() instanceof UnresolvableException)
                throw (UnresolvableException) e.getCause();
            else
                throw new UnresolvableException("Cannot resolve because of unexpected error", e);
        }
        catch (SystemException e)
        {
            throw new UnresolvableException("Cannot resolve because of unexpected system error", e);
        }
    }

    private JSONObject enableSource(List<String> referenceList, DBAndSchemaSynchronizer synchronizer, boolean enable) throws JSONException, SourceMappingNotFoundException
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

    private DBAndSchemaSynchronizer initializeSynchronizer() throws Exception
    {
        ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
        return DBAndSchemaSynchronizer.getSynchronizer(manager.getCurrentConnectionInfo());
    }
}
