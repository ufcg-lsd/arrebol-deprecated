package org.fogbowcloud.app.utils;

public class DataStoreHelper {
	
	protected static String dataStoreFolderExecution = null; 
	public static final String DATASTORES_FOLDER = "/datastores";
	protected static final String DATASTORES_TEST_FOLDER = "/datastores_test";
	protected static final String PREFIX_DATASTORE_URL = "jdbc:sqlite:";

	/**
	 * @param dataStoreUrl coming from properties.
	 * @param dataStoreName coming from each DataStore class.
	 */
	public static String getDataStoreUrl(String dataStoreUrl, String dataStoreName) {
		if (dataStoreUrl == null || dataStoreUrl.isEmpty()) {
			dataStoreUrl = PREFIX_DATASTORE_URL + DATASTORES_FOLDER + "/" + dataStoreName;
		}
		
		return dataStoreUrl;
	}

}