package com.nchroniaris.ASC.client.multiplexer;

import com.nchroniaris.ASC.client.exception.MultiplexerNotFoundException;

import java.io.File;

/**
 * This abstract class defines the set of interactions that must be made for any terminal multiplexer. I've defined this early on to make it possible to extend the functionality to other terminal multiplexers (which would be configurable)
 */
public abstract class TerminalMultiplexer {

    protected final String PATH_EXECUTABLE;

    public TerminalMultiplexer(String executablePath) {

        if (executablePath == null)
            throw new IllegalArgumentException("The executablePath argument cannot be null!");

        if (!new File(executablePath).exists())
            throw new MultiplexerNotFoundException(String.format("The multiplexer runtime specified in the properties file does not exist! Got \"%s\"", executablePath));

        this.PATH_EXECUTABLE = executablePath;

    }

    /**
     * Starts a multiplexer session using a name and an executable
     *
     * @param sessionName The name of the session. This will be used to refer to the session on subsequent calls and used outside of this program to access the executable's standard input and output.
     * @param executable  The program or script that is meant to be run within the session. Make sure this file exists or otherwise works, as each multiplexer need not capture errors from WITHIN a session.
     * @throws IllegalArgumentException If the session already exists, this exception will be thrown
     */
    public abstract void startSession(String sessionName, String executable) throws IllegalArgumentException;

    /**
     * Sends a command to a specific multiplexer session specified by the name. Commands will be run as is as specified by `command`
     *
     * @param sessionName The name of the session. This will be used to direct the command to a specific session.
     * @param command     The contents of the command to run inside the aforementioned session
     * @throws IllegalArgumentException If the session does NOT exist, this exception will be thrown
     */
    public abstract void sendCommand(String sessionName, String command) throws IllegalArgumentException;

}
