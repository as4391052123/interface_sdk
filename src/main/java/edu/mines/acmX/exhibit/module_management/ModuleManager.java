package edu.mines.acmX.exhibit.module_management;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.mines.acmX.exhibit.input_services.hardware.BadDeviceFunctionalityRequestException;
import edu.mines.acmX.exhibit.input_services.hardware.DeviceConnectionException;
import edu.mines.acmX.exhibit.input_services.hardware.HardwareManager;
import edu.mines.acmX.exhibit.input_services.hardware.HardwareManagerManifestException;
import edu.mines.acmX.exhibit.input_services.hardware.drivers.InvalidConfigurationFileException;
import edu.mines.acmX.exhibit.module_management.loaders.ManifestLoadException;
import edu.mines.acmX.exhibit.module_management.loaders.ModuleLoadException;
import edu.mines.acmX.exhibit.module_management.loaders.ModuleLoader;
import edu.mines.acmX.exhibit.module_management.loaders.ModuleManagerManifestLoader;
import edu.mines.acmX.exhibit.module_management.loaders.ModuleManifestLoader;
import edu.mines.acmX.exhibit.module_management.metas.CheckType;
import edu.mines.acmX.exhibit.module_management.metas.DependencyType;
import edu.mines.acmX.exhibit.module_management.metas.ModuleManagerMetaData;
import edu.mines.acmX.exhibit.module_management.metas.ModuleMetaData;
import edu.mines.acmX.exhibit.module_management.modules.ModuleInterface;

/**
 * TODO cleanup TODO should module manager manifest be located outside of jar
 * files? This class is the main entry point for the exhibit using the interface
 * sdk library. This controls the lifecycle of modules and determines which
 * modules can run by ensuring that they have all of their required
 * dependencies. Also cycles through the next module to be run.
 * 
 * Singleton
 * 
 * @author Andrew DeMaria
 * @author Austin Diviness
 */

public class ModuleManager {

	static Logger logger = LogManager.getLogger(ModuleManager.class.getName());

	/**
	 * Main function for the ModuleManager framework. Creates an instance of
	 * ModuleManager and runs it.
	 */
	public static void main(String[] args) {
		CommandLineParser cl = new GnuParser();
		CommandLine cmd;
		Options opts = generateCLOptions();
		try {
			cmd = cl.parse(opts, args);

			if (cmd.getArgs().length != 0) {
				logger.warn("Left over args: " + cmd.getArgs().toString());
			}

			String[] debugArgs = { "openni-config", "manifest", "modules-path",
					"default-module", "help" };
			debugArgs(cmd, debugArgs);

			if (cmd.hasOption("help")) {
				printHelp(opts);
			} else {

				if (cmd.hasOption("manifest")) {
					ModuleManager.configure(cmd.getOptionValue("manifest"));
				} else {
					logger.warn("Using deprecated default module path");
					ModuleManager
							.configure("src/test/resources/module_manager/CLoaderModuleManagerManifest.xml");
				}
				ModuleManager m;
				m = ModuleManager.getInstance();
				m.run();
			}

		} catch (ParseException e) {
			printHelp(opts);
			logger.error("Incorrect command line arguments");
			e.printStackTrace();
		} catch (ManifestLoadException e) {
			logger.fatal("Could not load the module manager manifest");
			e.printStackTrace();
		} catch (ModuleLoadException e) {
			logger.fatal("Could not load the default module");
			e.printStackTrace();
		} catch (HardwareManagerManifestException e) {
			logger.fatal("Could not load the manfiest for the Hardware Manager");
			e.printStackTrace();
		} catch (BadDeviceFunctionalityRequestException e) {
			logger.fatal("Default module depends on unknown device functionalities");
			e.printStackTrace();
		} catch (InvalidConfigurationFileException e) {
			logger.fatal(e.getMessage());
			e.printStackTrace();
		}

	}

	private static void printHelp(Options opts) {
		HelpFormatter formatter = new HelpFormatter();
		// TODO header and footer
		formatter.printHelp("java -jar [JARNAME]", "header", opts, "footer",
				true);
	}

	private static void debugArgs(CommandLine cmd, String[] args) {
		for (String arg : args) {
			logger.debug(arg + " was: " + cmd.getOptionValue(arg));
		}
	}

	private static Options generateCLOptions() {
		Options options = new Options();
		// options = optionsUsingIndividualAgruments(options);
		options.addOption(optionsUsingManifest());
		// options.addOption(openNiArguments());
		options.addOption("h", "help", false, "Print these helpful hints");
		return options;
	}

	private static Option openNiArguments() {
		return OptionBuilder
				.withLongOpt("openni-config")
				.withDescription(
						"If using openNI, this argument must be populated with a path "
								+ "to a openNI xml configuration file.")
				.hasArg().withArgName("PATH").create();

	}

	private static Options optionsUsingIndividualAgruments(Options opts) {
		opts.addOption(OptionBuilder
				.withLongOpt("default-module")
				.withDescription(
						"Use this module as the default module to load")
				.hasArg().withArgName("MODULE_PACKAGE").create());

		opts.addOption(OptionBuilder.withLongOpt("modules-path")
				.withDescription("Use this path to load modules from").hasArg()
				.withArgName("PATH").create());
		return opts;
	}

	private static Option optionsUsingManifest() {
		return OptionBuilder.withLongOpt("manifest")
				.withDescription("Use a custom module manager manifest file")
				.hasArg().withArgName("PATH").create();
	}

	/**
	 * Singleton instance of ModuleManager This is volatile in order to be safe
	 * with multiple threads
	 */
	private static volatile ModuleManager instance = null;

	// instance of hw manager
	private static HardwareManager hardwareInstance = null;

	// config variables
	private static ModuleManagerMetaData metaData;
	private static String pathToModuleManagerManifest;
	private static String pathToOpenNIConfig;

	// core manager data variables
	private ModuleInterface currentModule;
	/*
	 * private ModuleInterface nextModule;
	 */
	private ModuleInterface defaultModule;
	private ModuleMetaData nextModuleMetaData;
	private ModuleMetaData currentModuleMetaData;
	private ModuleMetaData defaultModuleMetaData;
	private boolean loadDefault;
	private Map<String, ModuleMetaData> moduleConfigs;

	/**
	 * TODO
	 * 
	 * @throws ManifestLoadException
	 */
	public static void configure(String moduleManifestPath)
			throws ManifestLoadException {
		metaData = loadModuleManagerConfig(moduleManifestPath);
	}

//	private static void configure(String defaultModule, String pathToModules) {
//		logger.info("Using explicitly given configuration");
//		metaData = new ModuleManagerMetaData(defaultModule, pathToModules);
//	}

	private ModuleManager() throws ManifestLoadException, ModuleLoadException,
			HardwareManagerManifestException,
			BadDeviceFunctionalityRequestException {
		if (metaData == null) {
			logger.fatal("ModuleManager must be configured before you can create an instance");
			throw new ManifestLoadException(
					"Module Manager was not properly configured");
		}

		refreshModules();
		try {
			setDefaultModule(metaData.getDefaultModuleName());
		} catch (ModuleLoadException e) {
			logger.fatal("Could not load the default module");
			throw e;
		}

		try {
			hardwareInstance = HardwareManager.getInstance();
		} catch (HardwareManagerManifestException e) {
			throw e;
		}

		hardwareInstance.setRunningModulePermissions(defaultModuleMetaData
				.getInputTypes());
		loadDefault = true;
	}

	/**
	 * This method will refresh any modules that are not the default module. The
	 * default module is cached and will not be affected by this function. Any
	 * other modules may be affected in the following ways:
	 * 
	 * a new module will be added if it meets the requirements for dependencies
	 * 
	 * an exisiting module will be removed if the jar file was removed
	 * 
	 * an exisiting module will be removed if the dependencies change.
	 * 
	 */
	private void refreshModules() {
		moduleConfigs = loadAllModuleConfigs(metaData.getPathToModules());
		checkDependencies();
	}

	/**
	 * Fetches instance of ModuleManager, or creates one if it has not yet
	 * created.
	 * 
	 * @return The single instance of ModuleManager
	 * @throws ManifestLoadException
	 *             When the ModuleManager configuration is incorrect
	 * @throws ModuleLoadException
	 * @throws HardwareManagerManifestException
	 * @throws BadDeviceFunctionalityRequestException
	 */
	public static ModuleManager getInstance() throws ManifestLoadException,
			ModuleLoadException, HardwareManagerManifestException,
			BadDeviceFunctionalityRequestException {
		/*
		 * Now this is a bit tricky here. Please dont change this unless you are
		 * well read up on threading.
		 * 
		 * The first if statement is for performance to prevent threads from
		 * uncessarily blocking on the nested synchronized statement.
		 * 
		 * The syncronized statement itself ensures that only one thread can
		 * make an instance if it does not exist
		 */
		if (instance == null) {
			synchronized (ModuleManager.class) {
				if (instance == null) {
					instance = new ModuleManager();
				}
			}
		}

		return instance;
	}

	// TODO document
	/**
	 * Utility class to filter the jar files from other files that may exist in
	 * the modules' directory.
	 */
	private class JarFilter implements FilenameFilter {
		public boolean accept(File dir, String filename) {
			return filename.endsWith(".jar");
		}
	}

	/**
	 * Creates a Map of all Modules found in the directory indicated by the path
	 * and associates each package name to the ModuleMetaData object created
	 * from that Module's manifest file.
	 * 
	 * @param path
	 *            The path to the directy holding modules
	 * 
	 * @return A Map, where the keys are Module package names and the value is
	 *         the meta data gathered from that module's manifest file
	 */
	public Map<String, ModuleMetaData> loadAllModuleConfigs(String path) {
		// TODO caching
		Map<String, ModuleMetaData> modConfigs = new HashMap<String, ModuleMetaData>();
		logger.info("Loading jars in [" + path + "]");
		File jarDir = new File(path);

		File[] listOfJarFiles = jarDir.listFiles(new JarFilter());

		for (File each : listOfJarFiles) {
			try {
				ModuleMetaData m = ModuleManifestLoader.load(each
						.getCanonicalPath());
				m.setJarFileName(each.getName());
				modConfigs.put(m.getPackageName(), m);
			} catch (ManifestLoadException e) {
				logger.warn("Could not load manifest for " + each);
			} catch (IOException e) {
				logger.warn("Could not find manifest for " + each);
			}
		}

		return modConfigs;
	}

	/**
	 * Loads the ModuleManager config file. 
     * TODO should really be private
	 * 
	 * @param path
	 *            Path to the ModuleManager xml config file
	 */
	public static ModuleManagerMetaData loadModuleManagerConfig(String path)
			throws ManifestLoadException {
		logger.info("Loading Module Manager config file [" + path + "]");

		return ModuleManagerManifestLoader.load(path);
	}

	/**
	 * Loads an instance of ModuleInterface from the associated ModuleMetaData.
	 * 
	 * @param data
	 *            ModuleMetaData to be loaded
	 * 
	 * @return loaded Module
	 * @throws ModuleLoadException
	 */
	public ModuleInterface loadModuleFromMetaData(ModuleMetaData data)
			throws ModuleLoadException {
		String path = (new File(metaData.getPathToModules(),
				data.getJarFileName())).getPath();
		return ModuleLoader.loadModule(path, data, this.getClass()
				.getClassLoader());
	}

	/**
	 * Iterates through the loaded ModuleMetaData objects, removing those that
	 * don't have their required module dependencies.
	 */
	private void checkModuleDependencies() {
		// First generate a new depth first search data instance
		Map<String, CheckType> depthData = generateEmptyDepthFirstSeachData();
		// part of a depth first search
		// while there are modules that have not been checked,
		// check them.
		String moduleNameToCheck;
		boolean isModuleOkay;
		while ((moduleNameToCheck = getFirstModuleWithType(depthData,
				CheckType.UNKNOWN)) != null) {
			isModuleOkay = canModuleRunWithItsDependentModules(
					moduleNameToCheck, depthData);
			if (!isModuleOkay) {
				this.moduleConfigs.remove(moduleNameToCheck);
			}
		}
	}

	/**
	 * This function is used internally for performing its checkDependencies
	 * operation
	 * 
	 * @return Returns the first module name ( or key in this case ) of a module
	 *         that has not yet been checked (CheckType.UNKNOWN). If there are
	 *         no modules with this status then null is returned.
	 */
	private String getFirstModuleWithType(Map<String, CheckType> depthData,
			CheckType desired) {
		Iterator<String> i = depthData.keySet().iterator();
		while (i.hasNext()) {
			String currentKey = i.next();
			CheckType current = depthData.get(currentKey);
			if (current == desired) {
				return currentKey;
			}
		}
		return null;
	}

	/**
	 * Iterates through the loaded ModuleMetaData objects, removing those that
	 * don't have their required input services.
	 */
	private void checkModuleInputServices() {
        // TODO checkPermisions(..)
		// TODO!!!
	}

	/**
	 * Ensures that all modules have all dependencies available, including
	 * required modules and input services.
	 */
	public void checkDependencies() {
        // TODO remove check module Input Services..
		checkModuleInputServices();
		checkModuleDependencies();
	}

	/**
	 * This function is used internally to generate the data needed in the depth
	 * first search algorithm. It is not be used outside of the class.
	 * 
	 * @return An empty Map of module names to their current check status
	 */
	private Map<String, CheckType> generateEmptyDepthFirstSeachData() {
		Map<String, CheckType> depthData = new HashMap<String, CheckType>();
		Iterator<String> i = this.moduleConfigs.keySet().iterator();
		while (i.hasNext()) {
			depthData.put(i.next(), CheckType.UNKNOWN);
		}

		return depthData;

	}

	/**
	 * Checks a module can run only in regards to whether or not modules it
	 * depends on exist. In essence this function will return false if a module
	 * has a required dependency and whether that required module and any of
	 * that referenced modules referenced modules (i.e. its recursive) do not
	 * exist. It will return true otherwise. Note that optional module
	 * dependencies are exactly that, optional and are not checked.
	 * 
	 * Notice that this function essenitally performs a <a
	 * href="http://en.wikipedia.org/wiki/Depth-first_search"
	 * >DepthFirstSeach</a> when checking modules to successfully accomplish its
	 * goal without ending in an infinite loop.
	 * 
	 * @param current
	 *            ModuleMetaData currently being checked
	 * 
	 * @param checkedModules
	 *            Data about the current progress of the module checks
	 * @return true if the module dictated by the given current module name can
	 *         run.
	 */
	private boolean canModuleRunWithItsDependentModules(String current,
			Map<String, CheckType> checkedModules) {

		// This is the main part of the DFS algorithm
		checkedModules.put(current, CheckType.DIRTY);
		logger.info("Checking module dependencies for " + current);
		ModuleMetaData meta = moduleConfigs.get(current);
		if (meta == null) {
			// we ran into a module that does not exist! AHAHA... make sure
			// that this module does not exist, stop processing and return false
			logger.warn("One of the required modules for " + current
					+ " does not exisit. Removing " + current);
			checkedModules.remove(current);
			moduleConfigs.remove(current); // slightly unnecessary at this point
											// since we know it does not exist
			return false;
		}

		// Now make sure that all required submodules exist
		boolean moduleOkay = true;
		Map<String, DependencyType> dependencies = meta.getModuleDependencies();

		Iterator<String> i = dependencies.keySet().iterator();
		while (i.hasNext()) {
			String nextToCheck = i.next();
			if (dependencies.get(nextToCheck) == DependencyType.REQUIRED) {
				CheckType statusOfNextToCheck = checkedModules.get(nextToCheck);
				if (statusOfNextToCheck == null
						|| statusOfNextToCheck == CheckType.UNKNOWN) {
					// notice that the following operator will short circuit and
					// nextToCheck may be checked later (or not at all) if
					// moduleOkay is already false.
					// Also notice that it should be okay to call this next
					// function with a module name that does not exist
					moduleOkay = moduleOkay
							&& canModuleRunWithItsDependentModules(nextToCheck,
									checkedModules);

				} else if (checkedModules.get(nextToCheck) == CheckType.DIRTY) {
					// We have a circular dependency at this point and should
					// not make a new call to check the nextModule
					// NOTHING
				}
			}
		}

		if (!moduleOkay) {
			// we ran into a module that does not have all of its dependencies!
			// remove this module from our module listing
			logger.warn("Removing module " + current + " from module list");
			checkedModules.remove(current);
			moduleConfigs.remove(current);
			return false;
		} else {
			logger.info("Module " + current + " has all required dependencies.");
			checkedModules.put(current, CheckType.CHECKED); // part of DFS
			return true;
		}

	}

	/**
	 * Main run loop of the ModuleManager. Each loops sets the next module to
	 * the default module specified, then runs the current module's init
	 * function. After, the current module is set to the next module, which will
	 * be the defualt module if the current module has not specified the next
	 * one.
	 * 
	 * A note about the internal implementation: We use a semaphore
	 * implementation called CountDownLatch <a
	 * href="http://en.wikipedia.org/wiki/Semaphore_(programming)">link</a>
	 * which is used to block the module manager execution in run while a module
	 * is running. At first we were hoping to call a blocking function in the
	 * run function so that we could wait on a return from the module. This soon
	 * became unreasonable as a typical Processing Module entry point is #init
	 * which would not block because it would spawn new threads. This may also
	 * become relevant for the AWTModule.
	 * 
	 * Right now it is assumed that only one module is running at a time and
	 * hence only one module will be activly calling the module managers run
	 * function.. If this ever becomes not the case we may need multiple
	 * countDown latches and ensure that this is syncronized along with any
	 * other public functions.
     * TODO document ordering on calling hw functions
     *      document manifest stuff
     *      changes to the metaData
     *      general integration aspect
	 * @throws InvalidConfigurationFileException 
     *
	 */
	public void run() throws InvalidConfigurationFileException {
		while (true) {
			setupPreRuntime();
            runCurrentModule();
            postModuleRuntime();
		}
	}
	
	private void setupPreRuntime() throws InvalidConfigurationFileException {
		if (loadDefault = true ) {
			preDefaultRuntime();
		} else {
			try {
				preModuleRuntime();
			} catch (ModuleLoadException e) {
				logger.error("Module [" + nextModuleMetaData.getPackageName()
						+ "] could not be loaded");
				logger.warn("Loading default module");
				preDefaultRuntime();
			} catch (BadDeviceFunctionalityRequestException e) {
				logger.error("Module [" + nextModuleMetaData.getPackageName()
						+ "] depends on unknown functionality");
				logger.warn("Loading default module");
				preDefaultRuntime();
			}
		}

        // if anything is thrown here it is not specific to a module.
		commonPreRuntime();
	}
	
	private void commonPreRuntime() throws InvalidConfigurationFileException {
        loadDefault = true;
        hardwareInstance.resetAllDrivers();
	}

	private void preDefaultRuntime() {
		setCurrentAsDefault();
		try {
			hardwareInstance.setRunningModulePermissions(defaultModuleMetaData
					.getInputTypes());
		} catch (BadDeviceFunctionalityRequestException e) {
			// This should never happen because the default module has already
			// been checked with the hardware manager during the instantiation of
			// the ModuleManager
			logger.fatal("The default module does not have its required devices");
		}
	}

	private void preModuleRuntime() throws BadDeviceFunctionalityRequestException, ModuleLoadException {
		setCurrentAsNextModule();
		hardwareInstance.setRunningModulePermissions(currentModuleMetaData
				.getInputTypes());
		logger.info("Loaded module " + nextModuleMetaData.getPackageName());
	}

	private void runCurrentModule() {
		CountDownLatch waitForModule = new CountDownLatch(1);

		currentModule.init(waitForModule);
		currentModule.execute();

		try {
			waitForModule.await();
		} catch (InterruptedException e) {
			logger.warn("Module execution was interrupted");
		}

	}

	private void postModuleRuntime() {
		// refresh modules
		refreshModules();
	}

	public static void setPathToManifest(String path) {
		ModuleManager.pathToModuleManagerManifest = path;
	}

	/**
	 * Sets the default module for ModuleManager. Throws an exception if the
	 * default module cannot be loaded in which case the ModuleManager should
	 * exit.
	 * 
	 * @param name
	 *            Package name of module to be made default.
	 * 
	 */
	private void setDefaultModule(String name) throws ModuleLoadException {
		defaultModuleMetaData = moduleConfigs.get(name);
		defaultModule = loadModuleFromMetaData(defaultModuleMetaData);
	}

	/**
	 * Sets the current module as the default module including the required
	 * metaData.
	 */
	private void setCurrentAsDefault() {
		// TODO check that defaultModule still exists?
		// actually we may not care.
		currentModule = defaultModule;
		currentModuleMetaData = defaultModuleMetaData;
	}

	/**
	 * This will attempt to set the current module as the nextModule
	 * 
	 * @throws ModuleLoadException
	 */
	private void setCurrentAsNextModule() throws ModuleLoadException {
		currentModuleMetaData = nextModuleMetaData;
		currentModule = loadModuleFromMetaData(nextModuleMetaData);
	}

	/**
	 * Sets next module to be loaded, after the current module.
     *
     * TODO check the module can run with the hardware manager.
	 * 
	 * @param name
	 *            Package name of module to be loaded next.
	 * 
	 * @return true if module is set, false otherwise.
	 */
	public boolean setNextModule(String name) {
		// may just be a call to query?
		// make a test to check that xml is checked as well even if the module
		// exists
		// TODO check configuration for name
		// grab the associated ModuleMetaData
		// instantiate the next module using loadModuleFromMetaData
		// TODO check that this method is syncronized!!!
		// BE CAREFUL!!!

		// check that currentModule can set this package in question
		if (!currentModuleMetaData.getOptionalAll()
				&& !currentModuleMetaData.getModuleDependencies().containsKey(
						name)) {
			return false;
		}
		try {
			nextModuleMetaData = moduleConfigs.get(name);
			if (nextModuleMetaData == null) {
				throw new ModuleLoadException(
						"Metadata for the requested module is not available");
			}
            hardwareInstance.checkPermissions( nextModuleMetaData.getInputTypes() );
			loadDefault = false;
			return true;
		} catch (ModuleLoadException e) {
			loadDefault = true; // dont necessarily need since it wasnt changed.
			return false;
		} catch (BadDeviceFunctionalityRequestException e) {
			// Hardware functionality was not known
			loadDefault = true;  // dont necessarily need since it wanst changed
			return false;
		}
	}

	public InputStream loadResourceFromModule(String jarResourcePath,
			String packageName) {
		ModuleMetaData data = moduleConfigs.get(packageName);
		try {
			return ModuleLoader.loadResource(metaData.getPathToModules() + "/"
					+ data.getJarFileName(), data, this.getClass()
					.getClassLoader(), jarResourcePath);
		} catch (MalformedURLException e) {
			// TODO Logging
			e.printStackTrace();
			return null;
		} catch (ModuleLoadException e) {
			// TODO Logging
			e.printStackTrace();
			return null;
		}
	}

	public InputStream loadResourceFromModule(String jarResourcePath) {
		return loadResourceFromModule(jarResourcePath,
				currentModuleMetaData.getPackageName());
	}

	private static void setOpenNiConfiguration(String path) {
		pathToOpenNIConfig = path;
	}

	// TODO should be private, made public for tests
	public void setCurrentModuleMetaData(String name) {
		currentModuleMetaData = moduleConfigs.get(name);
	}

	public String[] getAllAvailableModules() {
		return moduleConfigs.keySet().toArray(new String[0]);
	}

	// USED ONLY FOR TESTING BELOW THIS COMMENT
	public void setCurrentModuleMetaData(ModuleMetaData current) {
		this.currentModuleMetaData = current;
	}

	public ModuleManagerMetaData getMetaData() {
		return metaData;
	}

	public static void removeInstance() {
		pathToModuleManagerManifest = null;
		instance = null;
	}

	public void setModuleMetaDataMap(Map<String, ModuleMetaData> m) {
		this.moduleConfigs = m;
	}

	public Map<String, ModuleMetaData> getModuleMetaDataMap() {
		return this.moduleConfigs;
	}

	public void setCurrentModule(ModuleInterface m) {
		currentModule = m;
	}

	public void testSetDefaultModule(String name) throws ModuleLoadException {
		setDefaultModule(name);
	}

	public void setMetaData(ModuleManagerMetaData data) {
		metaData = data;
	}

	public static void createEmptyInstance() {
		instance = new ModuleManager("NotUsed");
	}

	private ModuleManager(
			String notUsedExceptToDifferentiateBetweenTheActualCTor) {

	}

	public String getNextModuleName() {
		return this.nextModuleMetaData.getPackageName();
	}
}
