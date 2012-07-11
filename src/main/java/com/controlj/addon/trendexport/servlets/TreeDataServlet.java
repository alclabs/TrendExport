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

import com.controlj.addon.trendexport.DBAndSchemaSynchronizer;
import com.controlj.addon.trendexport.config.ConfigManager;
import com.controlj.addon.trendexport.config.ConfigManagerLoader;
import com.controlj.addon.trendexport.exceptions.SourceMappingNotFoundException;
import com.controlj.addon.trendexport.exceptions.TableNotInDatabaseException;
import com.controlj.addon.trendexport.helper.TrendSourceTypeAndPathResolver;
import com.controlj.addon.trendexport.helper.TrendTableNameGenerator;
import com.controlj.addon.trendexport.util.ErrorHandler;
import com.controlj.green.addonsupport.InvalidConnectionRequestException;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.AnalogTrendSource;
import com.controlj.green.addonsupport.access.aspect.DigitalTrendSource;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
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
    private static final AspectAcceptor<TrendSource> standardAcceptor = new AspectAcceptor<TrendSource>()
    {
        @Override
        public boolean accept(@NotNull TrendSource aspect)
        {
            return (aspect.isEnabled() && aspect.isHistorianEnabled() &&
                   (aspect instanceof AnalogTrendSource || aspect instanceof DigitalTrendSource));
        }
    };

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setContentType("application/json");
        try
        {
            final ConfigManager manager = new ConfigManagerLoader().loadConnectionInfoFromDataStore();
            final String key = req.getParameter("key");
            final String mode = req.getParameter("mode");

            SystemConnection connection = DirectAccess.getDirectAccess().getUserSystemConnection(req);
            JSONArray treeArray = connection.runReadAction(new ReadActionResult<JSONArray>()
            {
                @Override
                public JSONArray execute(@NotNull SystemAccess access) throws JSONException, IOException
                {
                    DBAndSchemaSynchronizer synchronizer = null;
                    JSONArray jsonArray = new JSONArray();

                    try
                    {
                        synchronizer = DBAndSchemaSynchronizer.getSynchronizer(manager.getCurrentConnectionInfo());
                        synchronizer.connect();

                        Tree geoTree = access.getTree(SystemTree.Geographic);

                        if (key == null)
                        {
                            Collection<Location> treeChildren = getTreeFromNodeKey(geoTree, key);
                            jsonArray = toJSON(treeChildren, synchronizer);
                        }
                        else if (req.getParameter("mode").contains("lazyTree") || req.getParameter("mode").contains("data"))
                        {
                            Collection<Location> treeChildren = getTreeFromNodeKey(geoTree, key);
                            jsonArray = toJSON(treeChildren, synchronizer);
                        }
                    }
                    catch (IOException e)
                    {
                        ErrorHandler.handleError("TreeData IOException", e);
                        resp.sendError(500, "Configuration was not able to load.");

                    }
//                    catch (UnresolvableException e) // todo - why does the user care? likely doesn't
//                    {
//                        ErrorHandler.handleError("TreeData UnresolvableException", e);
//                        resp.sendError(500, "The source could not be resolved.");
//                    }
                    catch (DatabaseException e)
                    {
                        ErrorHandler.handleError("TreeData IOException", e);
                        resp.sendError(500, "The database has encountered an error.");
                    }
                    catch (Exception e)
                    {
                        ErrorHandler.handleError("TreeData - Unable to connect to data synchronizer", e);
                    }
                    finally
                    {
                        if (synchronizer != null)
                            synchronizer.disconnect();
                    }

                    return jsonArray;
                }
            });

            assert treeArray != null;
            if (key == null || !mode.contains("data"))
                treeArray.write(resp.getWriter());
        }
        catch (SystemException e)
        {
            // An error occurred. This may be temporary. Please refresh the page
            resp.sendError(500, "A System error occurred. Ensure that the system is running before continuing.");
            ErrorHandler.handleError("TreeData System Error - ", e);
        }
        catch (InvalidConnectionRequestException e)
        {
            resp.sendError(500, "Invalid login. Refresh the page to login.");
            ErrorHandler.handleError("TreeData System Error - ", e);
        }
        catch (Exception e)
        {
            // error occurred while handling the tree
            ErrorHandler.handleError("TreeData Error - ", e);
        }
    }

    private Collection<Location> getTreeFromNodeKey(Tree tree, String lookupString)
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
        List<Location> treeChildren = new ArrayList<Location>();
        Collection<Location> locChildren = location.getChildren(LocationSort.PRESENTATION);
        for (Location child : locChildren)
        {
            if (quickFind(child, standardAcceptor))
                treeChildren.add(child);

//            API 1.2.x (WebCTRL 5.5+) optimization - not supported in API v1.1.+ or WebCTRL 5.2
//            if (child.has(TrendSource.class, Acceptors.enabledTrendSource()))
//                treeChildren.add(child);
        }

        return treeChildren;
    }

    private boolean quickFind(Location location, final AspectAcceptor<TrendSource> acceptor)
    {
        try
        {
            location.find(TrendSource.class, new AspectAcceptor<TrendSource>()
            {
                @Override
                public boolean accept(@NotNull TrendSource aspect)
                {
                   if (acceptor.accept(aspect))
                      throw new RuntimeException();
                   return false;
                }
            });

            return false;
        }
        catch (RuntimeException e)
        {
            return true;
        }
    }

    private boolean hasEnabledTrendSource(Location location)
    {
        try
        {
            TrendSource aspect = location.getAspect(TrendSource.class);
            return standardAcceptor.accept(aspect);
        }
        catch (NoSuchAspectException e)
        {
            return false;
        }
    }

    private boolean childHasEnabledTrendSource(Location location)
    {
        for (Location child : location.getChildren())
        {
            if (quickFind(child, standardAcceptor))
                return true;
        }
        return false;
    }

    private String getIconForType(LocationType type)
    {
        return TreeIcon.findIcon(type).getImageUrl();
    }

    private JSONArray toJSON(Collection<Location> treeEntries, DBAndSchemaSynchronizer synchronizer)
            throws JSONException, DatabaseVersionMismatchException, UpgradeException, DatabaseException, UnresolvableException, TableNotInDatabaseException
    {
        JSONArray arrayData = new JSONArray();
        for (Location location : treeEntries)
        {
            try
            {
                JSONObject next = new JSONObject();
                String lookup = location.getPersistentLookupString(true);
                String displayName = location.getDisplayName();

                if (!location.hasParent())
                    next.put("activate", true);

                next.put("title", displayName);
                next.put("key", lookup);

                if (hasEnabledTrendSource(location))
                {
                    boolean childTrendChildren = childHasEnabledTrendSource(location);
                    next.put("isLazy", childTrendChildren);
                    next.put("isFolder", childTrendChildren);
                    next.put("isSource", true);
                    next.put("url", getTableName(location, displayName, synchronizer));
                }
                else
                {
                    next.put("isLazy", true);
                    next.put("isFolder", true);
                    next.put("isSource", false);
                }

                // Convert persistent lookup to reference name here because the synchronizer only maintains a list of
                // active trend sources via GQLPaths
                String referencePath = TrendSourceTypeAndPathResolver.getReferencePath(location);

                if (synchronizer.containsSource(referencePath) && synchronizer.isSourceEnabled(referencePath))
                    next.put("addClass", "selectedNode");
                else if (synchronizer.containsSource(referencePath) && !synchronizer.isSourceEnabled(referencePath))
                    next.put("addClass", "disabledNode");
                else
                    next.put("addClass", "null");

                next.put("nodeDisplayPath", location.getDisplayPath());
                next.put("nodeType", location.getType());

                next.put("icon", getIconForType(location.getType()));
                arrayData.put(next);
            }
            catch (SourceMappingNotFoundException ignored)
            {
                // ignored because we're loading lazy data
            }
        }

        return arrayData;
    }

    private String getTableName(Location location, String displayName, DBAndSchemaSynchronizer synchronizer)
            throws UnresolvableException, TableNotInDatabaseException
    {
        // get table name if one exists if not, get it from the table name generator

        String referencePath = TrendSourceTypeAndPathResolver.getReferencePath(location);
        try
        {
            if (synchronizer.containsSource(referencePath))
                return synchronizer.getRetrieverForTrendSource(referencePath).getTableName();
            else
                return TrendTableNameGenerator.generateUniqueTableName(displayName, synchronizer.getSourceMappings().getTableNames());
        }
        catch (SourceMappingNotFoundException e)
        {
            return TrendTableNameGenerator.generateUniqueTableName(displayName, synchronizer.getSourceMappings().getTableNames());
        }
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