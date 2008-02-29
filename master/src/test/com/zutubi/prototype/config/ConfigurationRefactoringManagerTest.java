package com.zutubi.prototype.config;

import com.zutubi.config.annotations.Reference;
import com.zutubi.config.annotations.SymbolicName;
import com.zutubi.prototype.type.CompositeType;
import com.zutubi.prototype.type.MapType;
import com.zutubi.prototype.type.TemplatedMapType;
import com.zutubi.prototype.type.TypeException;
import com.zutubi.prototype.type.record.MutableRecord;
import static com.zutubi.prototype.type.record.PathUtils.getPath;
import com.zutubi.pulse.core.config.AbstractNamedConfiguration;
import static com.zutubi.util.CollectionUtils.asMap;
import static com.zutubi.util.CollectionUtils.asPair;
import com.zutubi.validation.ValidationException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 */
public class ConfigurationRefactoringManagerTest extends AbstractConfigurationSystemTestCase
{
    private static final String SAMPLE_SCOPE = "sample";
    private static final String TEMPLATE_SCOPE = "template";

    private ConfigurationRefactoringManager configurationRefactoringManager;
    private CompositeType typeA;
    private String rootPath;
    private long rootHandle;

    protected void setUp() throws Exception
    {
        super.setUp();
        configurationRefactoringManager = new ConfigurationRefactoringManager();
        configurationRefactoringManager.setTypeRegistry(typeRegistry);
        configurationRefactoringManager.setConfigurationTemplateManager(configurationTemplateManager);
        configurationRefactoringManager.setConfigurationReferenceManager(configurationReferenceManager);

        typeA = typeRegistry.register(MockA.class);
        MapType mapA = new MapType(typeA, typeRegistry);

        MapType templatedMap = new TemplatedMapType(typeA, typeRegistry);

        configurationPersistenceManager.register(SAMPLE_SCOPE, mapA);
        configurationPersistenceManager.register(TEMPLATE_SCOPE, templatedMap);

        MutableRecord root = typeA.unstantiate(new MockA("root"));
        configurationTemplateManager.markAsTemplate(root);
        rootPath = configurationTemplateManager.insertRecord(TEMPLATE_SCOPE, root);
        rootHandle = configurationTemplateManager.getRecord(rootPath).getHandle();
    }

    public void testCanCloneEmptyPath()
    {
        assertFalse(configurationRefactoringManager.canClone(""));
    }

    public void testCanCloneSingleElementPath()
    {
        assertFalse(configurationRefactoringManager.canClone(SAMPLE_SCOPE));
    }

    public void testCanCloneNonexistantPath()
    {
        assertFalse(configurationRefactoringManager.canClone("sample/fu"));
    }

    public void testCanCloneTemplateRoot()
    {
        assertFalse(configurationRefactoringManager.canClone(rootPath));
    }

    public void testCanCloneParentPathNotACollection()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertFalse(configurationRefactoringManager.canClone("sample/a/b"));
    }

    public void testCanCloneParentPathAList()
    {
        String aPath = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        String listPath = getPath(aPath, "blist");
        String clonePath = getPath(listPath, configurationTemplateManager.getRecord(listPath).keySet().iterator().next());
        assertFalse(configurationRefactoringManager.canClone(clonePath));
    }

    public void testCanCloneTemplateItem()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertTrue(configurationRefactoringManager.canClone("sample/a"));
    }

    public void testCanCloneItemBelowTopLevel()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertTrue(configurationRefactoringManager.canClone("sample/a/bmap/colby"));
    }

    public void testCloneEmptyPath()
    {
        illegalClonePathHelper("", "Invalid path '': no parent");
    }

    public void testCloneSingleElementPath()
    {
        illegalClonePathHelper(SAMPLE_SCOPE, "Invalid path 'sample': no parent");
    }

    public void testCloneInvalidParentPath()
    {
        illegalClonePathHelper("huh/instance", "Invalid path 'huh': references non-existant root scope 'huh'");
    }

    public void testCloneInvalidPath()
    {
        illegalClonePathHelper("sample/nosuchinstance", "Invalid path 'sample/nosuchinstance': path does not exist");
    }

    public void testCloneParentPathNotACollection()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        illegalClonePathHelper("sample/a/b", "Invalid parent path 'sample/a': only elements of a map collection may be cloned (parent has type com.zutubi.prototype.type.CompositeType)");
    }

    public void testCloneParentPathAList()
    {
        String aPath = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        String listPath = getPath(aPath, "blist");
        String clonePath = getPath(listPath, configurationTemplateManager.getRecord(listPath).keySet().iterator().next());
        illegalClonePathHelper(clonePath, "Invalid parent path '" + listPath + "': only elements of a map collection may be cloned (parent has type com.zutubi.prototype.type.ListType)");
    }

    public void testCloneTemplateRoot()
    {
        illegalClonePathHelper(rootPath, "Invalid path '" + rootPath + "': cannot clone root of a template hierarchy");
    }

    private void illegalClonePathHelper(String path, String expectedError)
    {
        try
        {
            configurationRefactoringManager.clone(path, "clone");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals(expectedError, e.getMessage());            
        }
        catch (ConfigRuntimeException e)
        {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            assertEquals(expectedError, cause.getMessage());
        }
    }

    public void testCloneEmptyCloneName() throws TypeException
    {
        invalidCloneNameHelper(insertTemplateA(rootPath, "a", false), "", "Invalid empty clone key");
    }

    public void testCloneDuplicateCloneName() throws TypeException
    {
        insertTemplateA(rootPath, "existing", false);
        invalidCloneNameHelper(insertTemplateA(rootPath, "a", false), "existing", "name is already in use, please select another name");
    }

    public void testCloneCloneNameInAncestor() throws TypeException
    {
        addB(rootPath, "parentB");
        String childPath = insertTemplateA(rootPath, "child", false);
        configurationTemplateManager.delete(getPath(childPath, "bmap", "parentB"));
        String childBPath = addB(childPath, "childB");
        invalidCloneNameHelper(childBPath, "parentB", "name is already in use in ancestor \"root\", please select another name");
    }

    public void testCloneCloneNameInDescendent() throws TypeException
    {
        String parentBPath = addB(rootPath, "parentB");
        String childPath = insertTemplateA(rootPath, "child", false);
        addB(childPath, "childB");
        invalidCloneNameHelper(parentBPath, "childB", "name is already in use in descendent \"child\", please select another name");
    }

    public void testSimpleClone()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        String clonePath = configurationRefactoringManager.clone(path, "clone of a");
        assertEquals("sample/clone of a", clonePath);
        assertClone(configurationTemplateManager.getInstance(clonePath, MockA.class), "a");
    }

    public void testSimpleCloneInTemplateScope() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a"), false);
        String clonePath = configurationRefactoringManager.clone(path, "clone of a");
        assertEquals("template/clone of a", clonePath);
        MockA clone = configurationTemplateManager.getInstance(clonePath, MockA.class);
        assertTrue(clone.isConcrete());
        assertEquals(rootHandle, configurationTemplateManager.getTemplateParentRecord(clonePath).getHandle());
        assertClone(clone, "a");
    }

    public void testCloneOfTemplate() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a"), true);
        String clonePath = configurationRefactoringManager.clone(path, "clone of a");
        assertEquals("template/clone of a", clonePath);
        MockA clone = configurationTemplateManager.getInstance(clonePath, MockA.class);
        assertFalse(clone.isConcrete());
        assertEquals(rootHandle, configurationTemplateManager.getTemplateParentRecord(clonePath).getHandle());
        assertClone(clone, "a");
    }

    public void testCloneBelowTopLevel()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        MockB colby = configurationTemplateManager.getInstance(path, MockA.class).getBmap().values().iterator().next();
        String clonePath = configurationRefactoringManager.clone(colby.getConfigurationPath(), "clone");
        assertEquals("sample/a/bmap/clone", clonePath);
        MockB clone = configurationTemplateManager.getInstance(clonePath, MockB.class);
        
        assertNotSame(colby, clone);
        assertEquals("clone", clone.getName());
        assertEquals(1, clone.getY());
    }

    public void testMultipleClone()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("1"));
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("2"));

        configurationRefactoringManager.clone(SAMPLE_SCOPE, asMap(asPair("1", "clone of 1"), asPair("2", "clone of 2")));

        assertClone(configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 1"), MockA.class), "1");
        assertClone(configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 2"), MockA.class), "2");
    }

    public void testCloneWithInternalReference()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        MockA instance = configurationTemplateManager.getInstance(path, MockA.class);
        instance.setRefToRef(instance.getRef());
        configurationTemplateManager.save(instance);

        String clonePath = configurationRefactoringManager.clone(path, "clone");
        instance = configurationTemplateManager.getInstance(path, MockA.class);
        assertNotNull(instance.getRef());
        assertSame(instance.getRef(), instance.getRefToRef());

        MockA cloneInstance = configurationTemplateManager.getInstance(clonePath, MockA.class);
        assertNotSame(instance.getRef(), cloneInstance.getRef());
        assertNotSame(instance.getRefToRef(), cloneInstance.getRefToRef());
        assertSame(cloneInstance.getRef(), cloneInstance.getRefToRef());
    }

    public void testMultipleCloneWithReferenceBetween()
    {
        String path1 = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("1"));
        MockA instance1 = configurationTemplateManager.getInstance(path1, MockA.class);
        MockA instance2 = createAInstance("2");
        instance2.setRefToRef(instance1.getRef());
        String path2 = configurationTemplateManager.insert(SAMPLE_SCOPE, instance2);

        configurationRefactoringManager.clone(SAMPLE_SCOPE, asMap(asPair("1", "clone of 1"), asPair("2", "clone of 2")));

        instance1 = configurationTemplateManager.getInstance(path1, MockA.class);
        instance2 = configurationTemplateManager.getInstance(path2, MockA.class);
        MockA clone1 = configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 1"), MockA.class);
        MockA clone2 = configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 2"), MockA.class);

        assertClone(clone1, "1");
        assertClone(clone2, "2");
        assertSame(instance1.getRef(), instance2.getRefToRef());
        assertNotSame(instance1.getRef(), clone2.getRefToRef());
        assertSame(clone1.getRef(), clone2.getRefToRef());
    }

    public void testMultipleCloneOfTemplateHierarchy() throws TypeException
    {
        templateHierarchyHelper(asMap(asPair("parent", "clone of parent"), asPair("child", "clone of child")));
    }

    public void testMultipleCloneOfTemplateHierarchyChildFirst() throws TypeException
    {
        templateHierarchyHelper(asMap(asPair("child", "clone of child"), asPair("parent", "clone of parent")));
    }

    public void testCloneWithInheritedItem() throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(parentPath, new MockA("child"), false);

        String clonePath = configurationRefactoringManager.clone("template/child/bmap/colby", "clone of colby");
        MockB clone = configurationTemplateManager.getInstance(clonePath, MockB.class);
        assertEquals(1, clone.getY());
    }

    public void testCloneWithOveriddenItem() throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        MockA childInstance = createAInstance("child");
        childInstance.getBmap().get("colby").setY(111222333);
        insertTemplateAInstance(parentPath, childInstance, false);

        String clonePath = configurationRefactoringManager.clone("template/child/bmap/colby", "clone of colby");
        MockB clone = configurationTemplateManager.getInstance(clonePath, MockB.class);
        assertEquals(111222333, clone.getY());
    }

    private void templateHierarchyHelper(Map<String, String> originalKeyToCloneKey) throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(parentPath, createAInstance("child"), false);

        configurationRefactoringManager.clone(TEMPLATE_SCOPE, originalKeyToCloneKey);

        String parentClonePath = getPath(TEMPLATE_SCOPE, "clone of parent");
        MockA parentClone = configurationTemplateManager.getInstance(parentClonePath, MockA.class);
        assertFalse(parentClone.isConcrete());
        assertClone(parentClone, "parent");
        assertEquals(rootHandle, configurationTemplateManager.getTemplateParentRecord(parentClonePath).getHandle());

        String childClonePath = getPath(TEMPLATE_SCOPE, "clone of child");
        MockA childClone = configurationTemplateManager.getInstance(childClonePath, MockA.class);
        assertTrue(childClone.isConcrete());
        assertEquals(parentClone.getHandle(), configurationTemplateManager.getTemplateParentRecord(childClonePath).getHandle());
    }

    private MockA createAInstance(String name)
    {
        MockA instance = new MockA(name);
        instance.setX(10);
        MockB b = new MockB("b");
        b.setY(44);
        instance.setB(b);
        instance.getBlist().add(new MockB("lisby"));
        MockB colby = new MockB("colby");
        colby.setY(1);
        instance.getBmap().put("colby", colby);
        instance.setRef(new Referee("ee"));
        return instance;
    }

    private void assertClone(MockA clone, String name)
    {
        assertEquals("clone of " + name, clone.getName());
        assertEquals(10, clone.getX());
        MockB cloneB = clone.getB();
        assertEquals("b", cloneB.getName());
        assertEquals(44, cloneB.getY());
        assertEquals(1, clone.getBlist().size());
        assertEquals("lisby", clone.getBlist().get(0).getName());
        assertEquals(1, clone.getBmap().size());
        MockB cloneColby = clone.getBmap().get("colby");
        assertEquals("colby", cloneColby.getName());
        assertEquals(1, cloneColby.getY());
        Referee cloneRef = clone.getRef();
        assertNotNull(cloneRef);
        assertEquals("ee", cloneRef.getName());
    }

    private void invalidCloneNameHelper(String path, String cloneName, String expectedError) throws TypeException
    {
        try
        {
            configurationRefactoringManager.clone(path, cloneName);
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals(expectedError, e.getMessage());            
        }
        catch(ConfigRuntimeException e)
        {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof ValidationException);
            assertEquals(expectedError, cause.getMessage());
        }
    }

    private String insertTemplateA(String templateParentPath, String name, boolean template) throws TypeException
    {
        return insertTemplateAInstance(templateParentPath, new MockA(name), template);
    }

    private String insertTemplateAInstance(String templateParentPath, MockA instance, boolean template) throws TypeException
    {
        MutableRecord record = typeA.unstantiate(instance);
        if(template)
        {
            configurationTemplateManager.markAsTemplate(record);
        }
        configurationTemplateManager.setParentTemplate(record, configurationTemplateManager.getRecord(templateParentPath).getHandle());
        return configurationTemplateManager.insertRecord(TEMPLATE_SCOPE, record);
    }

    private String addB(String aPath, String name)
    {
        return configurationTemplateManager.insert(getPath(aPath, "bmap"), new MockB(name));
    }

    @SymbolicName("a")
    public static class MockA extends AbstractNamedConfiguration
    {
        private int x;
        private MockB b;
        private List<MockB> blist = new LinkedList<MockB>();
        private Map<String, MockB> bmap = new HashMap<String, MockB>();
        private Referee ref;
        @Reference
        private Referee refToRef;

        public MockA()
        {
        }

        public MockA(String name)
        {
            super(name);
        }

        public int getX()
        {
            return x;
        }

        public void setX(int x)
        {
            this.x = x;
        }

        public MockB getB()
        {
            return b;
        }

        public void setB(MockB b)
        {
            this.b = b;
        }

        public List<MockB> getBlist()
        {
            return blist;
        }

        public void setBlist(List<MockB> blist)
        {
            this.blist = blist;
        }

        public Map<String, MockB> getBmap()
        {
            return bmap;
        }

        public void setBmap(Map<String, MockB> bmap)
        {
            this.bmap = bmap;
        }

        public Referee getRef()
        {
            return ref;
        }

        public void setRef(Referee ref)
        {
            this.ref = ref;
        }

        public Referee getRefToRef()
        {
            return refToRef;
        }

        public void setRefToRef(Referee refToRef)
        {
            this.refToRef = refToRef;
        }
    }

    @SymbolicName("b")
    public static class MockB extends AbstractNamedConfiguration
    {
        private int y;

        public MockB()
        {
        }

        public MockB(String name)
        {
            super(name);
        }

        public int getY()
        {
            return y;
        }

        public void setY(int y)
        {
            this.y = y;
        }
    }

    @SymbolicName("referee")
    public static class Referee extends AbstractNamedConfiguration
    {
        public Referee()
        {
        }

        public Referee(String name)
        {
            super(name);
        }
    }
}
