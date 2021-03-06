/**
 * Copyright (C) 2013 Colorado School of Mines
 *
 * This file is part of the Interface Software Development Kit (SDK).
 *
 * The InterfaceSDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The InterfaceSDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the InterfaceSDK.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mines.acmX.exhibit.module_management.loaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import edu.mines.acmX.exhibit.module_management.loaders.ManifestLoadException;
import edu.mines.acmX.exhibit.module_management.loaders.ModuleManifestLoader;
import edu.mines.acmX.exhibit.module_management.metas.DependencyType;
import edu.mines.acmX.exhibit.module_management.metas.ModuleMetaData;

/**
 * Unit test for ModuleManifestLoader.
 *
 */
public class ModuleManifestLoaderTest {

    @Test
    public void testLoadCorrectModuleWithoutExtraStuff() throws ManifestLoadException {
        String pathToJar = "src/test/resources/modules/HelloWorld.jar";
        Map<String, DependencyType> desiredInputs =  new HashMap<String, DependencyType>();
        Map<String, DependencyType> desiredModules =  new HashMap<String, DependencyType>();
        ModuleMetaData shouldEqual = new ModuleMetaData(
                "edu.mines.andrew.games",
                "Hello",
                "0.0.3",
                "0.0.3",
                "hi.png",
                "Hello World",
                "andrew demaria",
                "0.1",
                desiredInputs,
                desiredModules,
                false);
        
        ModuleMetaData actual = ModuleManifestLoader.load( pathToJar );
        assertTrue( shouldEqual.equals(actual));
    }

    @Test
    public void testLoadCorrectModuleWithOptionalModulesAndInputs() throws ManifestLoadException {
        String pathToJar = "src/test/resources/modules/PiggyGoodWithLotsOfDepend.jar";
        Map<String, DependencyType> desiredInputs =  new HashMap<String, DependencyType>();
        Map<String, DependencyType> desiredModules =  new HashMap<String, DependencyType>();

        
        //TODO Make sure we document the fact that the developers need to look into
        // the hardware manifest file to figure out the names for functionalities.
        // We also need to document what each functionality that WE provide entails.
        desiredInputs.put("acceleration", DependencyType.OPTIONAL);
        desiredInputs.put("image2d", DependencyType.REQUIRED);

        desiredModules.put("edu.mines.acmX.some_other_game", DependencyType.REQUIRED);
        desiredModules.put("edu.mines.acmX.another_game", DependencyType.OPTIONAL);

        ModuleMetaData shouldEqual = new ModuleMetaData(
                "com.andrew.lotsofdepends",
                "Piggy",
                "0.0.0",
                "0.0.0",
                "",
                "i_love_piggys",
                "andrew demaria",
                "0.1",
                desiredInputs,
                desiredModules,
                false);

        assertEquals( shouldEqual, ModuleManifestLoader.load( pathToJar ));
    }

    /**
     * Should fail when the manifest file cannot be located
     * @throws ManifestLoadException 
     */
    @Test(expected=ManifestLoadException.class)
    public void testLoadBadModuleManifest() throws ManifestLoadException {
        String jarPath = "src/test/resources/modules/BadModuleManifest.jar";
        ModuleManifestLoader.load( jarPath );
    }

    /**
     * Should fail when the xml cannot be parsed
     * @throws ManifestLoadException 
     */
    @Test(expected=ManifestLoadException.class)
    public void testLoadMaformedXMLManifest() throws ManifestLoadException {
        String jarPath = "src/test/resources/modules/MaformedXMLManifest.jar";
        ModuleManifestLoader.load( jarPath );
    }

    /**
     * Should fail when the data is incorrect
     * @throws ManifestLoadException 
     */
    @Test(expected=ManifestLoadException.class)
    public void testLoadManifestIllegalStructure() throws ManifestLoadException {
        String jarPath = "src/test/resources/modules/ManifestIllegalStructure.jar";
        ModuleManifestLoader.load( jarPath );
    }
}
