package com.controlj.addon.trendexport.Helper;

import java.util.Collection;

public class TrendTableNameGenerator
{
    private static int nameIndex = 0;
    private static final int maxNumber = 999999999;

    public static String generateUniqueTableName(String displayName, Collection<String> existingTableNames)
    {
        // fix beginning
        if (displayName.length() > 16)
            displayName = displayName.substring(0, 16);

        displayName = displayName.toLowerCase();

        /*int charsToRemove = 0;
        for (int i = 0; i < displayName.length() && !Character.isLetter(displayName.charAt(i)); ++i)
            charsToRemove = i;

        if (charsToRemove == displayName.length() || charsToRemove > 18)
            return generateTableName(existingTableNames);

        displayName = displayName.substring(charsToRemove);*/

        StringBuilder builder = new StringBuilder();
        for (char c : displayName.toCharArray())
        {
            if (Character.isLetterOrDigit(c) || c == '_')
                builder.append(c);
            else
                builder.append('_');
        }

        int tempNameIndex = 0;
        String tempTableName = builder.append("_").toString();
        while (existingTableNames.contains(tempTableName + ++tempNameIndex)) { }

        return tempTableName + tempNameIndex;
    }

    private static String generateTableName(Collection<String> existingTableNames)
    {
        // reset if we need to begin rolling to the other letters
        if (nameIndex == maxNumber)
            nameIndex = 0;

        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String name = letters.charAt(nameIndex / maxNumber) + "" + nameIndex;

        if (existingTableNames.contains(name))
        {
            nameIndex++;
            generateTableName(existingTableNames);
        }

        return name;
    }

    public static boolean isTableNameValid(String tableName)
    {
        // if length > 18
        if (tableName.length() > 18)
            return false;

        // if char[] at 0 is not a letter
        if (!Character.isLetter(tableName.charAt(0)))
            return false;

        // if char[] contains only A-Z, a-z, 0-9, and '_'
        for (int i = 1; i < tableName.length(); ++i)
        {
            if (!Character.isLetterOrDigit(tableName.charAt(i)) && tableName.charAt(i) != '_')
                return false;
        }

        return true;
    }
}
