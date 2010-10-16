package org.prevoz.android;

/**
 * Has static global properties of the project
 * TODO: Find a better representation
 * @author Jernej Virag
 *
 */
public class Globals
{
    /**
     * URL to access Prevoz API
     */
    
    // Cookie domain for authentication cookies
    public static final String API_DOMAIN = "prevoz.org";
    // Used for all API calls
    public static final String API_URL = "http://prevoz.org/api";
    // URL of webpage to authenticate user
    public static final String LOGIN_URL = "http://prevoz.org/accounts/simple/signin/";
    
    public static final String PREF_FILE_NAME = "PrevozPreferences";
    
    public static final int REQUEST_SUCCESS = 0;
    public static final int REQUEST_ERROR_NETWORK = 1;
    public static final int REQUEST_ERROR_SERVER = 2;
    
    
    public static final String[] locations = { "Ljubljana", "Maribor", "Celje", "Murska Sobota", "Beltinci"  };
}
