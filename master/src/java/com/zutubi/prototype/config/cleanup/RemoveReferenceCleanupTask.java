package com.zutubi.prototype.config.cleanup;

import com.zutubi.prototype.type.record.MutableRecord;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.prototype.type.record.Record;
import com.zutubi.prototype.type.record.RecordManager;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Predicate;

import java.util.Arrays;

/**
 * A record cleanup task that removes a reference from a collection.
 */
public class RemoveReferenceCleanupTask extends RecordCleanupTaskSupport
{
    private RecordManager recordManager;

    public RemoveReferenceCleanupTask(String referencingPath, RecordManager recordManager)
    {
        super(referencingPath);
        this.recordManager = recordManager;
    }

    public void run()
    {
        String collectionPath = PathUtils.getParentPath(getAffectedPath());
        String itemName = PathUtils.getBaseName(getAffectedPath());
        int itemIndex = Integer.parseInt(itemName);
        String parentPath = PathUtils.getParentPath(collectionPath);
        String baseName = PathUtils.getBaseName(collectionPath);

        Record parentRecord = recordManager.select(parentPath);
        if (parentRecord != null)
        {
            MutableRecord newValues = parentRecord.copy(false);
            String[] references = (String[]) newValues.get(baseName);
            String[] newReferences = new String[references.length - 1];

            for(int srcIndex = 0, destIndex = 0; srcIndex < references.length; srcIndex++)
            {
                if(srcIndex != itemIndex)
                {
                    newReferences[destIndex++] = references[srcIndex];
                }
            }

            newValues.put(baseName, newReferences);
            recordManager.update(parentPath, newValues);
        }
    }
}
