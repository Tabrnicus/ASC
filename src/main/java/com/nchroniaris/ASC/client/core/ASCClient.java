package com.nchroniaris.ASC.client.core;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class ASCClient {

    private boolean serverless;

    // This attribute holds the precise, absolute path for the directory that contains the jar file.
    // I am aware doing this might be a bit awkward, but the specific way I have thought this application out is in such a way that it is meant to be "portable". Therefore, I would prefer if all relevant files that are core to the application reside in some sort of directory relative to the jar file. This also comes with the benefit of not having to make sure that the working directory is the same as the directory where the jar resides, as ALL files will be relative to THIS path instead of relative to the working dir.
    // Of course, this approach is done in favor of creating a configuration file in the home directory for example, that would house a "main directory" property of some sort which would avoid such black magic as shown in the function.
    // Adapted from: https://stackoverflow.com/questions/40317459/how-to-open-a-file-in-the-same-directory-as-the-jar-file-of-the-application
    public static final String jarWorkingDir = ASCClient.findJarWorkingDir();

    /**
     * Uses the location of the Main class as a reference to obtain the path to the directory that encloses the jar file being run. We must decode the path as a URL as the presence of any spaces in the path will result in a `%20` instead of an actual space. Th
     * @return The full, unescaped path of the directory that the .jar resides in.
     */
    private static String findJarWorkingDir() {

        // Get the location of the jar file, and then represent it as a file to get its parent.
        String dir = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();

        // Try to decode the URL-esque string we get above. This is to remove spaces and other special URL characters.
        try {

            dir = URLDecoder.decode(dir, "UTF-8");

        } catch (UnsupportedEncodingException e) {

            System.err.println("[CRITIAL] Something went wrong with finding the location of the .jar file: " + e.getMessage());
            System.exit(1);

        }

        return dir;

    }

    public ASCClient(boolean serverless) {

        this.serverless = serverless;

    }

    public void start() {

        // Test output to see if the arguments work or not
        System.out.println(String.format("The current serverless value is {%s}", this.serverless ? "true" : "false"));

    }

}
