package com.controlj.addon.trendexport.config;

// Holds sourceMappings for

import com.controlj.addon.trendexport.helper.TrendPathAndDBTableName;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SourceMappings
{
    private Set<TrendPathAndDBTableName> sourcesAndTableNames;

    public SourceMappings()
    {
        sourcesAndTableNames = new HashSet<TrendPathAndDBTableName>();
    }

    public SourceMappings(Set<TrendPathAndDBTableName> loadedPathsAndNames)
    {
        sourcesAndTableNames = loadedPathsAndNames;
    }

    public Collection<TrendPathAndDBTableName> getSourcesAndTableNames()
    {
        return sourcesAndTableNames;
    }

    public void addSourceAndName(TrendPathAndDBTableName t)
    {
        sourcesAndTableNames.add(t);
    }

    public void removeSource(String source)
    {
        TrendPathAndDBTableName temp = getTrendPathAndDBTableNameObject(source);

        if (temp != null)
        {
            temp.setIsEnabled(false);
            sourcesAndTableNames.remove(temp);
        }
    }

    public TrendPathAndDBTableName getTrendPathAndDBTableNameObject(String source)
    {
        for (TrendPathAndDBTableName obj : sourcesAndTableNames)
        {
            if (obj.getTrendSourceReferenceName().equalsIgnoreCase(source))
                return obj;
        }

        return null;
    }

    public Collection<String> getSources()
    {
        Collection<String> names = new HashSet<String>(sourcesAndTableNames.size());
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
            names.add(t.getTrendSourceReferenceName());

        return names;
    }

    public Collection<String> getTableNames()
    {
        Collection<String> names = new HashSet<String>(sourcesAndTableNames.size());
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
            names.add(t.getDbTableName());

        return names;
    }

    public Collection<String> getDisplayNames()
    {
        Collection<String> names = new HashSet<String>(sourcesAndTableNames.size());
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
            names.add(t.getDisplayName());

        return names;
    }

    public boolean containsSource(String source)
    {
        return getTrendPathAndDBTableNameObject(source) != null;
    }

    public String getNameFromSource(String source)
    {
        return getTrendPathAndDBTableNameObject(source).getDbTableName();
    }

    public String getSourceFromTableName(String tableName)
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getTrendSourceReferenceName();
        }

        return null;
    }

    public short getTypeFromTableName(String tableName)
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getTrendType();
        }

        return -1;
    }

    public String getDisplayNameFromTableName(String tableName)
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getDisplayName();
        }

        return null;
    }

    public void setIsEnabled(String source, boolean isEnabled)
    {
        getTrendPathAndDBTableNameObject(source).setIsEnabled(isEnabled);
    }

    public boolean getIsEnabled(String tableName)
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getIsEnabled();
        }

        return false;
    }

    public String getDisplayPathFromTableName(String tableName)
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getTrendSourceDisplayPath();
        }

        return null;
    }
}
