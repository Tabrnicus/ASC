package com.nchroniaris.ASC.client.model;

import com.nchroniaris.ASC.client.core.ASCProperties;
import com.nchroniaris.ASC.client.exception.SessionExistsException;
import com.nchroniaris.ASC.client.multiplexer.TerminalMultiplexer;
import com.nchroniaris.ASC.util.model.GameServer;

import java.io.File;
import java.time.LocalTime;

/**
 * A concrete subclass of Event. This particular event executes a file that is specified in its constructor.
 */
public class ExecuteFileEvent extends Event {

    private final String executablePath;
    private final String[] additionalArgs;

    /**
     * The main constructor for ExecuteFileEvent. As required by the superclass we must get a GameServer and a LocalTime. We additionally get an executable to run and any number of additional arguments
     *
     * @param multiplexer    A TerminalMultiplexer object offered as dependency injection. This can be any one of the classes that implements this interface.
     * @param gameServer     A GameServer object that describes the particular details of the game server that the event belongs to. Many of the attributes of this object are useful for subclasses of `Event`.
     * @param time           A LocalTime object that describes the exact time of day that the event should run.
     * @param executablePath A valid path to an executable file. Existence of the file is checked upon instantiation and can result in an error so make sure it exists.
     * @param additionalArgs Any number (0 or more) additional string arguments to be passed as arguments to the file being executed. CANNOT be null
     */
    public ExecuteFileEvent(TerminalMultiplexer multiplexer, GameServer gameServer, LocalTime time, String executablePath, String[] additionalArgs) {

        super(multiplexer, gameServer, time);

        if (!new File(executablePath).exists())
            throw new IllegalArgumentException(String.format("File (%s) does not exist! Please specify an existing file with the correct permissions", executablePath));

        this.executablePath = executablePath;

        if (additionalArgs == null)
            throw new IllegalArgumentException("Additional arguments cannot be null! If you don't want to add any additional arguments, use the other constructor");

        this.additionalArgs = additionalArgs;

    }

    /**
     * The main constructor for ExecuteFileEvent. As required by the superclass we must get a GameServer and a LocalTime. We additionally get an executable to run.
     *
     * @param multiplexer    A TerminalMultiplexer object offered as dependency injection. This can be any one of the classes that implements this interface.
     * @param gameServer     A GameServer object that describes the particular details of the game server that the event belongs to. Many of the attributes of this object are useful for subclasses of `Event`.
     * @param time           A LocalTime object that describes the exact time of day that the event should run.
     * @param executablePath A valid path to an executable file. Existence of the file is checked upon instantiation and can result in an error so make sure it exists.
     */
    public ExecuteFileEvent(TerminalMultiplexer multiplexer, GameServer gameServer, LocalTime time, String executablePath) {

        // Call main constructor with a default value for args
        this(multiplexer, gameServer, time, executablePath, new String[]{});

    }

    @Override
    protected String eventString() {
        return "Execute (Generic) File";
    }

    @Override
    public void run() {

        try {

            // Use the multiplexer to start a session using the executable and any additional args. Keep in mind that additionalArgs can be empty, but not null. This is enforced in startSession().
            super.multiplexer.startSession(super.gameServer.getSessionName(), this.executablePath, this.additionalArgs);
            ASCProperties.getInstance().LOGGER.logInfo(String.format("Event [%s] - Session '%s' started.", this.eventString(), super.gameServer.getSessionName()));

        } catch (SessionExistsException e) {

            ASCProperties.getInstance().LOGGER.logWarning(String.format("Event [%s] - The session '%s' was not started because it is already active!", this.eventString(), super.gameServer.getSessionName()));

        }

    }

}
