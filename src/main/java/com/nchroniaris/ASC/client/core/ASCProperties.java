package com.nchroniaris.ASC.client.core;

import java.io.*;
import java.net.URLDecoder;
import java.util.Properties;

/**
 * Class that handles the reading, writing, and handling of properties from a given ASCProperties file. This gives the rest of the program access to file paths that can of course be changed by the user.
 */
public class ASCProperties {

    // Lazy style Singleton implementation
    private static ASCProperties properties = null;

    // This attribute holds the absolute path for the directory that contains the jar file.
    // I am aware doing this might be a bit awkward, but the specific way I have thought this application out is in such a way that it is meant to be "portable". Therefore, I would prefer if all relevant files that are core to the application reside in some sort of directory relative to the jar file. This also comes with the benefit of not having to make sure that the working directory is the same as the directory where the jar resides, as ALL files will be relative to THIS path instead of relative to the working dir.
    // Of course, this approach is done in favor of creating a configuration file in the **home directory** for example, which would house a "main directory" property of some sort which would avoid such black magic as shown in the function.
    private static final String PATH_WORKING_DIR = ASCProperties.findJarWorkingDir();

    // Location of the properties file. Written as a combination of the actual path of the jar file and a relative path for which the file should exist. For safety, we are using File.separator in order to grab the system separator chars ('/' on UNIX and '\' on Windows).
    private static final String PATH_PROPERTIES = ASCProperties.PATH_WORKING_DIR + File.separator + "resources" + File.separator + "ASC.properties";

    // These are absolute paths that are meant to be in accordance with the ASC.properties file. They are public because they are declared final.
    public final String PATH_SCREEN;
    public final String PATH_DB;

    /**
     * This gets the instance of the class as it is implemented as a singleton.
     *
     * @return The ASCProperties instance
     */
    public static ASCProperties getInstance() {

        if (ASCProperties.properties == null)
            ASCProperties.properties = new ASCProperties();

        return ASCProperties.properties;

    }

    /**
     * Uses the location of the Main class as a reference to obtain the path to the directory that encloses the jar file being run. We must decode the path as a URL as the presence of any spaces in the path will result in a `%20` instead of an actual space.
     *
     * @return The full path (spaces are unescaped) of the directory that the .jar resides in.
     */
    private static String findJarWorkingDir() {

        // Adapted from: https://stackoverflow.com/questions/40317459/how-to-open-a-file-in-the-same-directory-as-the-jar-file-of-the-application

        // Get the location of the jar file, and then represent it as a file to get its parent.
        String dir = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();

        // Try to decode the URL-esque string we get above. This is to remove spaces and other special URL characters.
        try {

            dir = URLDecoder.decode(dir, "UTF-8");

        } catch (UnsupportedEncodingException e) {

            System.err.println("[CRITICAL] Something went wrong with finding the location of the .jar file: " + e.getMessage());
            System.exit(1);

        }

        return dir;

    }

    /**
     * This constructor reads and initializes the ASCProperties file and fills in all its instance variables with the corresponding ones from the properties file.
     */
    private ASCProperties() {

        Properties properties = new Properties();

        // Since the instance variables are final, we must do this rigamarole to avoid a compiler error. In reality, any of these variables will NEVER be null as upon catching an exception we will exit.
        String PATH_DB = null;
        String PATH_SCREEN = null;

        // Open the properties file as a FileInputStream using a try-with-resources block and load the properties file and all relevant keys
        try (FileInputStream propertiesFile = new FileInputStream(ASCProperties.PATH_PROPERTIES)) {

            properties.load(propertiesFile);

            // Get all variables from the properties file. The reason for including
            PATH_DB = ASCProperties.PATH_WORKING_DIR + File.separator + properties.getProperty("path.db").replaceAll(String.format("^%s+", File.separator), "");
            PATH_SCREEN = properties.getProperty("path.screen").trim();

        } catch (FileNotFoundException e) {

            System.err.println("Properties file not found! Please run the initial setup first.");
            System.exit(1);

        } catch (IOException e) {

            System.err.println("There was an issue reading the properties file! Perhaps it is corrupt.");
            e.printStackTrace();
            System.exit(1);

        } catch (NullPointerException e) {

            System.err.println("One or more of the properties does not exist or is commented out! Please run initial setup to rebuild the properties file.");
            System.exit(1);

        }

        this.PATH_DB = PATH_DB;
        this.PATH_SCREEN = PATH_SCREEN;

    }

    // TODO: 2020-08-05 Method to generate a default properties file

}
