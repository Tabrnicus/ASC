package com.nchroniaris.ASC.client.core;

import com.nchroniaris.ASC.client.exception.PropertiesNotFoundException;
import com.nchroniaris.ASC.client.exception.PropertyNotSetException;
import com.nchroniaris.ASC.client.multiplexer.ScreenMultiplexer;
import com.nchroniaris.ASC.client.multiplexer.TerminalMultiplexer;

import java.io.*;
import java.net.URLDecoder;
import java.util.Properties;

/**
 * Class that handles the reading, writing, and handling of properties from a given ASCProperties file. This gives the rest of the program access to file paths that can of course be changed by the user.
 */
public class ASCProperties {

    // Lazy style Singleton implementation
    private static ASCProperties properties = null;

    // These are the String representations of the property names in the preferences file. Changes to property names will occur here.
    private static final String PROPERTY_PATH_SCREEN = "path.screen";
    private static final String PROPERTY_PATH_DB = "path.db";
    private static final String PROPERTY_MULTIPLEXER = "multiplexer";

    // This attribute holds the absolute path for the directory that contains the jar file.
    // I am aware doing this might be a bit awkward, but the specific way I have thought this application out is in such a way that it is meant to be "portable". Therefore, I would prefer if all relevant files that are core to the application reside in some sort of directory relative to the jar file. This also comes with the benefit of not having to make sure that the working directory is the same as the directory where the jar resides, as ALL files will be relative to THIS path instead of relative to the working dir.
    // Of course, this approach is done in favor of creating a configuration file in the **home directory** for example, which would house a "main directory" property of some sort which would avoid such black magic as shown in the function.
    private static final String PATH_WORKING_DIR = ASCProperties.findJarWorkingDir();

    // Location of the properties file. Written as a combination of the actual path of the jar file and a relative path for which the file should exist. For safety, we are using File.separator in order to grab the system separator chars ('/' on UNIX and '\' on Windows).
    private static final String PATH_PROPERTIES = ASCProperties.PATH_WORKING_DIR + File.separator + "resources" + File.separator + "ASC.properties";

    // These are public because they are declared final. The `PATH_...` variables are absolute paths that are meant to be in accordance with the ASC.properties file.
    public final String PATH_DB;
    public final TerminalMultiplexer MULTIPLEXER;

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
        TerminalMultiplexer MULTIPLEXER = null;

        // Open the properties file as a FileInputStream using a try-with-resources block and load the properties file and all relevant keys
        try (FileInputStream propertiesFile = new FileInputStream(ASCProperties.PATH_PROPERTIES)) {

            properties.load(propertiesFile);

            // Get all variables from the properties file. The reason for including the trim() is to make sure we are checking for potential null values. This will occur when a certain property does not exist in the property file. Since we call .replaceAll() on the path.db property, there is no reason to call .trim().


            PATH_DB = this.resolvePath(properties.getProperty(ASCProperties.PROPERTY_PATH_DB));

            // These variables are local, and it will not be converted to an instance variable
            String mpType = properties.getProperty(ASCProperties.PROPERTY_MULTIPLEXER);

            String pathScreen = this.resolvePath(properties.getProperty(ASCProperties.PROPERTY_PATH_SCREEN));

            // Figure out what multiplexer the user wants to use and instantiate the right one. This switch statement is shallow but that is because I am only supporting screen at the moment.
            // switch(null) will fail in the case that the property is not set, which will produce a NullPointerException.
            switch (mpType) {

                case "screen":
                    MULTIPLEXER = new ScreenMultiplexer(pathScreen);
                    break;

                default:
                    throw new UnsupportedOperationException(String.format("[CRITICAL] The current multiplexer type set in the properties file is not supported at the moment (Got '%s'). Please specify a valid multiplexer type.", mpType));

            }

        } catch (FileNotFoundException e) {

            throw new PropertiesNotFoundException("[CRITICAL] Properties file not found! Please run the initial setup first.");

        } catch (IOException e) {

            System.err.println("[CRITICAL] There was an issue reading the properties file! Perhaps it is corrupt.");
            e.printStackTrace();
            System.exit(1);

        } catch (NullPointerException e) {

            System.err.println("[CRITICAL] One or more of the properties does not exist or is commented out! Please run initial setup to rebuild the properties file.");
            System.exit(1);

        } catch (PropertyNotSetException e) {

            System.err.println("[CRITICAL] One or more of the mandatory properties are empty! Please set them or regenerate the properties file.");
            System.exit(1);

        }

        this.PATH_DB = PATH_DB;
        this.MULTIPLEXER = MULTIPLEXER;

    }

    /**
     * This method takes a path (relative or absolute) taken from the properties file as input and forces it to be absolute. Relative paths are resolved in reference to the working dir of the jar file. Therefore, the relative path 'resources/ASC.log' will resolve to something like '/path/to/jar/dir/resources/ASC.log'
     *
     * @param path An absolute or relative file path to something.
     * @return `path`, but resolved to be absolute
     * @throws NullPointerException    This is thrown if `path` is null.
     * @throws PropertyNotSetException This is thrown if `path` is an empty String.
     */
    private String resolvePath(String path) throws NullPointerException, PropertyNotSetException {

        // Throwing a NullPointerException will make sure the correct case gets caught in the enclosing try block that this is called from.
        if (path == null)
            throw new NullPointerException();

        // In this case we are raising a custom exception to indicate the absence of a mandatory property.
        if (path.isEmpty())
            throw new PropertyNotSetException();

        // If the path has a single slash in front of it it means it's an absolute path. If this is not the case, we interpret it as a relative path starting from the jar file downwards.
        if (path.charAt(0) == File.separatorChar)
            return path;
        else
            return ASCProperties.PATH_WORKING_DIR + File.separator + path;

    }

    // TODO: 2020-08-05 Method to generate a default properties file

}
