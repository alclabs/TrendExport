package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.helper.TrendPathAndDBTableName;
import com.controlj.addon.trendexport.helper.TrendSourceTypeAndPathResolver;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.access.ActionExecutionException;
import com.controlj.green.addonsupport.access.SystemException;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public class TableListServlet extends HttpServlet
{
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        doPost(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("application/json");
        DBAndSchemaSynchronizer synchronizer = null;

        try
        {
            ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
            synchronizer = DBAndSchemaSynchronizer.getSynchronizer(manager.getCurrentConnectionInfo());
            synchronizer.connect();

            JSONObject object = getCurrentList(synchronizer);

            resp.getWriter().print(object);
        }
        catch (Exception e)
        {
            ErrorHandler.handleError("Error", e);
            resp.getWriter().print(e);
        }
        finally
        {
            if (synchronizer != null)
                synchronizer.disconnect();
        }
    }

    private JSONObject getCurrentList(DBAndSchemaSynchronizer synchronizer)
            throws JSONException, SystemException, ActionExecutionException, DatabaseException
    {
        JSONArray jsonArray = new JSONArray();
        Collection<TrendPathAndDBTableName> stuffs = synchronizer.getAllTrends();

        // catch an empty list and make a dummy object - prevents a crash in datatables
        if (stuffs.isEmpty())
        {
            JSONObject emptyObject = new JSONObject();

            emptyObject.put("sourceLookupString", "0123thisIsNotValid");
            emptyObject.put("sourceDisplayName", "Add a source from the Add or Remove Tab");
            emptyObject.put("displayPath", "Add a source from the Add or Remove Tab");
            emptyObject.put("tableName", "Add a source from the Add or Remove Tab");
            emptyObject.put("isEnabled", " ");

            jsonArray.put(emptyObject);
        }

        for (TrendPathAndDBTableName trendPathAndDBTableName : stuffs)
        {
            JSONObject object = new JSONObject();

            String referencePath = trendPathAndDBTableName.getTrendSourceReferencePath();
            object.put("sourceLookupString", TrendSourceTypeAndPathResolver.getPersistentLookupString(referencePath));
            object.put("sourceReferencePath", referencePath);
            object.put("sourceDisplayName", trendPathAndDBTableName.getDisplayName());
            object.put("displayPath", trendPathAndDBTableName.getTrendSourceDisplayPath());
            object.put("tableName", trendPathAndDBTableName.getDbTableName());
//            object.put("tableEntries", trendPathAndDBTableName.)
            object.put("isEnabled", trendPathAndDBTableName.getIsEnabled());

            jsonArray.put(object);
        }

        JSONObject responseObject = new JSONObject();
        responseObject.put("aaData", jsonArray);
        return responseObject;
    }
}
