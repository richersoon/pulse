package com.zutubi.tove.config;

import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractNamedConfiguration;
import com.zutubi.tove.security.*;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.MapType;
import com.zutubi.tove.type.TemplatedMapType;
import com.zutubi.tove.type.TypeException;
import com.zutubi.tove.type.record.MutableRecord;
import org.acegisecurity.AccessDeniedException;

import java.util.*;

/**
 */
public class ConfigurationSecurityManagerTest extends AbstractConfigurationSystemTestCase
{
    private static final String SCOPE_A = "scopeA";
    private static final String SCOPE_A_TEMPLATED = "scopeATemplated";
    private static final String SCOPE_B = "scopeB";
    private static final String ACTION_CREATE_A = "CREATE_A";
    private static final String ACTION_DELETE_A = "DELETE_A";

    private String pathA1;
    private String pathA2;
    private String pathGlobal;
    private String pathChild;
    private String pathB1;
    private String pathNestedB;

    protected void setUp() throws Exception
    {
        super.setUp();
        CompositeType typeA = registerMap(MockA.class, SCOPE_A, false);
        registerMap(MockA.class, SCOPE_A_TEMPLATED, true);
        registerMap(MockB.class, SCOPE_B, false);

        configurationSecurityManager.registerGlobalPermission("scopeA", AccessManager.ACTION_CREATE, ACTION_CREATE_A);
        configurationSecurityManager.registerGlobalPermission("scopeA/*", AccessManager.ACTION_DELETE, ACTION_DELETE_A);
        configurationSecurityManager.registerOwnedScope(SCOPE_A);
        configurationSecurityManager.registerOwnedScope(SCOPE_A_TEMPLATED);

        accessManager.registerAuthorityProvider(MockA.class, new AuthorityProvider<MockA>()
        {
            public Set<String> getAllowedAuthorities(String action, MockA resource)
            {
                return new HashSet<String>(Arrays.asList(resource.getName() + ":" + action));
            }
        });

        pathA1 = configurationTemplateManager.insert(SCOPE_A, new MockA("a1"));
        pathA2 = configurationTemplateManager.insert(SCOPE_A, new MockA("a2"));

        MockA a = new MockA("global");
        MutableRecord record = unstantiate(a);
        configurationTemplateManager.markAsTemplate(record);
        pathGlobal = configurationTemplateManager.insertRecord(SCOPE_A_TEMPLATED, record);
        long handle = configurationTemplateManager.getRecord(pathGlobal).getHandle();

        a = new MockA("child");
        record = unstantiate(a);
        configurationTemplateManager.setParentTemplate(record, handle);
        pathChild = configurationTemplateManager.insertRecord(SCOPE_A_TEMPLATED, record);

        pathB1 = configurationTemplateManager.insert(SCOPE_B, new MockB("b1"));
        configurationTemplateManager.insert(SCOPE_B, new MockB("b2"));

        pathNestedB = ((MockA) configurationTemplateManager.getInstance(pathA1)).getB().getConfigurationPath();
    }

    private CompositeType registerMap(Class clazz, String scope, boolean templated) throws TypeException
    {
        CompositeType type = typeRegistry.register(clazz);
        MapType mapType = templated ? new TemplatedMapType(type, typeRegistry) : new MapType(type, typeRegistry);
        configurationPersistenceManager.register(scope, mapType);
        return type;
    }

    public void testFindOwningResourceNoOwner()
    {
        assertNull(configurationSecurityManager.findOwningResource(pathB1));
    }

    public void testFindOwningResourceSelfOwner()
    {
        MockA a = (MockA) configurationSecurityManager.findOwningResource(pathA1);
        assertEquals("a1", a.getName());
    }

    public void testFindOwningResourceOwner()
    {
        MockA a = (MockA) configurationSecurityManager.findOwningResource(pathNestedB);
        assertEquals("a1", a.getName());
    }

    public void testHasPermissionNoOwner()
    {
        setActor("auth");
        assertFalse(configurationSecurityManager.hasPermission(pathB1, "nope"));
        assertTrue(configurationSecurityManager.hasPermission(pathB1, "auth"));
    }

    public void testHasPermissionOwner()
    {
        setActor("a1:auth");
        assertFalse(configurationSecurityManager.hasPermission(pathA1, "nope"));
        assertTrue(configurationSecurityManager.hasPermission(pathA1, "auth"));
    }

    public void testHasPermissionOwnerNested()
    {
        setActor("a1:auth");
        assertFalse(configurationSecurityManager.hasPermission(pathNestedB, "nope"));
        assertTrue(configurationSecurityManager.hasPermission(pathNestedB, "auth"));
    }

    public void testHasPermissionGlobalPermission()
    {
        setActor(ACTION_CREATE_A);
        assertFalse(configurationSecurityManager.hasPermission(SCOPE_A, "nope"));
        assertTrue(configurationSecurityManager.hasPermission(SCOPE_A, AccessManager.ACTION_CREATE));
    }
    
    public void testHasPermissionGlobalPermissionPattern()
    {
        setActor(ACTION_DELETE_A);
        assertFalse(configurationSecurityManager.hasPermission(pathA1, "nope"));
        assertTrue(configurationSecurityManager.hasPermission(pathA1, AccessManager.ACTION_DELETE));
    }

    public void testHasPermissionCreateNested()
    {
        setActor("a1:" + AccessManager.ACTION_CREATE);
        assertFalse(configurationSecurityManager.hasPermission(pathNestedB, AccessManager.ACTION_CREATE));
        setActor("a1:" + AccessManager.ACTION_WRITE);
        assertTrue(configurationSecurityManager.hasPermission(pathNestedB, AccessManager.ACTION_CREATE));
    }

    public void testHasPermissionDeleteNested()
    {
        setActor("a1:" + AccessManager.ACTION_DELETE);
        assertFalse(configurationSecurityManager.hasPermission(pathNestedB, AccessManager.ACTION_DELETE));
        setActor("a1:" + AccessManager.ACTION_WRITE);
        assertTrue(configurationSecurityManager.hasPermission(pathNestedB, AccessManager.ACTION_DELETE));
    }

    public void testViewPermissionReverseInherited()
    {
        setActor("child:" + AccessManager.ACTION_VIEW);
        assertTrue(configurationSecurityManager.hasPermission(pathChild, AccessManager.ACTION_VIEW));
        assertTrue(configurationSecurityManager.hasPermission(pathGlobal, AccessManager.ACTION_VIEW));
    }

    public void testWritePermissionNotReverseInherited()
    {
        setActor("child:" + AccessManager.ACTION_WRITE);
        assertTrue(configurationSecurityManager.hasPermission(pathChild, AccessManager.ACTION_WRITE));
        assertFalse(configurationSecurityManager.hasPermission(pathGlobal, AccessManager.ACTION_WRITE));
    }

    public void testEnsurePermission()
    {
        setActor("auth");
        try
        {
            configurationSecurityManager.ensurePermission(pathA2, "nope");
            fail();
        }
        catch(AccessDeniedException e)
        {
            assertEquals("Permission to nope at path 'scopeA/a2' denied", e.getMessage());
        }
    }

    public void testFilterPaths()
    {
        List<String> paths = new LinkedList<String>(Arrays.asList("a1", "a2"));
        setActor("a1:auth");
        configurationSecurityManager.filterPaths(SCOPE_A, paths, "auth");
        assertEquals(1, paths.size());
        assertEquals("a1", paths.get(0));
    }

    public void testFilterPathsNested()
    {
        List<String> paths = new LinkedList<String>(Arrays.asList("a1/b", "a2/b"));
        setActor("a1:auth");
        configurationSecurityManager.filterPaths(SCOPE_A, paths, "auth");
        assertEquals(1, paths.size());
        assertEquals("a1/b", paths.get(0));
    }

    private void setActor(final String... authorities)
    {
        accessManager.setActorProvider(new ActorProvider()
        {
            public Actor getActor()
            {
                return new DefaultActor("test", authorities);
            }
        });
    }

    @SymbolicName("mockA")
    public static class MockA extends AbstractNamedConfiguration
    {
        private MockB b = new MockB();

        public MockA()
        {
        }

        public MockA(String name)
        {
            super(name);
            b = new MockB("b for " + name);
        }

        public MockB getB()
        {
            return b;
        }

        public void setB(MockB b)
        {
            this.b = b;
        }
    }

    @SymbolicName("mockB")
    public static class MockB extends AbstractNamedConfiguration
    {
        public MockB()
        {
        }

        public MockB(String name)
        {
            super(name);
        }
    }
}
