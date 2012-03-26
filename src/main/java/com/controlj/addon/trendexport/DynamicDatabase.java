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

import com.controlj.addon.trendexport.config.SourceMappings;
import com.controlj.addon.trendexport.exceptions.SourceMappingNotFoundException;
import com.controlj.addon.trendexport.exceptions.TableNotInDatabaseException;
import com.controlj.addon.trendexport.helper.TrendDataProcessor;
import com.controlj.addon.trendexport.tables.MetaDataTable;
import com.controlj.addon.trendexport.tables.TrendDataTable;
import com.controlj.green.addonsupport.access.TrendException;
import com.controlj.green.addonsupport.access.trend.TrendData;
import com.controlj.green.addonsupport.xdatabase.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DynamicDatabase extends Database
{
    private static final String dbName = "TrendExport";
    private static final int currentVersion = 1;  // needs to be updated when database schema is changed to handle schema upgrades

    public final MetaDataTable metaDataTable;
    private final List<TrendDataTable> dataTables;

    public DynamicDatabase()
    {
        super(dbName, currentVersion);
        metaDataTable = new MetaDataTable(schema);
        dataTables = new ArrayList<TrendDataTable>();
    }

    public DynamicDatabase(SourceMappings sourceMappings) throws DatabaseException
    {
        super(dbName, currentVersion);
        metaDataTable = new MetaDataTable(schema);
        dataTables = new ArrayList<TrendDataTable>();

        for (String tableName : sourceMappings.getTableNames())
            dataTables.add(new TrendDataTable(schema, tableName, sourceMappings.getTypeFromTableName(tableName)));
    }

    public DynamicDatabase upgradeSchema(final SourceMappings newConfig, final boolean keepData)
            throws DatabaseException
    {
        final DynamicDatabase newDatabase = new DynamicDatabase(newConfig);

        try
        {
            schema.runUpgrade(newDatabase.schema, new UpgradeTask()
            {
                @Override
                public void execute(@NotNull DatabaseUpgradeAccess dbAccess) throws DatabaseException, SourceMappingNotFoundException, TableNotInDatabaseException
                {
                    // add new tables
                    for (TrendDataTable newDataTable : newDatabase.dataTables)
                    {
                        String tableName = newDataTable.getTableSchema().getName();
                        // set enabled
    //                    if (keepData != newConfig.getIsEnabled(tableName))
    //                        metaDataTable.setEnabled(DynamicDatabase.this, tableName, keepData);

                        if (!dataTables.contains(newDataTable))
                        {
                            String displayName = newConfig.getDisplayNameFromTableName(tableName);
                            String displayPath = newConfig.getDisplayPathFromTableName(tableName);
                            String source = newConfig.getSourceFromTableName(tableName);
                            short type = newConfig.getTypeFromTableName(tableName);
                            boolean enabled = newConfig.getIsEnabled(tableName);

                            if (source == null)
                                throw new DatabaseException("Source does not exist for table: " + tableName);

                            dbAccess.addTable(new TrendDataTable(schema, tableName, type).getTableSchema());
                            metaDataTable.insertRow(DynamicDatabase.this, source, displayName, displayPath, tableName, type, enabled);
                        }
                    }

                    // drop old tables no longer needed
                    for (TrendDataTable oldDataTable : dataTables)
                    {

                        if (!newDatabase.dataTables.contains(oldDataTable))
                        {
                            // get the source to delete the row in the metadata table
                            String tableName = oldDataTable.getTableSchema().getName();
                            Query q = buildSelect(DynamicDatabase.this.metaDataTable.referencePath).
                                    where(DynamicDatabase.this.metaDataTable.tableName.eq(tableName));

                            Result r = dbAccess.execute(q);
                            String source = null;

                            if (r.next())
                                source = r.get(DynamicDatabase.this.metaDataTable.referencePath);

                            if (source != null)
                            {
                                metaDataTable.setEnabled(DynamicDatabase.this, tableName, false);

                                if (!keepData)
                                {
                                    dbAccess.dropTable(oldDataTable.getTableSchema());
                                    metaDataTable.deleteRow(DynamicDatabase.this, source);
                                }

                            }
                        }
                    }
                }
            });
        }
        catch (UpgradeException e)
        {
            throw new DatabaseException("Error adding/removing tables to match new schema", e);
        }

        return newDatabase;
    }

    public void setEnabledOrDisabled(String referencePath, boolean enabled) throws DatabaseException
    {
        metaDataTable.setEnabledByReferenceName(this, referencePath, enabled);
    }

    public void insertDataIntoTrendTable(String tableName, TrendData<?> data, int numberOfSamplesToSkip)
            throws TableNotInDatabaseException, TrendException
    {
        TrendDataTable table = getDataTableByTableName(tableName);
        data.process(new TrendDataProcessor(this, table, numberOfSamplesToSkip));
    }

    public TrendDataTable getDataTableByTableName(String name) throws TableNotInDatabaseException
    {
        for (TrendDataTable table : this.dataTables)
        {
            if (table.getTableName().equals(name))
                return table;
        }

        throw new TableNotInDatabaseException();
    }
}
