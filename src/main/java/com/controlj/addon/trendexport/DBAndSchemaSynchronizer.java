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
import com.controlj.addon.trendexport.helper.TrendPathAndDBTableName;
import com.controlj.addon.trendexport.helper.TrendSourceTypeAndPathResolver;
import com.controlj.addon.trendexport.helper.TrendTableNameGenerator;
import com.controlj.addon.trendexport.util.ErrorHandler;
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

    // auto-generated name - only used in Test -> use addSourceAndTableName below
    public void addSource(final String referencePath) throws Exception
    {
        // get trend source via persistent lookup string and get type within read action
        SystemConnection connection = DirectAccess.getDirectAccess().getRootSystemConnection();
        connection.runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction()
        {
            @Override
            public void execute(@NotNull SystemAccess systemAccess) throws Exception
            {
                Location loc = systemAccess.getTree(SystemTree.Geographic).resolve(referencePath);
                TrendSource trendSource = loc.getAspect(TrendSource.class);

                String tableName = TrendTableNameGenerator.generateUniqueTableName(loc.getDisplayName(), sourceMappings.getTableNames());
                if (tableName != null)
                    addSourceAndTableName(referencePath, loc.getDisplayName(), loc.getDisplayPath(), tableName, trendSource.getType());
            }
        });


    }

    public DataStoreRetriever getRetrieverForTrendSource(String nodeLookupString)
    {

        return new DataStoreRetriever(sourceMappings.getNameFromSource(nodeLookupString), database);
    }

    public SourceMappings getSourceMappings()
    {
        return sourceMappings;
    }

    public int sizeOfSourceMappings()
    {
        return sourceMappings.getTableNames().size();
    }

    // human-generated name - or passed name from another method
    public synchronized void addSourceAndTableName(String referencePath, String displayName, String displayPath, String tableName, TrendSource.Type type)
            throws DatabaseVersionMismatchException, UpgradeException, DatabaseException
    {
        if (sourceMappings.containsSource(referencePath))
            return; // todo: check that tableName matches

        sourceMappings.addSourceAndName(new TrendPathAndDBTableName(referencePath, displayName, displayPath, tableName, type, true));
        DynamicDatabase newDatabase = database.upgradeSchema(sourceMappings, true);
        database.close();
        newDatabase.connect(connectionInfo);
        database = newDatabase;
    }

    public void removeSource(String gqlReferencePath, boolean keepData) throws DatabaseVersionMismatchException, UpgradeException, DatabaseException
    {
        sourceMappings.removeSource(gqlReferencePath);
        database.upgradeSchema(sourceMappings, keepData);
    }

    public void create() throws DatabaseVersionMismatchException, DatabaseException
    {
        database.create(connectionInfo);
        sourceMappings = new SourceMappings();
    }

    public synchronized void connect() throws DatabaseVersionMismatchException, DatabaseException, UpgradeException
    {
        ErrorHandler.handleError("Attempt CONNECT ", new Throwable());

        if (isConnected)
            return;

        // implicit upgrade:
        // written because on initial call, schema only had metadata table but when asked to add the
        // data tables, it would not add tables to the schema even if they already existed in the db (since we only
        // load the metadata table)
        try
        {
            ErrorHandler.handleError("Attempt CONNECTION ", new Throwable());

            database.connect(connectionInfo);
            sourceMappings = readMappingsFromDatabase();

            ErrorHandler.handleError("Connection Successful", new Throwable());
        }
        catch (Exception e)
        {
            ErrorHandler.handleError("Error Connecting ", e);
            create();
        }
        database.close();

        database = new DynamicDatabase(sourceMappings);
        database.connect(connectionInfo);
        database.upgradeSchema(sourceMappings, true);
        isConnected = true;
        connections++;

        ErrorHandler.handleError("Number of connections: " + connections, new Throwable());

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
        Collection<TrendPathAndDBTableName> listOfAll = this.getAllTrends();
        SourceMappings mappings = new SourceMappings();

        for (TrendPathAndDBTableName table : listOfAll)
        {
            if (table.getIsEnabled()) // we only want enabled source mappings
                mappings.addSourceAndName(table);
        }

        return mappings;
    }

    public void insertTrendSamples(String source, TrendData trendData, int numberOfSamplesToSkip) throws Exception
    {
        String tableName = sourceMappings.getNameFromSource(source);
        database.insertDataIntoTrendTable(tableName, trendData, numberOfSamplesToSkip);
    }

    public boolean containsSource(String referencePath)
    {
        return sourceMappings.containsSource(referencePath);
    }

    public Collection<TrendPathAndDBTableName> getAllTrends() throws DatabaseException
    {
        return this.getMetaDataTableInfo();
    }
}
