package com.nchroniaris.ASC.client.core;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String USAGE_STRING = "java -jar ASC-client.jar [-h|-s|-c|-d]";

    public static void main(String[] args) {

        ASCClient.ClientOptions options = new ASCClient.ClientOptions();

        // Split single combination arguments into multiple arguments so that the rest of this function can process them correctly
        List<String> convertedArgs = Main.convertArguments(args);

        // Check all arguments. If we don't have any, this loop will not run and it will just run the client normally with networking. If we have exactly one arg it will be checked and handled appropriately. If we have more than one argument it will either print the help text or it will give an error.
        for (String arg : convertedArgs) {

            switch (arg) {

                // Print help and usage information and quit
                case "-h":
                case "--help":
                    Main.printHelp();
                    return;

                case "-s":
                case "--serverless":
                    options.serverless = true;
                    break;

                case "-c":
                case "--console-only":
                    options.consoleOnly = true;
                    break;

                case "-d":
                case "--allow-dumb-terminal":
                    options.allowDumbTerminal = true;
                    break;

                // Print error message, usage, and exit
                default:
                    System.out.printf("Unrecognized option '%s'%n", arg);
                    Main.printUsage();
                    return;

            }

        }

        // Run client with command line options
        Main.runClient(options);

    }

    /**
     * Creates and runs the ASC Client instance to start up the application
     *
     * @param options An ASCClient.ClientOptions instance that has all the options from the command line filled out
     */
    private static void runClient(ASCClient.ClientOptions options) {

        ASCClient client = new ASCClient(options);
        client.start();

    }

    /**
     * Creates a new list of String arguments, with single combination arguments, like "-xyz" into full arguments, like "-x", "-y", and "-z" while preserving order.
     * @param programArgs A primitive String array that represents the current argument list
     * @return A {@code List<String>} that expands combination arguments into multiple entries, but is otherwise the same.
     */
    private static List<String> convertArguments(String[] programArgs) {

        List<String> convertedArgs = new ArrayList<>();

        // Iterate through all the arguments given by the OS
        for (String arg : programArgs) {

            // Trim the argument to allow things like " -h" to get through
            arg = arg.trim();

            // If the argument is a single-dash argument (the character class is for safety), with 1 or more arguments (like "-scd" for example)
            if (arg.matches("^-[A-Za-z0-9]+$")) {

                // For every letter (index 1 and onward, basically excluding the dash), add that as a new entry in the new list. This essentially splits arguments like "-scd" multiple ones: "-s", "-c", and "-d".
                for (int i = 1; i < arg.length(); i++)
                    convertedArgs.add("-" + arg.charAt(i));

            } else {

                // If it's not a single argument in that format just add the argument verbatim. This case will execute for double-dashed arguments, like "--help"
                convertedArgs.add(arg);

            }

        }

        return convertedArgs;

    }

    /**
     * Prints out the full usage and help information. This includes the usage text, link to the GitHub repo, options summary, and additional information.
     */
    private static void printHelp() {

        System.out.printf("Usage: %s%n%n", Main.USAGE_STRING);
        System.out.println("ASC-client is a Java application used for controlling game severs.");
        System.out.println("See https://github.com/Tardnicus/ASC for guides and more detailed information.\n");

        System.out.println("Option Summary:");

        System.out.println("\t-h, --help");
        System.out.println("\t\tShows this help menu");
        System.out.println();

        System.out.println("\t-s, --serverless");
        System.out.printf("\t\tRuns in serverless mode. This will disable all networking functionality and the client will never attempt to register any of its game servers with the ASC server (if configured).%n%n\t\tThis is helpful if you have no need to connect it to an ASC server or otherwise do not want remote commands to be executed on the machine that's running this client.%n");
        System.out.println();

        System.out.println("\t-c, --console-only");
        System.out.printf("\t\tRuns the client in console only mode. When run with this flag the client will not attempt to automatically schedule events upon startup.%n%n\t\tThis is helpful if you just want to execute manual commands in the console without accidentally starting anything automatically.%n");
        System.out.println();

        System.out.println("\t-d, --allow-dumb-terminal");
        System.out.printf("\t\tAllow a dumb terminal to be attached to the front-facing UI. Currently, if on startup the client cannot find a properly configured terminal to attach to, it will exit. This option allows a dumb terminal to be attached, which means that some terminals may now work but with limited or undefined functionality in terms of the operation of the console (the core of the program should be undisturbed).%n%n\t\tUsually this option should not be considered, as most standard terminals work out of the box. However if you are having trouble try using this option and see if it works.%n");
        System.out.println();

    }

    /**
     * Prints out the usage information to stdout. Meant to be called when an incorrect parameter has been read. This is the brief version, as the full help text is in printHelp().
     */
    private static void printUsage() {

        System.out.printf("Usage: %s%n", Main.USAGE_STRING);
        System.out.println("Try 'java -jar ASC-client.jar -h' for more information.");
        System.out.println();

    }

}
