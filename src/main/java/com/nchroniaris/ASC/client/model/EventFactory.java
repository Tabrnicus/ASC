package com.nchroniaris.ASC.client.model;

import com.nchroniaris.ASC.client.core.ASCProperties;
import com.nchroniaris.ASC.client.multiplexer.TerminalMultiplexer;
import com.nchroniaris.ASC.util.model.GameServer;

import java.time.LocalTime;
import java.util.Arrays;

/**
 * Factory class that handles the instantiation of the correct event subclass based on the eventType number. Since I have required the events table to have a FK on eventType, this class is tightly coupled with the eventType table.
 */
public class EventFactory {

    /**
     * Given an event type id, this method will return the correctly instantiated Event.
     *
     * @param eventType The eventType id (integer) based on the eventType table in the DB.
     * @param server    The GameServer object for which this event is associated with
     * @param time      A LocalTime object describing when exactly this event should be run.
     * @param args      A String array representing the arguments used for different event types. This value should NOT be null, only empty at the very least.
     * @return A correctly instantiated subclass of Event that is appropriate for the previous parameters given.
     * @throws UnsupportedOperationException This is thrown when the eventType id is unrecognized.
     * @throws IllegalArgumentException      This is thrown when certain parameters are null.
     */
    public static Event buildEvent(int eventType, GameServer server, LocalTime time, String[] args) throws UnsupportedOperationException, IllegalArgumentException {

        if (server == null)
            throw new IllegalArgumentException("The server argument should NOT be null! Please check the database for the event you are building, there might be a null value where there shouldn't be.");

        else if (time == null)
            throw new IllegalArgumentException("The time argument should NOT be null! Please check the database for the event you are building, there might be a null value where there shouldn't be.");

        else if (args == null)
            throw new IllegalArgumentException("The args argument should NOT be null! Please check the database for the event you are building, there might be a null value where there shouldn't be.");

        // Get multiplexer from properties file which would have already been instantiated based on the configuration
        TerminalMultiplexer multiplexer = ASCProperties.getInstance().MULTIPLEXER;

        try {

            switch (eventType) {

                case 0:
                    return new ExecuteFileEvent(multiplexer, server, time, args[0], Arrays.copyOfRange(args, 1, args.length));

                case 1:
                    return new StartServerEvent(multiplexer, server, time);

                case 2:
                    return new RunCommandEvent(multiplexer, server, time, args[0]);

                case 3:
                    return new StopCommandEvent(multiplexer, server, time);

                case 4:
                    return new WarnCommandEvent(multiplexer, server, time, args[0]);

                default:
                    throw new UnsupportedOperationException(String.format("An event with the id (%d) does not exist! Consider rebuilding the database.", eventType));

            }

        } catch (ArrayIndexOutOfBoundsException e) {

            // The purpose of this try/catch block is to throw a more descriptive error that is in line with the rest of the function. An ArrayIndexOutOfBoundsException will only happen when the eventId is either 0, 1, or 3, AND args is a valid list but with zero elements.
            throw new IllegalArgumentException("The args parameter is valid, but does not have enough elements for the operation. Please verify that the args field in the database has the correct data for the event you are trying to run.");

        }

    }

}
