package amralhossary.bonds;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.biojava.nbio.core.util.FileDownloadUtils;
import org.biojava.nbio.structure.align.util.UserConfiguration;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.density.DensityMapSource;

/**
 * The class is responsible for loading and saving application settings.
 * 
 * The settings are stored as properties,pairs of ID and VALUE, in a property file.
 * The ID is one of static Strings.
 * <p>
 * Newly added property should have an ID, added in {@link #loadSettings()} and {@link #saveSettings()}
 */
public class SettingsManager{

	private static final String PDB_FILES_FOLDER_KEY = "PdbFolder";
	private static final String WORKING_FOLDER_KEY = "HomeFolder";
	private static final String DOMAIN_ENABLED_KEY = "domainEnabled";
	private static final String AUTOFETCH_KEY = "autoFetch";
	private static final String FILEFORMAT_KEY = "fileFormat";
	private static final String SHOW_WHILE_PROCESSING_KEY = "showWhileProcessing";
	private static final String SHOW_ONLY_SELECTED_MODEL_KEY = "showOnlySelectedModel";
	private static final String SHOW_ELECTRON_DENSITY_KEY = "showElectronDensity";
	private static final String AUTOFETCH_ELECTRON_DENSITY_KEY = "autoFetchElectronDensity";
	private static final String DENSITY_MAP_KIND_KEY = "densityMapKind";
	private static final String DENSITY_CONTOUR_SIGMA_KEY = "densityContourSigma";
	private static final String DENSITY_CLIP_RADIUS_KEY = "densityClipRadius";
	private static final String DENSITY_CACHE_FOLDER_KEY = "densityCacheFolder";
	private static final String DENSITY_XRAY_SOURCES_KEY = "densityXraySources";
	private static final String DENSITY_EM_SOURCES_KEY = "densityEmSources";

	/**
	 * Defaults for the density settings, in one place.
	 * <p>
	 * These are the single authority: {@link #loadSettings()} falls back to them when a
	 * key is absent, and the "Restore defaults" buttons reset to them. The older settings
	 * do not have this - their defaults are written out in {@code loadSettings} and
	 * duplicated again in the bundled {@code res/CrossLinX_settings.ini}, which is exactly
	 * the drift described in KNOWN-ISSUES.md. New settings start the way the old ones
	 * should end up.
	 */
	public static final boolean DEFAULT_SHOW_ELECTRON_DENSITY = true;
	/** Off: showing a cached map costs nothing, downloading one is the user's decision. */
	public static final boolean DEFAULT_AUTOFETCH_ELECTRON_DENSITY = false;
	public static final String DEFAULT_DENSITY_MAP_KIND = "2fofc";
	public static final double DEFAULT_DENSITY_CONTOUR_SIGMA = 1.0;
	/** Angstroms around the interacting atoms; see ParsingUI's density fetch. */
	public static final double DEFAULT_DENSITY_CLIP_RADIUS = 5.0;
	/** Empty means "the BioJava cache folder", which sits beside the PDB store. */
	public static final String DEFAULT_DENSITY_CACHE_FOLDER = "";
	/** Empty means "whatever chain BioJava ships", rather than a list frozen at build time. */
	public static final String DEFAULT_DENSITY_XRAY_SOURCES = "";
	public static final String DEFAULT_DENSITY_EM_SOURCES = "";

	private static final String PROPERTIES_FILENAME = /*System.getProperty("user.dir")+*/ "CrossLinX_settings.ini";
	public static final boolean debugging = false;


	ArrayList<SettingListener> listeners = new ArrayList<SettingsManager.SettingListener>();
	/**static instance for the singleton*/
	private static SettingsManager settingsManager = null;
	private UserConfiguration userConfiguration = null; //new UserConfiguration();
	private String workingFolder;
	private boolean domainEnabled;

	private boolean showWhileProcessing;

	private boolean showOnlySelectedModel;

	private boolean showElectronDensity;
	private boolean autoFetchElectronDensity;
	private String densityMapKind;
	private double densityContourSigma;
	private double densityClipRadius;
	private String densityCacheFolder;
	private String densityXraySources;
	private String densityEmSources;

	public void loadSettings() {
		Properties properties=new Properties();
		InputStream res=null;
		try {
			File file=new File(PROPERTIES_FILENAME);
			if(file.exists()){
				System.out.println("Property File existss");
				res=new FileInputStream(file);
			} else {
				System.out.println("Property File doesn't exist");
				res=ClassLoader.getSystemResourceAsStream(PROPERTIES_FILENAME);
			}

			properties.load(res);
			String pdbFilePath = readStringProperty(properties, PDB_FILES_FOLDER_KEY, null);
			if (pdbFilePath != null) {
				pdbFilePath = FileDownloadUtils.expandUserHome(pdbFilePath);
//				userConfiguration.setPdbFilePath(pdbFilePath);
				if (pdbFilePath.endsWith("/") || pdbFilePath.endsWith(File.separator)) {
					pdbFilePath = pdbFilePath.substring(0, pdbFilePath.length()-1);
				}
				setPdbFilePath(pdbFilePath);
			} else {
				userConfiguration = new UserConfiguration();
				pdbFilePath = userConfiguration.getPdbFilePath();
			}
			String workingFilePath = readStringProperty(properties, WORKING_FOLDER_KEY, null);
			if (workingFilePath != null) {
				workingFilePath = FileDownloadUtils.expandUserHome(workingFilePath);
				this.workingFolder = workingFilePath;
			} else {
				this.workingFolder = new File(userConfiguration.getPdbFilePath(), "out").getPath();
			}
			
//			this.setFileFormat(readStringProperty(properties, FILEFORMAT_KEY, UserConfiguration.PDB_FORMAT));
			this.setFileFormat(readStringProperty(properties, FILEFORMAT_KEY, UserConfiguration.MMCIF_FORMAT));
			this.setAutoFetch(readBooleanProperty(properties, AUTOFETCH_KEY, this.userConfiguration.getFetchBehavior() != FetchBehavior.LOCAL_ONLY));
			this.setShowWhileProcessing(readBooleanProperty(properties, SHOW_WHILE_PROCESSING_KEY, true));
			this.setDomainEnabled(readBooleanProperty(properties, DOMAIN_ENABLED_KEY, true));
			this.setShowOnlySelectedModel(readBooleanProperty(properties, SHOW_ONLY_SELECTED_MODEL_KEY, true));

			this.showElectronDensity = readBooleanProperty(properties, SHOW_ELECTRON_DENSITY_KEY, DEFAULT_SHOW_ELECTRON_DENSITY);
			this.autoFetchElectronDensity = readBooleanProperty(properties, AUTOFETCH_ELECTRON_DENSITY_KEY, DEFAULT_AUTOFETCH_ELECTRON_DENSITY);
			this.densityMapKind = readStringProperty(properties, DENSITY_MAP_KIND_KEY, DEFAULT_DENSITY_MAP_KIND);
			this.densityContourSigma = readDoubleProperty(properties, DENSITY_CONTOUR_SIGMA_KEY, DEFAULT_DENSITY_CONTOUR_SIGMA);
			this.densityClipRadius = readDoubleProperty(properties, DENSITY_CLIP_RADIUS_KEY, DEFAULT_DENSITY_CLIP_RADIUS);
			this.densityCacheFolder = readStringProperty(properties, DENSITY_CACHE_FOLDER_KEY, DEFAULT_DENSITY_CACHE_FOLDER);
			this.densityXraySources = readStringProperty(properties, DENSITY_XRAY_SOURCES_KEY, DEFAULT_DENSITY_XRAY_SOURCES);
			this.densityEmSources = readStringProperty(properties, DENSITY_EM_SOURCES_KEY, DEFAULT_DENSITY_EM_SOURCES);

			System.out.println("Load Settings Ended");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(res!=null){
				try {
					res.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	private Boolean readBooleanProperty(Properties properties, String key, Boolean defaultValue) {
		return properties.containsKey(key) ? 
				Boolean.parseBoolean(properties.getProperty(key)) :
					defaultValue;
	}
	/**
	 * A malformed number falls back to the default rather than aborting the load: the
	 * whole of {@link #loadSettings()} sits in one try/catch, so an unparsable value here
	 * would silently leave every setting read after it at its field default.
	 */
	private double readDoubleProperty(Properties properties, String key, double defaultValue) {
		if (! properties.containsKey(key)) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(properties.getProperty(key));
		} catch (NumberFormatException e) {
			System.err.println("Setting [" + key + "] is not a number: [" + properties.getProperty(key)
					+ "]. Using " + defaultValue + ".");
			return defaultValue;
		}
	}
	private String readStringProperty(Properties properties, String key, String defaultValue) {
		return properties.containsKey(key)?properties.getProperty(key):defaultValue;
	}

	public void saveSettings(boolean closing) {
		Properties properties=new Properties();
		FileOutputStream res=null;
		try {
			File file=new File(PROPERTIES_FILENAME);
			if(!file.exists())
				file.createNewFile();
			res=new FileOutputStream(file);
			properties.clear();
			properties.setProperty(PDB_FILES_FOLDER_KEY, getPdbFilePath());
			properties.setProperty(WORKING_FOLDER_KEY, workingFolder);
			properties.setProperty(FILEFORMAT_KEY, getFileFormat());
			properties.setProperty(AUTOFETCH_KEY, String.valueOf(isAutoFetch()));
			properties.setProperty(SHOW_WHILE_PROCESSING_KEY, String.valueOf(isShowWhileProcessing()));
			properties.setProperty(DOMAIN_ENABLED_KEY, String.valueOf(isDomainEnabled()));
			properties.setProperty(SHOW_ONLY_SELECTED_MODEL_KEY, String.valueOf(isShowOnlySelectedModel()));
			properties.setProperty(SHOW_ELECTRON_DENSITY_KEY, String.valueOf(isShowElectronDensity()));
			properties.setProperty(AUTOFETCH_ELECTRON_DENSITY_KEY, String.valueOf(isAutoFetchElectronDensity()));
			properties.setProperty(DENSITY_MAP_KIND_KEY, getDensityMapKind());
			properties.setProperty(DENSITY_CONTOUR_SIGMA_KEY, String.valueOf(getDensityContourSigma()));
			properties.setProperty(DENSITY_CLIP_RADIUS_KEY, String.valueOf(getDensityClipRadius()));
			properties.setProperty(DENSITY_CACHE_FOLDER_KEY, getDensityCacheFolder());
			properties.setProperty(DENSITY_XRAY_SOURCES_KEY, getDensityXraySources());
			properties.setProperty(DENSITY_EM_SOURCES_KEY, getDensityEmSources());

			properties.store(res, null);
			System.out.println("Save Settings Ended");
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(res!=null){
				try {
					res.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}	
		if(!closing) {
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).refreshSettings();
			}
		}
	}

	private SettingsManager() {
		try {
			loadSettings();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}


	/**
	 * static getter of instance
	 * @return a single instance of {@link SettingsManagerImpl}
	 */
	public static SettingsManager getSettingsManager() {
		return (settingsManager==null)?settingsManager=new SettingsManager():settingsManager ;
	}

	/**
	 * Note that this method creates a new {@link UserConfiguration} object.
	 * @param pdbFilePath the pdbFilePath to set
	 * @throws IOException 
	 */
	public void setPdbFilePath(String pdbFolder) throws IOException {
		File temp = new File(pdbFolder);
		if (temp.exists() || temp.mkdirs()) {
			//must be done before creating the userConfiguration object
			System.setProperty(UserConfiguration.PDB_DIR, pdbFolder);
			if(this.userConfiguration == null) {
				//Take it from the system property PDB_DIR
				this.userConfiguration = new UserConfiguration();
				// No need to set it explicitly as it was set in the system property before userConfiguration was created. 
//				this.userConfiguration.setPdbFilePath(pdbFolder);
			}else {
				this.userConfiguration.setPdbFilePath(pdbFolder);
			}
		}else {
			throw new IOException("Folder ["+pdbFolder+"]  NOT found & couldn't be created !");
		}
	}

	/**
	 * @return the pdbFilePath
	 */
	public String getPdbFilePath() {
		return userConfiguration.getPdbFilePath();
	}

	
	public boolean isAutoFetch() {
		return userConfiguration.getFetchBehavior() != FetchBehavior.LOCAL_ONLY;
//		return userConfiguration.getAutoFetch();
	}

	public void setAutoFetch(boolean autoFetch) {
		if (autoFetch) {
			userConfiguration.setFetchBehavior(FetchBehavior.DEFAULT);
		} else {
			userConfiguration.setFetchBehavior(FetchBehavior.LOCAL_ONLY);
		}
	}
	
	

	public void setFileFormat(String fileFormat) {
		userConfiguration.setFileFormat(fileFormat);
	}

	public String getFileFormat() {
		return userConfiguration.getFileFormat();
	}

	public boolean isShowWhileProcessing() {
		return showWhileProcessing;
	}

	/**
	 * @param showWhileProcessing the showWhileProcessing to set
	 */
	public void setShowWhileProcessing(boolean showWhileProcessing) {
		this.showWhileProcessing = showWhileProcessing;
	}

	public UserConfiguration getUserConfiguration() {
		return userConfiguration;
	}

	public String getWorkingFolder() {
		return workingFolder;
	}

	public void setWorkingFolder(String workingFolder) throws IOException {
		File homeFolder = new File(workingFolder);
		if (homeFolder.exists() || homeFolder.mkdirs()) {
			this.workingFolder = workingFolder;
			File cacheFolder = new File(workingFolder+File.separatorChar+ResultManager.CACHE_RESULT_FOLDER);
			if (! cacheFolder.exists()) {
				cacheFolder.mkdirs();
			}
		}else {
			throw new IOException("Folder ["+workingFolder+"]  NOT found & couldn't be created !");
		}
	}

	/**
	 * Whether the viewer shows only the model picked in the links panel, or all the models
	 * of an ensemble at once.
	 * <p>
	 * On by default: the models of an NMR ensemble sit on top of one another, so showing
	 * all 38 at once is a thicket, and an interaction highlighted in one of them is lost
	 * in the other 37.
	 */
	public boolean isShowOnlySelectedModel() {
		return this.showOnlySelectedModel;
	}

	public void setShowOnlySelectedModel(boolean showOnlySelectedModel) {
		this.showOnlySelectedModel = showOnlySelectedModel;
	}

	public boolean isDomainEnabled() {
		return this.domainEnabled;
	}

	public void setDomainEnabled(boolean domainEnabled) {
		this.domainEnabled = domainEnabled;
	}

	/**
	 * Whether a density map is drawn around the interacting atoms of the shown structure.
	 * <p>
	 * On by default, because on its own this only draws maps already in the cache, which
	 * costs nothing and reaches no network. Downloading is a separate decision - see
	 * {@link #isAutoFetchElectronDensity()}.
	 */
	public boolean isShowElectronDensity() {
		return this.showElectronDensity;
	}

	public void setShowElectronDensity(boolean showElectronDensity) {
		this.showElectronDensity = showElectronDensity;
	}

	/**
	 * Whether a map missing from the cache may be downloaded.
	 * <p>
	 * Off by default. Deliberately independent of {@link #isAutoFetch()}, which governs
	 * coordinate files: a user with a complete local PDB mirror still has no density maps,
	 * and maps are far larger than the coordinates they belong to.
	 */
	public boolean isAutoFetchElectronDensity() {
		return this.autoFetchElectronDensity;
	}

	public void setAutoFetchElectronDensity(boolean autoFetchElectronDensity) {
		this.autoFetchElectronDensity = autoFetchElectronDensity;
	}

	/** A {@code DensityMapKind} label: {@code 2fofc}, {@code fofc}, {@code em} or {@code auto}. */
	public String getDensityMapKind() {
		return this.densityMapKind;
	}

	public void setDensityMapKind(String densityMapKind) {
		this.densityMapKind = densityMapKind;
	}

	/** Contour level, as a multiple of the map's RMS deviation. */
	public double getDensityContourSigma() {
		return this.densityContourSigma;
	}

	public void setDensityContourSigma(double densityContourSigma) {
		this.densityContourSigma = densityContourSigma;
	}

	/**
	 * How far around the interacting atoms the map is contoured, in Angstroms. Clipping is
	 * not only tidiness: contouring a whole cryo-EM grid takes long enough to look like a
	 * freeze.
	 */
	public double getDensityClipRadius() {
		return this.densityClipRadius;
	}

	public void setDensityClipRadius(double densityClipRadius) {
		this.densityClipRadius = densityClipRadius;
	}

	/**
	 * Where cached maps live, or empty for BioJava's own cache folder, which sits beside
	 * the PDB store.
	 */
	public String getDensityCacheFolder() {
		return this.densityCacheFolder;
	}

	/**
	 * @param densityCacheFolder the folder, or empty/null to follow the PDB store
	 * @throws IOException if the folder does not exist and cannot be created
	 */
	public void setDensityCacheFolder(String densityCacheFolder) throws IOException {
		if (densityCacheFolder == null || densityCacheFolder.trim().isEmpty()) {
			this.densityCacheFolder = "";
			return;
		}
		String expanded = FileDownloadUtils.expandUserHome(densityCacheFolder.trim());
		File folder = new File(expanded);
		if (folder.exists() || folder.mkdirs()) {
			this.densityCacheFolder = expanded;
		} else {
			throw new IOException("Folder [" + expanded + "]  NOT found & couldn't be created !");
		}
	}

	/**
	 * The X-ray source chain, as ordered comma-separated {@code DensityMapSource} labels.
	 * A source present in the list is enabled and a source absent from it is disabled, so
	 * one string carries both the order and the enabling. Empty means BioJava's own chain.
	 */
	public String getDensityXraySources() {
		return this.densityXraySources;
	}

	public void setDensityXraySources(String densityXraySources) {
		this.densityXraySources = densityXraySources == null ? "" : densityXraySources;
	}

	/** The cryo-EM source chain; see {@link #getDensityXraySources()}. */
	public String getDensityEmSources() {
		return this.densityEmSources;
	}

	public void setDensityEmSources(String densityEmSources) {
		this.densityEmSources = densityEmSources == null ? "" : densityEmSources;
	}

	/**
	 * Reads a stored source chain back into the order BioJava wants.
	 * <p>
	 * A label that no longer exists is skipped rather than rejected, so a settings file
	 * written by a newer build - or by a BioJava that has since renamed a source - still
	 * loads, minus the source nobody here recognises.
	 *
	 * @param stored ordered, comma-separated source tokens; empty or null for the default
	 * @param defaultChain what to use when nothing is stored, i.e. BioJava's own chain
	 * @return the enabled sources, in the user's order
	 */
	public static List<DensityMapSource> parseSourceChain(String stored, List<DensityMapSource> defaultChain) {
		if (stored == null || stored.trim().isEmpty()) {
			return defaultChain;
		}
		List<DensityMapSource> chain = new ArrayList<DensityMapSource>();
		for (String token : stored.split(",")) {
			DensityMapSource source = sourceForToken(token.trim());
			if (source != null && ! chain.contains(source)) {
				chain.add(source);
			}
		}
		//An empty result means every stored label was unrecognisable. Falling back to the
		//default beats handing BioJava an empty chain, which could never find anything.
		return chain.isEmpty() ? defaultChain : chain;
	}

	/** @return the tokens of {@code chain}, in order, as stored in the settings file */
	public static String formatSourceChain(List<DensityMapSource> chain) {
		if (chain == null || chain.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (DensityMapSource source : chain) {
			if (builder.length() > 0) {
				builder.append(',');
			}
			builder.append(source.getFileToken());
		}
		return builder.toString();
	}

	/** @return the source with that file token, or null if none has it */
	private static DensityMapSource sourceForToken(String token) {
		for (DensityMapSource source : DensityMapSource.values()) {
			if (source.getFileToken().equalsIgnoreCase(token)) {
				return source;
			}
		}
		return null;
	}

	interface SettingListener{
		void refreshSettings();
	}
	public void registerListener(SettingListener listener) {
		if (! listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

}
