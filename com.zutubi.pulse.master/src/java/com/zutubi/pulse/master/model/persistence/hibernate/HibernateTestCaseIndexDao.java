package com.zutubi.pulse.master.model.persistence.hibernate;

import com.zutubi.pulse.core.model.TestCaseIndex;
import com.zutubi.pulse.master.model.persistence.TestCaseIndexDao;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate4.HibernateCallback;

import java.util.List;

/**
 * Hibernate implementation of {@link TestCaseIndexDao}.
 */
public class HibernateTestCaseIndexDao extends HibernateEntityDao<TestCaseIndex> implements TestCaseIndexDao
{
    public Class<TestCaseIndex> persistentClass()
    {
        return TestCaseIndex.class;
    }

    public List<TestCaseIndex> findBySuite(final long stageNameId, final String suite)
    {
        return getHibernateTemplate().execute(new HibernateCallback<List<TestCaseIndex>>()
        {
            @SuppressWarnings("unchecked")
            public List<TestCaseIndex> doInHibernate(Session session) throws HibernateException
            {
                Query queryObject = session.createQuery("from TestCaseIndex model where model.stageNameId = :stageNameId and model.name like :suite");
                queryObject.setParameter("stageNameId", stageNameId);
                queryObject.setParameter("suite", suite + "%");
                return queryObject.list();
            }
        });
    }

    public List<TestCaseIndex> findByStage(final long stageNameId)
    {
        return getHibernateTemplate().execute(new HibernateCallback<List<TestCaseIndex>>()
        {
            @SuppressWarnings("unchecked")
            public List<TestCaseIndex> doInHibernate(Session session) throws HibernateException
            {
                Query queryObject = session.createQuery("from TestCaseIndex model where model.stageNameId = :stageNameId");
                queryObject.setParameter("stageNameId", stageNameId);
                return queryObject.list();
            }
        });
    }

    public int deleteByProject(final long projectId)
    {
        return getHibernateTemplate().execute(new HibernateCallback<Integer>()
        {
            public Integer doInHibernate(Session session) throws HibernateException
            {
                Query queryObject = session.createQuery("delete from TestCaseIndex model where model.projectId = :projectId");
                queryObject.setParameter("projectId", projectId);
                return queryObject.executeUpdate();
            }
        });
    }
}
