package com.nchroniaris.ASC.client.model;

import com.nchroniaris.ASC.client.multiplexer.TerminalMultiplexer;
import com.nchroniaris.ASC.util.model.GameServer;

import java.io.File;
import java.time.LocalTime;
import java.util.Arrays;

/**
 * A concrete subclass of Event. This particular event executes a file that is specified in its constructor.
 */
public class ExecuteFileEvent extends Event {

    private final String executablePath;
    private final String[] additionalArgs;

    /**
     * The main constructor for ExecuteFileEvent. As required by the superclass we must get a GameServer and a LocalTime. We additionally get a file to run and any number of additional arguments
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
            throw new IllegalArgumentException("Additional arguments cannot be null! If you don't want to add any additional arguments, initialize an empty string list.");

        this.additionalArgs = additionalArgs;

    }

    @Override
    public void run() {

        // Debug
        System.out.printf("pretend we are running the file (%s) at (%s) with the game-moniker combo (%s) with additional args (%s)%n", this.executablePath, time.toString(), super.gameServer.getSessionName(), Arrays.toString(this.additionalArgs));

    }

}
