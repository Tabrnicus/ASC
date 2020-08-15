package com.nchroniaris.ASC.client.model;

import com.nchroniaris.ASC.client.multiplexer.TerminalMultiplexer;
import com.nchroniaris.ASC.util.model.GameServer;

import java.time.LocalTime;

/**
 * This is a small class that specializes an ExecuteFileEvent object. When we KNOW that a particular GameServer has to be started, we can just instantiate this instead of the base ExecuteFileEvent so that we don't have to deal with extracting the startFile ourselves.
 */
public class StartServerEvent extends ExecuteFileEvent {

    /**
     * Since the start file path is known ahead of time, there is no need to specify a file to run during instantiation
     * @param multiplexer    A TerminalMultiplexer object offered as dependency injection. This can be any one of the classes that implements this interface.
     * @param gameServer     A GameServer object that describes the particular details of the game server that the event belongs to.
     * @param time           A LocalTime object that describes the exact time of day that the event should run.
     */
    public StartServerEvent(TerminalMultiplexer multiplexer, GameServer gameServer, LocalTime time) {

        super(multiplexer, gameServer, time, gameServer.getStartFile());

    }

    @Override
    protected String eventString() {
        return "Start Server";
    }

}
