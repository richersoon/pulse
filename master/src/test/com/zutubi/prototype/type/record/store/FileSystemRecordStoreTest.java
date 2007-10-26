package com.zutubi.prototype.type.record.store;

import com.zutubi.prototype.transaction.TransactionManager;
import com.zutubi.prototype.transaction.UserTransaction;
import com.zutubi.prototype.transaction.Transaction;
import com.zutubi.prototype.transaction.TransactionStatus;
import com.zutubi.prototype.type.record.DefaultRecordSerialiser;
import com.zutubi.prototype.type.record.MutableRecord;
import com.zutubi.prototype.type.record.MutableRecordImpl;
import com.zutubi.prototype.type.record.Record;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 *
 *
 */
public class FileSystemRecordStoreTest extends PulseTestCase
{
    private FileSystemRecordStore recordStore = null;
    private File persistentDirectory = null;
    private TransactionManager transactionManager;
    private static final Random RAND = new Random(System.currentTimeMillis());

    protected void setUp() throws Exception
    {
        super.setUp();

        persistentDirectory = FileSystemUtils.createTempDir();
        transactionManager = new TransactionManager();

        restartRecordStore();
    }

    private void restartRecordStore() throws Exception
    {
        recordStore = new FileSystemRecordStore();
        recordStore.setTransactionManager(transactionManager);
        recordStore.setPersistenceDirectory(persistentDirectory);
        recordStore.init();
    }

    protected void tearDown() throws Exception
    {
        removeDirectory(persistentDirectory);
        transactionManager = null;
        recordStore = null;

        super.tearDown();
    }

    //---( a set of sanity checks of the basic functions. )---

    public void testInsert()
    {
        Record sample = createSampleRecord();
        recordStore.insert("sample", sample);

        Record stored = (Record) recordStore.select().get("sample");
        assertEquals(sample.get("a"), stored.get("a"));
    }

    public void testInsertPersistence() throws Exception
    {
        Record sample = createSampleRecord();
        recordStore.insert("sample", sample);

        restartRecordStore();

        Record stored = (Record) recordStore.select().get("sample");
        assertEquals(sample.get("a"), stored.get("a"));
    }

    public void testUpdate()
    {
        MutableRecord sample = createSampleRecord();
        recordStore.insert("sample", sample);

        sample.put("c", "c");

        // ensure that updating the original sample does not update the internal record store.
        Record stored = (Record) recordStore.select().get("sample");
        assertNull(stored.get("c"));

        // now update the record store.
        recordStore.update("sample", sample);
        stored = (Record) recordStore.select().get("sample");
        assertEquals(sample.get("c"), stored.get("c"));
    }

    public void testUpdatePersistence() throws Exception
    {
        MutableRecord sample = createSampleRecord();
        recordStore.insert("sample", sample);
        recordStore.update("sample", sample);
        
        restartRecordStore();

        Record stored = (Record) recordStore.select().get("sample");
        assertEquals(sample.get("c"), stored.get("c"));
    }

    public void testDelete()
    {
        Record sample = createSampleRecord();
        recordStore.insert("sample", sample);

        recordStore.delete("sample");
        assertNull(recordStore.select().get("sample"));
    }

    public void testDeletePersistence() throws Exception
    {
        Record sample = createSampleRecord();
        recordStore.insert("sample", sample);
        recordStore.delete("sample");

        restartRecordStore();

        assertNull(recordStore.select().get("sample"));
    }

    //---( check that the index files are correctly generated. )---

    public void testPersistentFiles()
    {
        Record sample = createSampleRecord();
        recordStore.insert("sample", sample);

        assertTrue(new File(persistentDirectory, "index").isFile());
        assertFalse(new File(persistentDirectory, "index.new").isFile());
        assertFalse(new File(persistentDirectory, "index.backup").isFile());

        // check that the journal entry is written to disk
        assertTrue(new File(persistentDirectory, "1").exists());

        assertFalse(new File(persistentDirectory, "2").exists());
        recordStore.update("sample", sample);
        assertTrue(new File(persistentDirectory, "2").exists());

        assertFalse(new File(persistentDirectory, "3").exists());
        recordStore.delete("sample");
        assertTrue(new File(persistentDirectory, "3").exists());
    }

    public void testCompaction() throws Exception
    {
        File snapshot = new File(persistentDirectory, "snapshot");
        File snapshotId = new File(snapshot, "snapshot_id.txt");

        assertFalse(snapshot.exists());

        Record sample = createSampleRecord();

        // insert generates one journal entry.
        recordStore.insert("sample", sample);
        assertFalse(snapshot.exists());
        assertTrue(new File(persistentDirectory, "1").exists());

        // compaction removes this journal entry.
        recordStore.compactNow();
        assertTrue(snapshot.exists());
        assertEquals("1", IOUtils.fileToString(snapshotId));
        assertFalse(new File(persistentDirectory, "1").exists());

        // ensure that the journal entry id is correct
        assertFalse(new File(persistentDirectory, "2").exists());
        recordStore.update("sample", sample);
        assertTrue(new File(persistentDirectory, "2").exists());

        recordStore.compactNow();
        assertEquals("2", IOUtils.fileToString(snapshotId));
        assertFalse(new File(persistentDirectory, "2").exists());

        // check that the snapshot is as expected.
        DefaultRecordSerialiser serialiser = new DefaultRecordSerialiser(snapshot);
        Record snapshotRecord = serialiser.deserialise("sample");
        assertEquals(sample, snapshotRecord);

        // check that the file system is cleaned up.
        assertEquals(3, persistentDirectory.list().length);
    }

    public void testCompactionOnRestart() throws Exception
    {
        File snapshot = new File(persistentDirectory, "snapshot");

        Record sample = createSampleRecord();

        // insert generates one journal entry.
        recordStore.insert("sample", sample);
        recordStore.update("sample", sample);
        recordStore.update("sample", sample);

        restartRecordStore();
        assertTrue(snapshot.exists());
        assertTrue(new File(snapshot, "sample").exists());
        assertEquals("3", IOUtils.fileToString(new File(snapshot, "snapshot_id.txt")));

        // restart should cleanup unused journal files.
    }

    public void testMutlipleRestartsAndTxns() throws Exception
    {
        for (int i = 0; i < 10; i++)
        {
            recordStore.insert("path_" + i, createSampleRecord());
            restartRecordStore();
        }
    }

    // Testing some of the boundry conditions.
    public void testRestartOnPersistenceDirectoryWithNoData() throws Exception
    {
        Record base = recordStore.select();
        assertNotNull(base);
        assertEquals(0, base.keySet().size());

        restartRecordStore();

        base = recordStore.select();
        assertNotNull(base);
        assertEquals(0, base.keySet().size());
    }

    public void testCompactionDuringCommitTransaction() throws Exception
    {
        File snapshot = new File(persistentDirectory, "snapshot");

        Record sample = createSampleRecord();

        // insert generates one journal entry.
        recordStore.insert("sample", sample);

        UserTransaction txn = new UserTransaction(transactionManager);
        txn.begin();

        MutableRecord anotherSample = createSampleRecord();
        anotherSample.put("d", "d");
        recordStore.insert("another", anotherSample);

        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                try
                {
                    recordStore.compactNow();
                }
                catch (Exception e)
                {
                    fail("Exception thrown during test: " + e.getClass().getName() + " " + e.getMessage());
                }
            }
        });

        recordStore.update("sample", anotherSample);
        txn.commit();

        // snapshot should be from before the transaction started and so only contain journal entry 1.
        assertTrue(snapshot.exists());
        assertEquals("1", IOUtils.fileToString(new File(snapshot, "snapshot_id.txt")));

        // ensure that the snapshot is of the data from before the second transaction.
        DefaultRecordSerialiser serialiser = new DefaultRecordSerialiser(snapshot);
        Record snapshotRecord = serialiser.deserialise("sample");
        assertEquals(sample, snapshotRecord);
    }

    public void testCompactionDuringRollbackTransaction() throws Exception
    {
        File snapshot = new File(persistentDirectory, "snapshot");

        Record sample = createSampleRecord();

        // insert generates one journal entry.
        recordStore.insert("sample", sample);

        UserTransaction txn = new UserTransaction(transactionManager);
        txn.begin();

        MutableRecord anotherSample = createSampleRecord();
        anotherSample.put("d", "d");
        recordStore.insert("another", anotherSample);

        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                try
                {
                    recordStore.compactNow();
                }
                catch (Exception e)
                {
                    fail("Exception thrown during test: " + e.getClass().getName() + " " + e.getMessage());
                }
            }
        });

        recordStore.update("sample", anotherSample);
        txn.rollback();

        // snapshot should be from before the transaction started and so only contain journal entry 1.
        assertTrue(snapshot.exists());
        assertEquals("1", IOUtils.fileToString(new File(snapshot, "snapshot_id.txt")));

        // ensure that the snapshot is of the data from before the second transaction.
        DefaultRecordSerialiser serialiser = new DefaultRecordSerialiser(snapshot);
        Record snapshotRecord = serialiser.deserialise("sample");
        assertEquals(sample, snapshotRecord);
    }

    public void testTransactionRollback()
    {
        UserTransaction txn = new UserTransaction(transactionManager);
        txn.begin();

        recordStore.insert("sample", createSampleRecord());

        txn.rollback();

        assertNull(recordStore.select().get("sample"));
        assertFalse(new File(persistentDirectory, "index").exists());
        assertFalse(new File(persistentDirectory, "1").exists());

        txn.begin();

        recordStore.insert("sample", createSampleRecord());

        txn.commit();

        assertTrue(new File(persistentDirectory, "index").exists());
        assertTrue(new File(persistentDirectory, "2").exists());
        assertNotNull(recordStore.select().get("sample"));

        txn.begin();

        recordStore.insert("another", createSampleRecord());

        txn.rollback();

        assertTrue(new File(persistentDirectory, "index").exists());
        assertTrue(new File(persistentDirectory, "2").exists());
        assertFalse(new File(persistentDirectory, "3").exists());
        assertNull(recordStore.select().get("another"));
    }

    public void testTransactionCommit()
    {
        UserTransaction txn = new UserTransaction(transactionManager);
        txn.begin();

        recordStore.insert("sample", createSampleRecord());

        assertFalse(new File(persistentDirectory, "index").exists());
        assertFalse(new File(persistentDirectory, "1").exists());

        txn.commit();

        assertTrue(new File(persistentDirectory, "index").exists());
        assertTrue(new File(persistentDirectory, "1").exists());
    }

    // test restart recovery when shutdown occured during a transaction.
    public void testRecoveryOfSnapshotAfterInitialSnapshotDump() throws Exception
    {
        MutableRecord sample = createRandomSampleRecord();
        recordStore.insert("sample", sample);
        recordStore.compactNow();

        File snapshot = new File(persistentDirectory, "snapshot");
        File newSnapshot = new File(persistentDirectory, "snapshot.new");

        assertTrue(snapshot.exists());
        assertFalse(newSnapshot.exists());

        MutableRecord newSample = createRandomSampleRecord();
        DefaultRecordSerialiser serialiser = new DefaultRecordSerialiser(newSnapshot);
        serialiser.serialise("", newSample, true);
        
        assertTrue(newSnapshot.exists());

        restartRecordStore();

        assertEquals(sample, recordStore.select().get("sample"));
        assertFalse(newSnapshot.exists());
    }

    public void testRecoveryOfSnapshotAfterBackup() throws Exception
    {
        MutableRecord sample = createRandomSampleRecord();
        recordStore.insert("sample", sample);
        recordStore.compactNow();

        File snapshot = new File(persistentDirectory, "snapshot");
        File newSnapshot = new File(persistentDirectory, "snapshot.new");
        File backupSnapshot = new File(persistentDirectory, "snapshot.backup");

        assertTrue(snapshot.renameTo(backupSnapshot));

        MutableRecord newSample = createRandomSampleRecord();
        DefaultRecordSerialiser serialiser = new DefaultRecordSerialiser(newSnapshot);
        serialiser.serialise("", newSample, true);

        assertTrue(newSnapshot.exists());
        assertTrue(backupSnapshot.exists());
        assertFalse(snapshot.exists());

        restartRecordStore();

        assertEquals(sample, recordStore.select().get("sample"));
        assertFalse(newSnapshot.exists());
        assertFalse(backupSnapshot.exists());
    }

    public void testRecoveryCleanupOfBackupAfterNewSnapshotCommit() throws Exception
    {
        MutableRecord sample = createRandomSampleRecord();
        recordStore.insert("sample", sample);
        recordStore.compactNow();

        File snapshot = new File(persistentDirectory, "snapshot");
        File backupSnapshot = new File(persistentDirectory, "snapshot.backup");

        assertTrue(snapshot.renameTo(backupSnapshot));

        MutableRecord newSample = createRandomSampleRecord();
        DefaultRecordSerialiser serialiser = new DefaultRecordSerialiser(snapshot);
        serialiser.serialise("sample", newSample, true);
        IOUtils.copyFile(new File(backupSnapshot, "snapshot_id.txt"), new File(snapshot, "snapshot_id.txt"));

        assertTrue(backupSnapshot.exists());
        assertTrue(snapshot.exists());

        restartRecordStore();

        assertEquals(newSample, recordStore.select().get("sample"));
        assertFalse(backupSnapshot.exists());
    }

    // simulate failure to write a transactional file.
    public void testFailureToCreateNewSnapshotDirectoryDuringCompaction() throws IOException
    {
        MutableRecord sample = createRandomSampleRecord();
        recordStore.insert("sample", sample);

        recordStore.setFileSystem(new DefaultFS()
        {
            public boolean mkdirs(File file)
            {
                if (file.equals(new File(persistentDirectory, "snapshot.new")))
                {
                    return false;
                }
                return super.mkdirs(file);
            }
        });

        try
        {
            recordStore.compactNow();
            fail("Expected IOException.");
        }
        catch (IOException e)
        {
            assertTrue(e.getMessage().contains("Failed to create new snapshot directory"));
        }

        // ensure that everything is still ok.
        assertEquals(sample, recordStore.select().get("sample"));

        // ensure that a subsequent compact is successful.
        recordStore.setFileSystem(new DefaultFS());
        recordStore.compactNow();
    }

    public void testFailureToWriteNewIndexDuringTransaction()
    {
        MutableRecord sample = createRandomSampleRecord();
        recordStore.setFileSystem(new DefaultFS()
        {
            public boolean createNewFile(File file) throws IOException
            {
                if (file.getName().equals("index.new"))
                {
                    return false;
                }
                return super.createNewFile(file);
            }
        });
        
        assertNull(recordStore.select().get("sample"));
        UserTransaction txn = new UserTransaction(transactionManager);
        txn.begin();

        Transaction transaction = transactionManager.getTransaction();

        recordStore.insert("sample", sample);
        txn.commit();

        assertNull(recordStore.select().get("sample"));
        assertEquals(TransactionStatus.ROLLEDBACK, transaction.getStatus());
    }

    public void testFailureToWriteNewJournalEntryDuringTransaction()
    {
        MutableRecord sample = createRandomSampleRecord();
        recordStore.setFileSystem(new DefaultFS()
        {
            public boolean createNewFile(File file) throws IOException
            {
                try
                {
                    Integer.parseInt(file.getName());
                    return false;
                }
                catch (NumberFormatException e)
                {
                    return super.createNewFile(file);
                }
            }
        });
        
        UserTransaction txn = new UserTransaction(transactionManager);
        txn.begin();

        Transaction transaction = transactionManager.getTransaction();

        recordStore.insert("sample", sample);
        txn.commit();

        assertNull(recordStore.select().get("sample"));
        assertEquals(TransactionStatus.ROLLEDBACK, transaction.getStatus());
    }

    //---( helper methods. )---

    private MutableRecord createSampleRecord()
    {
        MutableRecord sample = new MutableRecordImpl();
        sample.put("a", "a");
        sample.put("b", "b");
        return sample;
    }

    private MutableRecord createRandomSampleRecord()
    {
        MutableRecord randomSample = new MutableRecordImpl();
        for (int i = 0; i < RAND.nextInt(10); i++)
        {
            randomSample.put(Integer.toString(i), Integer.toString(RAND.nextInt(20)));
        }
        return randomSample;
    }

    private void assertEquals(Record expected, Record actual)
    {
        assertEquals(expected.size(), actual.size());

        assertEquals(expected.keySet(), actual.keySet());
        for (String key : expected.keySet())
        {
            assertEquals(expected.get(key), actual.get(key));
        }

        assertEquals(expected.metaKeySet(), actual.metaKeySet());
        for (String key : expected.metaKeySet())
        {
            assertEquals(expected.getMeta(key), actual.getMeta(key));
        }

        assertEquals(expected.nestedKeySet(), actual.nestedKeySet());
        for (String key : expected.nestedKeySet())
        {
            assertEquals((Record)expected.get(key), (Record)actual.get(key));
        }
    }
}
