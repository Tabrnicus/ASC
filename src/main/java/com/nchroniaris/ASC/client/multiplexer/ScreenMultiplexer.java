package com.nchroniaris.ASC.client.multiplexer;

import com.nchroniaris.ASC.client.core.ASCProperties;
import com.nchroniaris.ASC.client.exception.MultiplexerNotFoundException;

import java.io.File;
import java.io.IOException;

/**
 * This is a concrete implementation of the TerminalMultiplexer interface. This class implements methods of interacting with the host OS's GNU screen implementation. Keep in mind that the path.screen property must be set to a VALID installation of GNU screen (not FAU or any others) or else some of these commands may not work.
 */
public class ScreenMultiplexer implements TerminalMultiplexer {

    private final String PATH_SCREEN;

    /**
     * Creates a new ScreenMultiplexer() instance. Calling this constructor will check if the property value in ASCProperties.PATH_SCREEN leads to a valid executable (not necessarily a screen executable)
     */
    public ScreenMultiplexer() {

        ASCProperties properties = ASCProperties.getInstance();

        this.PATH_SCREEN = properties.PATH_SCREEN;

        if (!new File(this.PATH_SCREEN).exists())
            throw new MultiplexerNotFoundException(String.format("The screen runtime specified in the properties file does not exist! Got \"%s\"", this.PATH_SCREEN));

    }

    @Override
    public void startSession(String sessionName, String executable) throws IllegalArgumentException {

        // If the session by the same name ALREADY exists, it makes no sense to make a new one. Therefore we throw an error.
        if (this.sessionExists(sessionName))
            throw new IllegalArgumentException(String.format("Screen session '%s' exists already! Please make sure to exit this session properly before starting a new one!", sessionName));

        ProcessBuilder builder = new ProcessBuilder();

        // Set up command. This uses screen and creates a detached session (-dm) whose window is adaptable (-A) with a session name (-S). It runs the executable in that screen session. Whether this executable works or not will not be reflected in this method call -- barring any issues that will be thrown as an IOException such as invalid run perms
        builder.command(this.PATH_SCREEN, "-AdmS", sessionName, executable);

        // Attempt to run the command to start up a new session in screen with the executable. We don't care about the exit code (as in most cases the exit code of the SCREEN command is not representative of any actual errors created WITHIN the session), so we ignore the return value. Recall that this method blocks the calling thread until the command has completed execution.
        this.runProcess(builder);

    }

    @Override
    public void sendCommand(String sessionName, String command) throws IllegalArgumentException {

        // If the session by the same name DOES NOT exist, it makes no sense to send a command to a non existent session. Therefore we throw an error.
        if (!this.sessionExists(sessionName)) {
            throw new IllegalArgumentException(String.format("Screen session '%s' does NOT exist! Please make sure to start this session before sending any commands to it!", sessionName));
        }

        ProcessBuilder builder = new ProcessBuilder();

        // Set up command. I'll be honest here and say i really don't know why this behemoth of a screen command works. I ended up finding a working solution a long time ago after many hours of googling and trial and error.
        builder.command(this.PATH_SCREEN, "-p0", "-S", sessionName, "-X", "exec", ".\\!\\!", "echo", command);

        // Attempt to run the command in a particular session. We don't care about the exit code (as in most cases the exit code of the SCREEN command is not representative of any actual errors created WITHIN the session), so we ignore the return value. Recall that this method blocks the calling thread until the command has completed execution.
        this.runProcess(builder);

    }

    /**
     * This checks if a specific screen session exists.
     *
     * @param sessionName The name of the session to check. Note that due to the way that screen handles session name matching if `sessionName` is a "left" substring of a larger session name then it may match a larger session name and may produce an unexpected result. For example, if the sessionName is 'ABC' then it will match 'ABCD' but not 'ZABC'
     * @return A boolean representing if the session exists or not.
     */
    private boolean sessionExists(String sessionName) {

        ProcessBuilder builder = new ProcessBuilder();

        // Set up command. This particular one queries the session with the specified name and tells it to select the current window. If this fails, we get a non-zero
        builder.command(this.PATH_SCREEN, "-S", sessionName, "-Q", "select", ".");

        // Attempt to run the command and get the return code which will tell us if the session exists or not. Recall that this method blocks the calling thread until the command has completed execution.
        int returnCode = this.runProcess(builder);

        return (returnCode == 0);

    }

    /**
     * This is a generic method to handle running a command created by a ProcessBuilder. This method as a result doesn't care what the contents of the command are, as its only function is to run it and handle its exceptions
     * @param builder The ProcessBuilder created by calling ProcessBuilder.command().
     * @return The exit code of the process running the command. This may or may not be useful to the caller.
     */
    private int runProcess(ProcessBuilder builder) {

        // Define return code. This will be updated in the following try block. We set it up as non zero to avoid a compiler error.
        int returnCode = 1;

        // Run command and get returnCode
        try {

            // Starts the command's execution using the sessionName parameter above
            Process process = builder.start();

            // Blocks the executing thread and eventually returns the exit code of the process.
            returnCode = process.waitFor();

        } catch (IOException e) {

            System.err.println("Some sort of error occurred while trying to execute the process!");
            e.printStackTrace();
            System.exit(1);

        } catch (InterruptedException e) {

            System.err.println("Process was interrupted while waiting for process to finish!");
            e.printStackTrace();
            System.exit(1);

        }

        return returnCode;

    }

}
