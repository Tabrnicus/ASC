package com.nchroniaris.ASC.client.model;

import com.nchroniaris.ASC.client.multiplexer.TerminalMultiplexer;
import com.nchroniaris.ASC.util.model.GameServer;

import java.time.LocalTime;

/**
 * A concrete subclass of Event. This particular event runs a particular command inside a GameServer's console session.
 */
public class RunCommandEvent extends Event {

    private final String commandToRun;

    /**
     * The main constructor for RunCommandEvent. As required by the superclass we must get a GameServer and a LocalTime. We additionally get a command to run in string form.
     *
     * @param multiplexer    A TerminalMultiplexer object offered as dependency injection. This can be any one of the classes that implements this interface.
     * @param gameServer   A GameServer object that describes the particular details of the game server that the event belongs to.
     * @param time         A LocalTime object that describes the exact time of day that the event should run.
     * @param commandToRun A string that represents the particular command to run.
     */
    public RunCommandEvent(TerminalMultiplexer multiplexer, GameServer gameServer, LocalTime time, String commandToRun) {

        super(multiplexer, gameServer, time);

        // This is to elegantly handle the case for which the command is null or if the command is empty.
        if (commandToRun == null || commandToRun.equals(""))
            throw new IllegalArgumentException("The command cannot be empty or null!");

        this.commandToRun = commandToRun;

    }

    /**
     * This is an extra constructor meant for subclasses that do not have a command to specify upon instantiation. Using this implies that the subclass will override assembleCommand().
     *
     * @param multiplexer    A TerminalMultiplexer object offered as dependency injection. This can be any one of the classes that implements this interface.
     * @param gameServer A GameServer object that describes the particular details of the game server that the event belongs to. Many of the attributes of this object are useful for subclasses of `Event`.
     * @param time       A LocalTime object that describes the exact time of day that the event should run.
     */
    protected RunCommandEvent(TerminalMultiplexer multiplexer, GameServer gameServer, LocalTime time) {

        super(multiplexer, gameServer, time);

        commandToRun = null;

    }

    @Override
    public final void run() {

        // Implementation of the Template pattern. In this case run() is the overarching algorithm and assembleCommand() is the
        String command = this.assembleCommand();

        // This check is for extra safety. If it is the case that a new subclass is created and does NOT override the default assembleCommand() behaviour, it can happen that we end up with a null or an empty string here. This can also happen if the subclass does not provide any actual command in assembleCommand().
        if (command == null || command.equals(""))
            throw new IllegalArgumentException("The command cannot be empty or null!");

        // Debug
        System.out.printf("pretend we are running the command (%s), at (%s), with the game-moniker combo (%s)%n", command, time.toString(), super.gameServer.getSessionName());

    }

    /**
     * This function is part of the template pattern implementation of this class. This is meant to be overridden in subclasses to provide a unique command to run. By default it just regurgitates the instance variable set in the constructor.
     *
     * @return A command meant to be run for the game server that is associated with this event.
     */
    protected String assembleCommand() {

        return this.commandToRun;

    }

}
