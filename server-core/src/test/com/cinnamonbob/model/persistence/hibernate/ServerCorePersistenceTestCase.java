package com.cinnamonbob.model.persistence.hibernate;

/**
 */
public abstract class ServerCorePersistenceTestCase extends PersistenceTestCase
{
    protected String[] getConfigLocations()
    {
        return new String[]{
                "com/cinnamonbob/bootstrap/testBootstrapContext.xml",
                "com/cinnamonbob/bootstrap/coreTestApplicationContext.xml",
                "com/cinnamonbob/bootstrap/coreDatabaseContext.xml"
        };
    }
}
