package com.nchroniaris.ASC.client.model;

import com.nchroniaris.ASC.util.model.GameServer;

import java.time.LocalTime;

/**
 * Abstract class that represents a general event that can be run against a given game server. All events need to run, so that is what they have in common.
 */
public abstract class Event {

    // TODO: 2020-07-22 perhaps change LocalTime to LocalDateTime
    protected final GameServer gameServer;
    protected final LocalTime time;

    /**
     * Protected constructor for any Event. Since `Event` is abstract, this constructor cannot be used directly. Instead, it is merely used to explicitly instantiate both instance variables when instantiating a subclass of `Event`
     *
     * @param gameServer A GameServer object that describes the particular details of the game server that the event belongs to. Many of the attributes of this object are useful for subclasses of `Event`.
     * @param time       A LocalTime object that describes the exact time of day that the event should run.
     */
    protected Event(GameServer gameServer, LocalTime time) {

        if (gameServer == null || time == null)
            throw new IllegalArgumentException("Both gameServer and time should NOT be null!");

        this.gameServer = gameServer;
        this.time = time;

    }

    /**
     * Gets the time that the event is supposed to run. Useful for when you need to create a thread or a timer that waits to execute run().
     *
     * @return LocalTime object that is the precise time when the event should run.
     */
    public LocalTime getTime() {
        return time;
    }

    /**
     * A method that allows the event to actually take place. Every event subclass must implement such a method.
     *
     * @return A boolean result based on whether the run was successful or not.
     */
    public abstract boolean run();

}
