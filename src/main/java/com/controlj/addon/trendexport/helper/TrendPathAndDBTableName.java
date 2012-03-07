package com.controlj.addon.trendexport.helper;

import com.controlj.green.addonsupport.access.aspect.TrendSource;

public class TrendPathAndDBTableName
{
    private String trendSourceDisplayName;
    private String trendSourceDisplayPath;
    private String trendSourceLookupString;
    private String dbTableName;
    private TrendSource.Type trendType;
    private boolean isEnabled;

    public TrendPathAndDBTableName(String sourcePath, String displayName, String displayPath, String tableName, TrendSource.Type type, boolean enabled)
    {
        this.trendSourceLookupString = sourcePath;
        this.trendSourceDisplayName = displayName;
        this.trendSourceDisplayPath = displayPath;
        this.dbTableName = tableName;
        this.trendType = type;
        this.isEnabled = enabled;
    }

    public String getTrendSourceReferenceName()
    {
        return trendSourceLookupString;
    }

    public String getDbTableName()
    {
        return dbTableName;
    }

    public short getTrendType()
    {
        if (trendType == TrendSource.Type.Analog)
            return 1;
        else if (trendType == TrendSource.Type.Digital)
            return 2;
        //else if (trendType == TrendSource.Type.EquipmentColor)
            //return 3;
        else
            return 4; // 'Complex' should never show up
    }

    public String getDisplayName()
    {
        return trendSourceDisplayName;
    }

    public String getTrendSourceDisplayPath()
    {
        return trendSourceDisplayPath;
    }

    public boolean getIsEnabled()
    {
        return isEnabled;
    }

    public void setIsEnabled(boolean enabled)
    {
        this.isEnabled = enabled;
    }
}
