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

package com.controlj.addon.trendexport;

// maintains correlation between source mappings and a dynamic schema, also handles DB upgrade tasks
// adding, removing sources is handled here encapsulating the synchronization between the two objects

import com.controlj.addon.trendexport.config.SourceMappings;
import com.controlj.addon.trendexport.exceptions.SourceMappingNotFoundException;
import com.controlj.addon.trendexport.exceptions.TableNotInDatabaseException;
import com.controlj.addon.trendexport.helper.TrendPathAndDBTableName;
import com.controlj.addon.trendexport.helper.TrendSourceTypeAndPathResolver;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.aspect.TrendSource;
import com.controlj.green.addonsupport.access.trend.TrendData;
import com.controlj.green.addonsupport.xdatabase.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class DBAndSchemaSynchronizer
{
    private static final AtomicReference<DBAndSchemaSynchronizer> me = new AtomicReference<DBAndSchemaSynchronizer>();

    private DynamicDatabase database;
    private SourceMappings sourceMappings;
    private DatabaseConnectionInfo connectionInfo;
    private boolean isConnected;
    private int connections;

    private DBAndSchemaSynchronizer(DatabaseConnectionInfo info)
    {
        database = new DynamicDatabase();
        connectionInfo = info;
        connections = 0;
    }

    public static synchronized DBAndSchemaSynchronizer getSynchronizer(DatabaseConnectionInfo info)
    {
        DBAndSchemaSynchronizer synchronizer = me.get();
        if (synchronizer == null || !isSameConnection(synchronizer.connectionInfo, info))
        {
            synchronizer = new DBAndSchemaSynchronizer(info);
            me.set(synchronizer);
        }
        return synchronizer;
    }

    private static boolean isSameConnection(DatabaseConnectionInfo info1, DatabaseConnectionInfo info2)
    {
        String host = info1.getHost();
        String instance = info1.getInstance();
        String user = info1.getUser();
        String passwd = info1.getPasswd();
        return info1.getType() == info2.getType() &&
                (host == null ? info2.getHost() == null : host.equals(info2.getHost())) &&
                (instance == null ? info2.getInstance() == null : instance.equals(info2.getInstance())) &&
                info1.getPort() == info2.getPort() &&
                (user == null ? info2.getUser() == null : user.equals(info2.getUser())) &&
                (passwd == null ? info2.getPasswd() == null : passwd.equals(info2.getPasswd()));
    }

    public DataStoreRetriever getRetrieverForTrendSource(String nodeLookupString) throws SourceMappingNotFoundException, TableNotInDatabaseException
    {
        return new DataStoreRetriever(sourceMappings.getTableNameFromSource(nodeLookupString), database);
    }

    public SourceMappings getSourceMappings()
    {
        return sourceMappings;
    }

    public int sizeOfSourceMappings()
    {
        return sourceMappings.getTableNames().size();
    }

    public boolean isSourceEnabled(String referenceName) throws SourceMappingNotFoundException
    {
        return sourceMappings.getTableNameObjectFromRefPath(referenceName).getIsEnabled();
    }

    // human-generated name - or passed name from another method
    public synchronized void addSourceAndTableName(String referencePath, String displayName, String displayPath, String tableName, TrendSource.Type type)
            throws DatabaseException
    {
        if (referencePath.length() > 2000)
            throw new DatabaseException("Reference path exceeds 2000 characters");

        if (sourceMappings.containsSource(referencePath) && sourceMappings.getTableNames().contains(tableName))
            return;

        if (displayName.length() > 100)
            displayName = displayName.substring(0, 100);

        if (displayPath.length() > 2000)
            displayPath = displayPath.substring(0, 2000);

        sourceMappings.addSourceAndName(new TrendPathAndDBTableName(referencePath, displayName, displayPath, tableName, type, true));
        DynamicDatabase newDatabase = database.upgradeSchema(sourceMappings, true);
        database.close();

        try
        {
            newDatabase.connect(connectionInfo);
        }
        catch (DatabaseVersionMismatchException e)
        {
            throw new DatabaseException("After adding table, database is wrong version!", e);
        }
        database = newDatabase;
    }

    public void removeSource(String gqlReferencePath, boolean keepData)
            throws DatabaseException
    {
        sourceMappings.removeSource(gqlReferencePath);
        database.upgradeSchema(sourceMappings, keepData);
    }

    public void enableOrDisableSource(final String lookupString, final boolean enable) throws DatabaseException, SystemException, ActionExecutionException
    {
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
        {
            @Override
            public void execute(@NotNull SystemAccess systemAccess) throws Exception
            {
                String gqlReferencePath = lookupString;
                if (lookupString.contains("DBID:"))
                {
                    Location loc = systemAccess.getTree(SystemTree.Geographic).resolve(lookupString);
                    gqlReferencePath = TrendSourceTypeAndPathResolver.getReferencePath(loc);
                }

                if (sourceMappings.containsSource(gqlReferencePath))
                {
                    database.setEnabledOrDisabled(gqlReferencePath, enable);
                    sourceMappings = readMappingsFromDatabase();
                }
            }
        });
    }

    public void create() throws DatabaseVersionMismatchException, DatabaseException
    {
        database.create(connectionInfo);
        sourceMappings = new SourceMappings();
    }

    public synchronized void connect() throws DatabaseException
    {
        connections++;
        if (isConnected)
            return;

        // implicit upgrade:
        // written because on initial call, schema only had metadata table but when asked to add the
        // data tables, it would not add tables to the schema even if they already existed in the db (since we only
        // load the metadata table)
        try
        {
            try
            {
                database.connect(connectionInfo);
                sourceMappings = readMappingsFromDatabase();
            }
            catch (DatabaseException e)
            {
                // either doesn't exist or need to be upgraded
                create();
                if (connections > 1)
                    throw new DatabaseConnectionException("Failed to connect to database. Please check your settings");
            }
            catch (Exception e)
            {
                create();
            }

            database.close();

            database = new DynamicDatabase(sourceMappings);
            database.connect(connectionInfo);
            database.upgradeSchema(sourceMappings, true);
            isConnected = true;
        }
        catch (Exception e)
        {
            throw new DatabaseException("Unable to upgrade the database", e);
        }
    }

    public synchronized void disconnect()
    {
//        ErrorHandler.handleError("Attempt DISCONNECT: " + connections, new Throwable());

        connections--;
        if (connections > 0)
            return;

//        ErrorHandler.handleError("Number of connections: " + connections, new Throwable());
        database.close();
        database = new DynamicDatabase();
        isConnected = false;
    }

    private Collection<TrendPathAndDBTableName> getMetaDataTableInfo() throws DatabaseException
    {
        return database.runQuery(new QueryTask<Collection<TrendPathAndDBTableName>>()
        {
            @Override
            public Collection<TrendPathAndDBTableName> execute(@NotNull DatabaseReadAccess databaseReadAccess) throws DatabaseException
            {
                Result result =
                        databaseReadAccess.execute(
                                database.buildSelect(
                                        database.metaDataTable.id,
                                        database.metaDataTable.referencePath,
                                        database.metaDataTable.displayName,
                                        database.metaDataTable.displayPath,
                                        database.metaDataTable.tableName,
                                        database.metaDataTable.sourceType,
                                        database.metaDataTable.isTrendEnabled).orderBy(database.metaDataTable.id.asc())
                        );

                Collection<TrendPathAndDBTableName> listOfEverything = new ArrayList<TrendPathAndDBTableName>();

                while (result.next())
                {
                    String sourcePath = result.get(database.metaDataTable.referencePath);
                    String displayName = result.get(database.metaDataTable.displayName);
                    String displayPath = result.get(database.metaDataTable.displayPath);
                    String tableName = result.get(database.metaDataTable.tableName);
                    Short type = result.get(database.metaDataTable.sourceType);
                    boolean isEnabled = result.get(database.metaDataTable.isTrendEnabled);

                    if (type == null)
                        throw new RuntimeException("Type not found");

                    TrendSource.Type trendSourceType = TrendSourceTypeAndPathResolver.getTrendSourceType(type);

                    listOfEverything.add(new TrendPathAndDBTableName(sourcePath, displayName, displayPath, tableName, trendSourceType, isEnabled));
                }

                return listOfEverything;
            }
        });
    }

    private SourceMappings readMappingsFromDatabase() throws DatabaseException
    {
        Collection<TrendPathAndDBTableName> listOfAll = this.getAllSources();
        SourceMappings mappings = new SourceMappings();

        for (TrendPathAndDBTableName table : listOfAll)
        {
//            if (table.getIsEnabled()) // we only want enabled source mappings
            mappings.addSourceAndName(table);
        }

        return mappings;
    }

    public SourceMappings getEnabledSources() throws DatabaseException
    {
        Collection<TrendPathAndDBTableName> listOfAll = this.getAllSources();
        SourceMappings mappings = new SourceMappings();

        for (TrendPathAndDBTableName table : listOfAll)
        {
            if (table.getIsEnabled())
                mappings.addSourceAndName(table);
        }

        return mappings;
    }

    public void insertTrendSamples(String source, TrendData trendData, int numberOfSamplesToSkip)
            throws SourceMappingNotFoundException, TableNotInDatabaseException, TrendException
    {
        String tableName = sourceMappings.getTableNameFromSource(source);
        database.insertDataIntoTrendTable(tableName, trendData, numberOfSamplesToSkip);
    }

    public boolean containsSource(String referencePath)
    {
        return sourceMappings.containsSource(referencePath);
    }

    public Collection<TrendPathAndDBTableName> getAllSources() throws DatabaseException
    {
        return this.getMetaDataTableInfo();
    }

    public Collection<String> getReferencePaths(Collection<TrendPathAndDBTableName> trendPathAndDBTableNames)
    {
        Collection<String> referencePaths = new ArrayList<String>();
        for (TrendPathAndDBTableName tableName : trendPathAndDBTableNames)
            referencePaths.add(tableName.getTrendSourceReferencePath());

        return referencePaths;
    }
}
