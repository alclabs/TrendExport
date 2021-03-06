package com.controlj.addon.trendexport.config;

// Holds sourceMappings for

import com.controlj.addon.trendexport.exceptions.SourceMappingNotFoundException;
import com.controlj.addon.trendexport.exceptions.TableNotInDatabaseException;
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

    public TrendPathAndDBTableName getTableNameObjectFromRefPath(String source) throws SourceMappingNotFoundException
    {
        for (TrendPathAndDBTableName obj : sourcesAndTableNames)
        {
            if (obj.getTrendSourceReferencePath().equalsIgnoreCase(source))
                return obj;
        }

        throw new SourceMappingNotFoundException("Source Not Found: " + source);
    }

    public Collection<TrendPathAndDBTableName> getSourcesAndTableNames()
    {
        return sourcesAndTableNames;
    }

    public boolean isEmpty()
    {
        return sourcesAndTableNames.isEmpty();
    }

    public void addSourceAndName(TrendPathAndDBTableName t)
    {
        sourcesAndTableNames.add(t);
    }

    public void removeSource(String source)
    {
        try
        {
            TrendPathAndDBTableName temp = getTableNameObjectFromRefPath(source);

            temp.setIsEnabled(false);
            sourcesAndTableNames.remove(temp);
        }
        catch (SourceMappingNotFoundException ignored)
        {
            // hmm, not there, must have already been removed!
        }
    }

    public Collection<String> getTableNames()
    {
        Collection<String> names = new HashSet<String>(sourcesAndTableNames.size());
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
            names.add(t.getDbTableName());

        return names;
    }

    public boolean containsSource(String source)
    {
        try
        {
            return getTableNameObjectFromRefPath(source) != null;
        }
        catch (SourceMappingNotFoundException e)
        {
            return false;
        }
    }

    public String getTableNameFromSource(String source) throws SourceMappingNotFoundException
    {
        return getTableNameObjectFromRefPath(source).getDbTableName();
    }

    public String getSourceFromTableName(String tableName) throws TableNotInDatabaseException
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getTrendSourceReferencePath();
        }

        throw new TableNotInDatabaseException("SourceMappings: Table " + tableName + " not in source mappings.");
    }

    public short getTypeFromTableName(String tableName)
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getTrendType();
        }

        return -2;
    }

    public String getDisplayNameFromTableName(String tableName) throws SourceMappingNotFoundException
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getDisplayName();
        }

        throw new SourceMappingNotFoundException("SourceMapping - displayNameFromTableName(" + tableName + ") not found in sourcemappings");
    }

    public boolean getIsEnabled(String tableName)
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.isEnabled();
        }

        return false;
    }

    public String getDisplayPathFromTableName(String tableName) throws SourceMappingNotFoundException
    {
        for (TrendPathAndDBTableName t : sourcesAndTableNames)
        {
            if (t.getDbTableName().equals(tableName))
                return t.getTrendSourceDisplayPath();
        }

        throw new SourceMappingNotFoundException("SourceMapping - displayPathFromTableName(" + tableName + ") not found in sourcemappings");
    }
}
