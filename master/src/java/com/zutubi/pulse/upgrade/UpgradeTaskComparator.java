package com.zutubi.pulse.upgrade;

import java.util.Comparator;

/**
 * <class-comment/>
 */
public class UpgradeTaskComparator implements Comparator<UpgradeTask>
{
    public int compare(UpgradeTask a, UpgradeTask b)
    {
        return 0;//a.getBuildNumber() - b.getBuildNumber();
    }
}
