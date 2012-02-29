package com.controlj.addon.trendexport.tables;

import com.controlj.green.addonsupport.xdatabase.*;
import com.controlj.green.addonsupport.xdatabase.column.BoolColumn;
import com.controlj.green.addonsupport.xdatabase.column.IntColumn;
import com.controlj.green.addonsupport.xdatabase.column.ShortColumn;
import com.controlj.green.addonsupport.xdatabase.column.StringColumn;

public class MetaDataTable
{
    private final TableSchema tableSchema;
    public final IntColumn id;
    public final StringColumn referencePath;
    public final StringColumn tableName;
    public final StringColumn displayName;
    public final StringColumn displayPath;
    public final ShortColumn sourceType;
    public final BoolColumn isTrendEnabled;

    public MetaDataTable(DatabaseSchema db)
    {
        this.tableSchema = db.addTable("Metadata");

        id = tableSchema.addIntColumn("ID");
        referencePath = tableSchema.addStringColumn("ReferencePath", 600);
        displayName = tableSchema.addStringColumn("DisplayName", 30);
        displayPath = tableSchema.addStringColumn("DisplayPath", 600);
        tableName = tableSchema.addStringColumn("TableName", 18);
        sourceType = tableSchema.addShortColumn("SourceType");
        isTrendEnabled = tableSchema.addBoolColumn("IsEnabled");
        tableSchema.setAutoGenerate(id);
        tableSchema.setPrimaryKey(id);
    }

    public TableSchema getTableSchema()
    {
        return tableSchema;
    }

    public void insertRow(Database db, String source, String displayName, String displayPath, String tableName, short type, boolean enabled) throws DatabaseException
    {
        Insert insert = db.buildInsert(this.referencePath.set(source),
                                       this.displayName.set(displayName),
                                       this.displayPath.set(displayPath),
                                       this.tableName.set(tableName),
                                       this.sourceType.set(type),
                                       this.isTrendEnabled.set(enabled));
        insert.execute(db);
    }

    public void deleteRow(Database db, String source) throws DatabaseException
    {
        // needs the trenddata table schema... :/
        Update delete = db.buildDelete(this.tableSchema).where(this.referencePath.eq(source));
        delete.execute(db);
    }

    public void setEnabled(Database db, String tableName, boolean keepData) throws DatabaseException
    {
          Update update = db.buildUpdate(this.isTrendEnabled.set(keepData)).where(this.tableName.eq(tableName));
          update.execute(db);
    }
}
