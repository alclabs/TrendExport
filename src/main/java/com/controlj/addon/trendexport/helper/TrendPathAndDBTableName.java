package com.controlj.addon.trendexport.helper;

import com.controlj.green.addonsupport.access.aspect.TrendSource;

public class TrendPathAndDBTableName
{
    private String trendSourceDisplayName;
    private String trendSourceDisplayPath;
    private String trendSourceReferencePath;
    private String dbTableName;
    private TrendSource.Type trendType;
    private boolean isEnabled;

    public TrendPathAndDBTableName(String referencePath, String displayName, String displayPath, String tableName, TrendSource.Type type, boolean enabled)
    {
        this.trendSourceReferencePath = referencePath;
        this.trendSourceDisplayName = displayName;
        this.trendSourceDisplayPath = displayPath;
        this.dbTableName = tableName;
        this.trendType = type;
        this.isEnabled = enabled;
    }

    public String getTrendSourceReferencePath()
    {
        return trendSourceReferencePath;
    }

    public String getDbTableName()
    {
        return dbTableName;
    }

    public short getTrendType()
    {
        return TrendSourceTypeAndPathResolver.getTrendSourceType(this.trendType);
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

    @Override public String toString()
    {
        return "TrendPathAndDBTableName{" +
                "trendSourceDisplayName='" + trendSourceDisplayName + '\'' +
                ", trendSourceDisplayPath='" + trendSourceDisplayPath + '\'' +
                ", trendSourceReferencePath='" + trendSourceReferencePath + '\'' +
                ", dbTableName='" + dbTableName + '\'' +
                ", trendType=" + trendType +
                ", isEnabled=" + isEnabled +
                '}';
    }
}
