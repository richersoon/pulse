package com.cinnamonbob.model.persistence.hibernate;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.Loader;
import org.hibernate.loader.entity.EntityLoader;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.BasicEntityPersister;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.cinnamonbob.model.persistence.ObjectHandleResolver;
import com.cinnamonbob.model.persistence.ObjectHandle;

/**
 * @author Daniel Ostermeier
 */
public class HibernateObjectHandleResolver extends ObjectHandleResolver
{
    private SessionImplementor session;
    private SessionFactoryImplementor factory;

    public Object resolve(ObjectHandle handle)
    {
        return session.load(handle.clazz, handle.id);
    }

    public Object[] resolve(ObjectHandle[] handles)
    {
        if (handles.length == 0)
            return new Object[0];

        Serializable[] ids = extractIds(handles);

        // initialise the entity loader
        Class persistentType = handles[0].clazz;
        ClassMetadata metaData = factory.getClassMetadata(persistentType);
        BasicEntityPersister persister = (BasicEntityPersister) factory.getEntityPersister(metaData.getEntityName());
        Loader loader = new EntityLoader(persister, handles.length, LockMode.READ, factory, Collections.EMPTY_MAP);

        List objects = loader.loadEntityBatch(session, ids, persister.getIdentifierType(), null, null, null, persister);
        return objects.toArray(new Object[objects.size()]);
    }

    private Serializable[] extractIds(ObjectHandle[] handles)
    {
        List<Serializable> ids = new LinkedList<Serializable>();
        for (int i = 0; i < handles.length; i++)
        {
            ids.add(handles[i].id);
        }
        return ids.toArray(new Serializable[ids.size()]);
    }



    //---( required resources )---

    public void setSession(Session session)
    {
        this.session = (SessionImplementor) session;
    }

    public void setSessionFactory(SessionFactory factory)
    {
        this.factory = (SessionFactoryImplementor) factory;
    }
}