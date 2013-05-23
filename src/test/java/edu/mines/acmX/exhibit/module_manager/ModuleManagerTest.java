package edu.mines.acmX.exhibit.module_manager;

import static org.junit.Assert.*;
import org.junit.*;
import org.apache.logging.log4j.Logger;

/**
 * Unit test for ModuleManager
 */
public class ModuleManagerTest 
{

    @Before
    public void resetModuleManager() {
        ModuleManager.removeInstance();
    }

	@Test
    public void testGetInstance() {
        ModuleManager m = ModuleManager.getInstance();
        assertTrue( m instanceof ModuleManager);
        ModuleManager other = ModuleManager.getInstance();
        assertTrue( m == other );
    }

    // The module manager should have an instance of ModuleManagerMetaData that
    // has been correctly instantiated with the given xml file.
	@Test
    public void testLoadModuleManagerConfig() {
        String xmlPath = "module_manager/testModuleManagerManifest.xml";
        ModuleManager m = ModuleManager.getInstance();
        m.loadModuleManagerConfig(xmlPath);
        ModuleManagerMetaData shouldEqual = new ModuleManagerMetaData("com.example.test");
        assertTrue( m.getMetaData().equals( shouldEqual ));
    }

    // expect a throw when the xml is baddly formed
    @Test( expected=Exception.class )
    public void testBadXMLModuleManagerConfig() {
        String xmlPath = "module_manager/testBadXMLModuleManagerManifest.xml";
        ModuleManager m = ModuleManager.getInstance();
        m.loadModuleManagerConfig(xmlPath);
    }

    // expect a throw when an xml attribute is missing.
    @Test( expected=Exception.class )
    public void testBadDataModuleManagerConfig() {
        String xmlPath = "module_manager/testBadDataModuleManagerManifest.xml";
        ModuleManager m = ModuleManager.getInstance();
        m.loadModuleManagerConfig(xmlPath);
    }

    // This should go through the test/resources/modules directory and get the
    // appropriate ModuleMetaData structures from jar files.
    @Test
    public void testLoadAllModuleConfigs() {
        ModuleManager m = ModuleManager.getInstance();
        m.loadAllModuleConfigs( "modules" );
        // TODO check that modules are loaded correctly
        assertTrue( false );
    }

    /**
     * Most of this functionality will be tested in the ModuleLoader, however
     * this just ensures that ModuleManager gets what it needs.
     */
    @Test
    public void testLoadModuleFromMetaData() {
        assertTrue( false );
    }

    private ModuleMetaData createEmptyModuleMetaData(String name) {
        Map<String, Boolean> inputTypesA = new HashMap<String, Boolean>();
        List<String> moduleDepA = new ArrayList<String>();
        ModuleMetaData a = new ModuleMetaData(
                name,
                "2.3",
                "2.3",
                "icon.png",
                "Title" + name,
                "Andrew",
                "0.1",
                inputTypesA,
                moduleDepA);
        return a;
    }


    @Test
    public void testCheckModuleDependencies() {
        ModuleMetaData a = createEmptyModuleMetaData("com.test.A");
        a.getRequiredModules().push("com.test.B");

        ModuleMetaData b = createEmptyModuleMetaData("com.test.B");

        // TODO

        assertTrue( false );
    }

    @Test
    public void testCheckModuleDependencyMissing() {
        assertTrue( false );
    }

    @Test
    public void testCheckCircularModuleDependencies() {
        assertTrue( false );
    }

    @Test
    public void testCheckRecursiveMissingModuleDependcies() {
        assertTrue( false );
    }

    @Test
    public void testModuleCanRun() {
        // TODO
        // may need to check order
        assertTrue( false );
    }

    @Test
    public void testRun() {
        // TODO
        assertTrue( false );
    }

    @Test
    public void testSetDefaultModule() {
        // TODO
        assertTrue( false );
    }

    @Test
    public void testSetNextModule() {
        // TODO
        assertTrue( false );
    }

}
