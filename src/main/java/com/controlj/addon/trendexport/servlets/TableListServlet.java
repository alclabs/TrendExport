package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.helper.TrendPathAndDBTableName;
import com.controlj.addon.trendexport.helper.TrendSourceTypeAndPathResolver;
import com.controlj.addon.trendexport.statistics.StatisticsLibrarian;
import com.controlj.addon.trendexport.statistics.Statistics;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.access.SystemException;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
import com.controlj.green.addonsupport.xdatabase.DatabaseVersionMismatchException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
        DBAndSchemaSynchronizer synchronizer = null;

        try
        {
            if (!req.getParameterMap().isEmpty())
            {
                resp.setContentType("text/html");
                resp.getWriter().print(convertStatisticsToHTMLTable(req.getParameter("source")));
            }
            else
            {
                resp.setContentType("application/json");
                ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
                synchronizer = DBAndSchemaSynchronizer.getSynchronizer(manager.getCurrentConnectionInfo());
                synchronizer.connect();

                JSONObject object = getCurrentList(synchronizer);
                resp.getWriter().print(object);
            }
        }
        catch (IOException e)
        {
            ErrorHandler.handleError("Unable to load configuration.", e);
            resp.sendError(500, "Unable to load configuration.");
        }
        catch (DatabaseException e)
        {
            resp.sendError(500, "Database Corrupted. This is usually related to the MetaData Table");
        }
        catch (Exception e)
        {
            resp.sendError(500, "Unable to load list due to an unspecified error.");
        }
        finally
        {
            if (synchronizer != null)
                synchronizer.disconnect();
        }
    }

    private JSONObject getCurrentList(DBAndSchemaSynchronizer synchronizer)
            throws JSONException, DatabaseException, SystemException, com.controlj.green.addonsupport.access.UnresolvableException, DatabaseVersionMismatchException
    {
        JSONArray jsonArray = new JSONArray();
        Collection<TrendPathAndDBTableName> stuffs = synchronizer.getAllSources();

        // catch an empty list and make a dummy object - prevents a crash in datatables
        if (stuffs.isEmpty())
        {
            JSONObject emptyObject = new JSONObject();

            emptyObject.put("sourceReferencePath", "0123thisIsNotValid");
            emptyObject.put("sourceLookupString", "0123thisIsNotValid");
            emptyObject.put("sourceDisplayName", "Add a source from the Add or Remove Tab");
            emptyObject.put("displayPath", "Add a source from the Add or Remove Tab");
            emptyObject.put("tableName", "Add a source from the Add or Remove Tab");
            emptyObject.put("isEnabled", " ");

            jsonArray.put(emptyObject);
        }

        for (TrendPathAndDBTableName trendPathAndDBTableName : stuffs)
        {
            try
            {
                JSONObject object = new JSONObject();

                String referencePath = trendPathAndDBTableName.getTrendSourceReferencePath();
                object.put("sourceLookupString", TrendSourceTypeAndPathResolver.getPersistentLookupString(referencePath));
                object.put("sourceReferencePath", referencePath);
                object.put("sourceDisplayName", trendPathAndDBTableName.getDisplayName());
                object.put("displayPath", trendPathAndDBTableName.getTrendSourceDisplayPath());
                object.put("tableName", trendPathAndDBTableName.getDbTableName());
//            object.put("tableEntries", trendPathAndDBTableName.)

                if (trendPathAndDBTableName.getIsEnabled())
                    object.put("isEnabled", "Enabled");
                else
                    object.put("isEnabled", "Disabled");

                jsonArray.put(object);
            }
            catch (com.controlj.green.addonsupport.access.UnresolvableException e)
            {
                ErrorHandler.handleError(e.getMessage(), e);
            }
        }

        JSONObject responseObject = new JSONObject();
        responseObject.put("aaData", jsonArray);
        return responseObject;
    }

    private String convertStatisticsToHTMLTable(String source)
    {
//        get the collection of stats for a source
        List<Statistics> sourceStatsList = new StatisticsLibrarian().getStatisticsForSource(source);
        if (sourceStatsList.isEmpty())
            return "<p style=\"background-color:#d7e1c5;\">No stats found for this source.</p>";

        StringBuilder builder = new StringBuilder();
//        builder.append("<table cellpadding = \"5\" cellspacing=\"0\" border=\"0\" style=\"padding-left:50px;background-color:#d7e1c5;\">");
        builder.append("<table class=\"pretty\" >");
        builder.append("<thead><tr>")
               .append("<th>Date of Collection</th>")
               .append("<th>Collection Duration</th>")
               .append("<th>Samples Collected</th></tr></thead>")
                .append("<tbody>");

        int i = 0;
        for (Statistics s : sourceStatsList)
        {
            builder.append(createHTMLForStatistics(s, i++));
        }

        builder.append("</tbody></table>");
        return builder.toString();
    }

    private String createHTMLForStatistics(Statistics statistics, int index)
    {
        return createTableRowForList(statistics.getDate(), statistics.getElapsedTime(), statistics.getSamples(), index % 2 == 0);
    }

    private String createTableRowForList(Date date, Long elapsedTime, Long samples, boolean isOdd)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("<tr class=\"").append(isOdd ? "odd" : "even").append("\">").
                append("<td>").append(DateFormat.getDateTimeInstance().format(date)).
                append("</td><td>").append(formatTime(elapsedTime)).
                append("</td><td>").append(NumberFormat.getInstance().format(samples)).
                append("</td></tr>");

        return builder.toString();
    }

    private String formatTime(Long millis)
    {
        if (millis < 1000)
            return "< 1 second";

        long seconds = millis / 1000;

        int iterations = 0;
        double time = seconds;
        while (time > 60)
        {
            time /= 60;
            iterations++;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = iterations; i >= 0; i--)
        {
            builder.append((int) time).append(' ').append(getTimeUnit(i)).append(' ');
            time -= (int) time;
            time *= 60;
        }

        return builder.toString();
    }

    private String getTimeUnit(int iteration)
    {
        if (iteration > 1)
            return "hour(s)";
        else if (iteration == 1)
            return "minute(s)";
        else
            return "seconds";


    }
}
