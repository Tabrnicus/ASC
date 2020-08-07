package com.nchroniaris.ASC.client.multiplexer;

/**
 * This interface defines the set of interactions that must be made for any terminal multiplexer. I've defined an interface early on to make it possible to extend the functionality to other terminal multiplexers (likely configurable)
 */
public interface TerminalMultiplexer {

    /**
     * Starts a multiplexer session using a name and an executable
     *
     * @param sessionName The name of the session. This will be used to refer to the session on subsequent calls and used outside of this program to access the executable's standard input and output.
     * @param executable  The program or script that is meant to be run within the session. Make sure this file exists or otherwise works, as each multiplexer need not capture errors from WITHIN a session.
     * @throws IllegalArgumentException If the session already exists, this exception will be thrown
     */
    void startSession(String sessionName, String executable) throws IllegalArgumentException;

    /**
     * Sends a command to a specific multiplexer session specified by the name. Commands will be run as is as specified by `command`
     *
     * @param sessionName The name of the session. This will be used to direct the command to a specific session.
     * @param command     The contents of the command to run inside the aforementioned session
     * @throws IllegalArgumentException If the session does NOT exist, this exception will be thrown
     */
    void sendCommand(String sessionName, String command) throws IllegalArgumentException;

}
