package edu.mines.acmX.exhibit.module_management.modules;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import edu.mines.acmX.exhibit.module_management.loaders.ManifestLoadException;
import edu.mines.acmX.exhibit.module_management.loaders.ModuleLoadException;
import edu.mines.acmX.exhibit.module_management.metas.ModuleMetaData;

/**
 * Interface that all Modules must implement in some way or another.
 * Contains the functions that are needed to properly interact with
 * Module Manager.
 *
 * @author	Andrew DeMaria
 * @author	Austin Diviness
 */
public interface ModuleInterface {

    public boolean setNextModuleToLoad( String moduleName );

	public InputStream loadResourceFromModule( String jarResourcePath, ModuleMetaData m ) throws ManifestLoadException, ModuleLoadException;

	public InputStream loadResourceFromModule( String jarResourcePath ) throws ManifestLoadException, ModuleLoadException;
	
	public ModuleMetaData getModuleMetaData(String packageName);
	
	public String[] getAllAvailableModules();

	public void init(CountDownLatch waitForModule);
	
	public void execute();
	
	public void finishExecution();

}


