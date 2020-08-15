package com.nchroniaris.ASC.client.model;

import com.nchroniaris.ASC.client.multiplexer.TerminalMultiplexer;
import com.nchroniaris.ASC.util.model.GameServer;

import java.time.LocalTime;

/**
 * Concrete subclass of RunCommandEvent. Specifically, this class handles creating the warning text of a server by providing the right warn command to the superclass. Evidently, this is a class that utilizes the Template design pattern in order to dynamically inject the right command to run.
 */
public class WarnCommandEvent extends RunCommandEvent {

    public static final String REPLACE_STRING = "$TIME";

    private final String timeIntervalMinutes;

    /**
     * Since the warn command is known ahead of time, the only thing that needs to be specified during instantiation is the number of minutes left until server shutdown.
     * @param multiplexer    A TerminalMultiplexer object offered as dependency injection. This can be any one of the classes that implements this interface.
     * @param gameServer          A GameServer object that describes the particular details of the game server that the event belongs to.
     * @param time                A LocalTime object that describes the exact time of day that the event should run.
     * @param timeIntervalMinutes A string representation of the number of minutes left. This will replace every occurrence of $TIME in the warn command with this exact string.
     */
    public WarnCommandEvent(TerminalMultiplexer multiplexer, GameServer gameServer, LocalTime time, String timeIntervalMinutes) {

        super(multiplexer, gameServer, time);

        if (timeIntervalMinutes == null || timeIntervalMinutes.equals(""))
            throw new IllegalArgumentException("timeInterval cannot be null or empty!");

        this.timeIntervalMinutes = timeIntervalMinutes;

    }

    /**
     * Returns the warn text that is defined in the particular GameServer instance that is associated with the event and replaces any occurrence of $TIME with the actual number passed into the constructor.
     *
     * @return A warn command as a string specific to the game server
     */
    @Override
    protected String assembleCommand() {

        return gameServer.getWarnCommand().replace(WarnCommandEvent.REPLACE_STRING, this.timeIntervalMinutes);

    }

    @Override
    protected String eventString() {
        return "Warn Command";
    }

}
