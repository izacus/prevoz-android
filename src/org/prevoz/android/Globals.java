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
    public static final String API_URL = "http://prevoz.org/api";
    
    public static final String LOGIN_URL = "http://prevoz.org/accounts/simple/signin/";
    
    public static final String PREF_FILE_NAME = "PrevozPreferences";
    
    public static final int REQUEST_SUCCESS = 0;
    public static final int REQUEST_ERROR_NETWORK = 1;
    public static final int REQUEST_ERROR_SERVER = 2;
}
