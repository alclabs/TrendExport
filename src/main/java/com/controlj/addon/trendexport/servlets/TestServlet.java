package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.DataStoreRetriever;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import com.controlj.green.addonsupport.access.trend.TrendData;
import com.controlj.green.addonsupport.access.trend.TrendRangeFactory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class TestServlet extends HttpServlet

{
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("application/json");
        ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
        DBAndSchemaSynchronizer synchronizer = null;
        JSONObject responseObject = new JSONObject();

        // get parameters
        String actionToAttempt = req.getParameter("testToRun");
        String nodeToAdd = req.getParameter("newNode");

        // set up objects
        try
        {
            synchronizer = new DBAndSchemaSynchronizer(manager.getCurrentConnectionInfo());

            if (actionToAttempt.contains("connect"))
            {
                synchronizer.connect();
                responseObject.put("result", "Connection successful");
            }
            else if (actionToAttempt.contains("addSource"))
            {
                synchronizer.connect();
                synchronizer.addSource(nodeToAdd);

                responseObject.put("result", "Success! Number of mappings: " + synchronizer.sizeOfSourceMappings());

            }
            else if (actionToAttempt.contains("getAllTrendData"))
            {
                synchronizer.connect();
                getTrendHistoryForASource(nodeToAdd, synchronizer);
                responseObject.put("result", ":D");
            }
            else if (actionToAttempt.contains("getLatestTrendData"))
            {
                synchronizer.connect();
                getLatestTrendHistory(nodeToAdd, synchronizer);
                responseObject.put("result", ":D");
            }

            else if (actionToAttempt.contains("removeSource"))
            {
                synchronizer.connect();
//                synchronizer.removeSource(nodeToAdd);

                responseObject.put("result", "Success!: " + synchronizer.sizeOfSourceMappings());
            }
//            else if ()
//            {

//            }
            // more tests to come ^_^

        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                responseObject.put("result", ":(\n" + e.getMessage());
            }
            catch (JSONException e1)
            {
                e1.printStackTrace();
            }
        }
        finally
        {
            if (synchronizer != null)
                synchronizer.disconnect();
        }

        resp.getWriter().print(responseObject);
        resp.flushBuffer();
    }

    // get data from source location - history for ALLLLLL time (from java epoch) -- nneds to record last sample time gathered(store in mappings or something)
    private void getTrendHistoryForASource(final String nodeLookupString, final DBAndSchemaSynchronizer synchronizer)
            throws Exception
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
        {
            @Override
            public void execute(@NotNull SystemAccess systemAccess) throws Exception
            {
                Location startLoc = systemAccess.getTree(SystemTree.Geographic).resolve(nodeLookupString);
                TrendSource trendSource = startLoc.getAspect(TrendSource.class);

                synchronizer.insertTrendSamples(nodeLookupString,
                        trendSource.getTrendData(
                                TrendRangeFactory.byDateRange(
                                        new Date(0), new Date()
                                )
                        ), 0);
            }
        });
    }

    // only gets the history from the last date recorded
    private void getLatestTrendHistory(final String nodeLookupString, final DBAndSchemaSynchronizer synchronizer)
            throws Exception
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
        {
            @Override
            public void execute(@NotNull SystemAccess systemAccess) throws Exception
            {
                Location startLoc = systemAccess.getTree(SystemTree.Geographic).resolve(nodeLookupString);
                TrendSource trendSource = startLoc.getAspect(TrendSource.class);

                // start using the retriever
                DataStoreRetriever retriever = synchronizer.getRetrieverForTrendSource(nodeLookupString);
                Date startDate = retriever.getLastRecordedDate();
                int numberOfSamplesToSkip = retriever.getNumberOfSamplesToSkip();
                TrendData trendData = trendSource.getTrendData(TrendRangeFactory.byDateRange(startDate, new Date()));

                synchronizer.insertTrendSamples(nodeLookupString, trendData, numberOfSamplesToSkip);
            }
        });
    }
}
