package com.nchroniaris.ASC.client.core;

public class Main {

    private static final String USAGE_STRING = "java -jar ASC-client.jar [-h|-s]";

    public static void main(String[] args) {

        ASCClient.ClientOptions options = new ASCClient.ClientOptions();

        // Check all arguments. If we don't have any, this loop will not run and it will just run the client normally with networking. If we have exactly one arg it will be checked and handled appropriately. If we have more than one argument it will either print the help text or it will give an error.
        for (String arg : args) {

            // Print help and usage information
            if (arg.equals("-h")) {

                Main.printHelp();
                return;

            } else if (arg.equals("-s")) {

                options.serverless = true;

            } else {

                System.out.println(String.format("Unrecognized option '%s'", arg));
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
     * Prints out the full usage and help information. This includes the usage text, link to the GitHub repo, options summary, and additional information.
     */
    private static void printHelp() {

        System.out.println(String.format("Usage: %s\n", Main.USAGE_STRING));
        System.out.println("ASC-client is a Java application used for controlling game severs.");
        System.out.println("See https://github.com/Tardnicus/ASC for guides and more detailed information.\n");

        System.out.println("Option Summary:");
        System.out.println("  -h\tShows this help menu");
        System.out.println("  -s\tRuns in serverless mode (see below)");
        System.out.println();

        System.out.println("Serverless Mode:");
        System.out.println("  When this mode is active, the client will not attempt to register any of its game servers with the master server. In other words all network functionality will be disabled completely. This is helpful if you have no need to connect it to a master server or otherwise do not want remote commands to be executed on the machine that's running this client.");

    }

    /**
     * Prints out the usage information to stdout. Meant to be called when an incorrect parameter has been read. This is the brief version, as the full help text is in printHelp().
     */
    private static void printUsage() {

        System.out.println(String.format("Usage: %s", Main.USAGE_STRING));
        System.out.println("Try 'java -jar ASC-client.jar -h' for more information.");

    }

}
