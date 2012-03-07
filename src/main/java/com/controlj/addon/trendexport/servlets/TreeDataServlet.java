/*
 * Copyright (c) 2011 Automated Logic Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.controlj.addon.trendexport.servlets;

import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.DataStoreRetriever;
import com.controlj.addon.trendexport.helper.TrendSourcePathResolvers;
import com.controlj.addon.trendexport.helper.TrendTableNameGenerator;
import com.controlj.addon.trendexport.util.AlarmHandler;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.InvalidConnectionRequestException;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import com.controlj.green.addonsupport.access.util.Acceptors;
import com.controlj.green.addonsupport.access.util.LocationSort;
import com.controlj.green.addonsupport.xdatabase.DatabaseException;
import com.controlj.green.addonsupport.xdatabase.DatabaseVersionMismatchException;
import com.controlj.green.addonsupport.xdatabase.UpgradeException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.controlj.green.addonsupport.access.LocationType.*;
import static com.controlj.green.addonsupport.access.LocationType.System;

public class TreeDataServlet extends HttpServlet
{
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("application/json");

        try
        {
            SystemConnection connection = DirectAccess.getDirectAccess().getUserSystemConnection(req);
            connection.runReadAction(new ReadAction()
            {
                @Override
                public void execute(@NotNull SystemAccess access) throws Exception
                {
                    ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
                    DBAndSchemaSynchronizer synchronizer = DBAndSchemaSynchronizer.getSynchronizer(manager.getCurrentConnectionInfo());

                    try
                    {
                        synchronizer.connect();
                        Tree geoTree = access.getTree(SystemTree.Geographic);

                        if (req.getParameterMap().size() == 1)
                        {
                            // initialize tree
                            Collection<Location> treeChildren = getEntries(geoTree, req.getParameter("key"));

                            JSONArray arrayData = toJSON(treeChildren, synchronizer);
                            arrayData.write(resp.getWriter());
                        }
                        else if (req.getParameter("mode").contains("lazyTree"))
                        {
                            // lazyRead tree
                            Collection<Location> treeChildren = getEntries(geoTree, req.getParameter("key"));
                            JSONArray arrayData = toJSON(treeChildren, synchronizer);

                            arrayData.write(resp.getWriter());
                        }
                        else if (req.getParameter("mode").contains("data"))
                        {
                            // get data about a node
                            Location location = geoTree.resolve(req.getParameter("key"));
                            String referencePath = TrendSourcePathResolvers.getReferencePath(location, new StringBuilder()).toString();

                            getTreeData(referencePath, synchronizer);
                        }
                    }
                    catch (Exception e)
                    {
                        ErrorHandler.handleError("System Error", e);
                        e.printStackTrace();
                    }
                    finally
                    {
                        synchronizer.disconnect();
                    }
                }
            });
        }
        catch (SystemException e)
        {
            ErrorHandler.handleError("System Error", e, AlarmHandler.TrendExportAlarm.CollectionDatabaseCommError);
        }
        catch (InvalidConnectionRequestException e)
        {
            ErrorHandler.handleError("Invalid Connection", e);
        }
        catch (ActionExecutionException e)
        {
            ErrorHandler.handleError("ActionExecution", e);
        }
    }


    private Collection<Location> getEntries(Tree tree, String lookupString)
    {
        if (lookupString == null)
            return getRoot(tree);

        try
        {
            return getChildren(tree.resolve(lookupString));
        }
        catch (UnresolvableException e)
        {
            return Collections.emptyList();
        }
    }

    private Collection<Location> getRoot(Tree tree)
    {
        return Collections.singleton(tree.getRoot());
    }

    private Collection<Location> getChildren(Location location)
    {
        // return all enabled children
        List<Location> treeChildren = new ArrayList<Location>();
        for (Location child : location.getChildren(LocationSort.PRESENTATION))
        {
            if (!child.find(TrendSource.class, Acceptors.enabledTrendSource()).isEmpty())
                treeChildren.add(child);

//            API 1.2.0 (WebCTRL 5.5+) optimization - not supported in API v1.1.+ or WebCTRL 5.2
//            if (child.has(TrendSource.class, Acceptors.enabledTrendSource()))
//                treeChildren.add(child);
        }
        return treeChildren;
    }

    private String getIconForType(LocationType type)
    {
        return TreeIcon.findIcon(type).getImageUrl();
    }

    private void getTreeData(String referencePath, DBAndSchemaSynchronizer synchronizer)
    {
        try
        {
            JSONObject next = new JSONObject();
            if (synchronizer.containsSource(referencePath))
            {
                DataStoreRetriever retriever = synchronizer.getRetrieverForTrendSource(referencePath);
                next.put("addClass", "selectedNode");
                next.put("selected", "true");
                next.put("url", retriever.getTableName());
            }
            else
            {
                next.put("addClass", "notSelected");
                next.put("selected", "false");
                next.put("target", "N/A");
                next.put("url", "N/A");
            }
        }
        catch (JSONException e)
        {
            ErrorHandler.handleError("TreeData JSON Write error", e);
        }
    }

    private JSONArray toJSON(Collection<Location> treeEntries, DBAndSchemaSynchronizer synchronizer)
            throws JSONException, DatabaseVersionMismatchException, UpgradeException, DatabaseException, UnresolvableException
    {
        JSONArray arrayData = new JSONArray();
        for (Location location : treeEntries)
        {
            JSONObject next = new JSONObject();
            String lookup = location.getPersistentLookupString(true);
            String displayName = location.getDisplayName();

            if (!location.hasParent())
                next.put("activate", true);

            next.put("title", displayName);
            next.put("key", lookup);

            if (location.getChildren().size() > 0)
            {
                next.put("hideCheckbox", true);
                next.put("isLazy", true);
                next.put("isFolder", true);
                next.put("url", "N/A");
            }
            else
            {
                next.put("hideCheckbox", false);
                next.put("isFolder", false);
                next.put("url", getTableName(lookup, displayName, synchronizer));
            }

            // Convert persstent lookup to reference name here because the synchronizer only maintains a list of
            // active trend sources via GQLPaths
            String referencePath = TrendSourcePathResolvers.getReferencePath(location, new StringBuilder()).toString();

            if (synchronizer.containsSource(referencePath))
                next.put("addClass", "selectedNode");
            else
                next.put("addClass", "null");

            next.put("nodeDisplayPath", location.getDisplayPath());
            next.put("nodeType", location.getType());

            next.put("icon", getIconForType(location.getType()));
            arrayData.put(next);
        }
        return arrayData;
    }

    private String getTableName(String lookup, String displayName, DBAndSchemaSynchronizer synchronizer)
    {
        // get table name if one exists if not, get it from the table name generator
        if (synchronizer.containsSource(lookup))
            return synchronizer.getRetrieverForTrendSource(lookup).getTableName();
        else
            return TrendTableNameGenerator.generateUniqueTableName(displayName, synchronizer.getSourceMappings().getTableNames());
    }

    public enum TreeIcon
    {
        SystemIcon(System, "system.gif"),
        AreaIcon(Area, "area.gif"),
        SiteIcon(Site, "site.gif"),
        NetworkIcon(Network, "network.gif"),
        DeviceIcon(Device, "hardware.gif"),
        DriverIcon(Driver, "dir.gif"),
        EquipmentIcon(Equipment, "equipment.gif"),
        MicroblockIcon(Microblock, "io_point.gif"),
        MicroblockComponentIcon(MicroblockComponent, "io_point.gif"),
        UnknownIcon(null, "unknown.gif");

        private static final String IMAGE_URL_BASE = "../../../_common/lvl5/skin/graphics/type/";

        private final LocationType locationType;
        private final String image;

        TreeIcon(LocationType locationType, String image)
        {
            this.locationType = locationType;
            this.image = image;
        }

        public String getImageUrl()
        {
            return IMAGE_URL_BASE + image;
        }

        public static TreeIcon findIcon(LocationType locationType)
        {
            for (TreeIcon icon : values())
            {
                if (icon.locationType == locationType)
                    return icon;
            }
            return UnknownIcon;
        }
    }
}