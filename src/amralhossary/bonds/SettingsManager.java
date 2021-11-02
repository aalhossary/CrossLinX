package amralhossary.bonds;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.biojava.nbio.core.util.FileDownloadUtils;
import org.biojava.nbio.structure.align.util.UserConfiguration;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;

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
	
	private static final String PROPERTIES_FILENAME = /*System.getProperty("user.dir")+*/ "CrossLinX_settings.ini";
	public static final boolean debugging = false;


	ArrayList<SettingListener> listeners = new ArrayList<SettingsManager.SettingListener>();
	/**static instance for the singleton*/
	private static SettingsManager settingsManager = null;
	private UserConfiguration userConfiguration = null; //new UserConfiguration();
	private String workingFolder;
	private boolean domainEnabled;

	private boolean showWhileProcessing;

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
//	private Double readDoubleProperty(Properties properties, String key, Double defaultValue) {
//		return (properties.containsKey(key) ? 
//				Double.parseDouble(properties.getProperty(key)) :
//					defaultValue);
//	}
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

	public boolean isDomainEnabled() {
		return this.domainEnabled;
	}

	public void setDomainEnabled(boolean domainEnabled) {
		this.domainEnabled = domainEnabled;
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
