package com.cinnamonbob.model.persistence.hibernate;

import com.cinnamonbob.model.Project;
import com.cinnamonbob.model.persistence.ObjectHandle;
import com.cinnamonbob.model.persistence.ProjectDao;
import org.hibernate.SessionFactory;

/**
 * @author Daniel Ostermeier
 */
public class HibernateObjectHandleResolverTestCase extends PersistenceTestCase
{
    private HibernateObjectHandleResolver resolver;
    private ProjectDao projectDao;

    public HibernateObjectHandleResolverTestCase(String testName)
    {
        super(testName);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        resolver = new HibernateObjectHandleResolver();
        SessionFactory factory = (SessionFactory) context.getBean("sessionFactory");
        resolver.setSessionFactory(factory);
        resolver.setSession(factory.openSession());

        projectDao = (ProjectDao) context.getBean("projectDao");
    }

    public void tearDown() throws Exception
    {
        resolver = null;
        super.tearDown();
    }

    public void testXYZ()
    {
        Project projectA = new Project();
        Project projectB = new Project();
        projectDao.save(projectA);
        projectDao.save(projectB);
        commitAndRefreshTransaction();

        ObjectHandle handleA = new ObjectHandle(projectA.getId(), Project.class);
        ObjectHandle handleB = new ObjectHandle(projectB.getId(), Project.class);

        Object[] resolvedObjects = resolver.resolve(new ObjectHandle[]{handleA, handleB});
        assertNotNull(resolvedObjects);
        assertEquals(2, resolvedObjects.length);
        assertEquals(projectA, resolvedObjects[0]);
        assertEquals(projectB, resolvedObjects[1]);
    }
}