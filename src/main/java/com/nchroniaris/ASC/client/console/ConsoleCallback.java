package com.nchroniaris.ASC.client.console;

import com.nchroniaris.ASC.client.model.Event;

import java.util.concurrent.Future;

/**
 * This interface serves as a callback for the console. Specifically, each method would be a common action that the console needs to perform on the ASCClient.
 */
public interface ConsoleCallback {

    /**
     * Schedules an event (defined by the parameter) as soon as possible.
     *
     * @param event The event to schedule
     * @return A {@code Future} that represents the future status of the event
     */
    Future<?> scheduleEvent(Event event);

    /**
     * Shuts down the ASCClient gracefully, respecting events that are currently executing
     */
    void shutdown();

    /**
     * Shuts down the ASCClient as fast as possible, disregarding the status of events that may be in the middle of executing.
     */
    void shutdownNow();

}
