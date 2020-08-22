package com.nchroniaris.ASC.client.core;

import com.nchroniaris.ASC.client.database.ASCRepository;
import com.nchroniaris.ASC.client.model.Event;
import com.nchroniaris.ASC.client.schedule.EventScheduler;
import com.nchroniaris.ASC.util.model.GameServer;

import java.util.ArrayList;
import java.util.List;

public class ASCClient {

    /**
     * A class representing all the flags that can be set for the program's execution. Upon calling ASCClient.start() it will analyze the options and execute properly.
     */
    public static class ClientOptions {

        public boolean serverless;

        /**
         * Default constructor. Inserts default values for all primitives.
         */
        public ClientOptions() {

            this.serverless = false;

        }

        /**
         * Copy constructor
         *
         * @param clientOptions The ClientOptions class you want to duplicate.
         */
        public ClientOptions(ClientOptions clientOptions) {

            if (clientOptions == null)
                throw new IllegalArgumentException("ClientOptions cannot be null!");

            this.serverless = clientOptions.serverless;

        }

    }

    public final ClientOptions options;

    public ASCClient(ClientOptions options) {

        // If options are null (which they shouldn't be) create a default set. Otherwise clone the object to prevent it be mutated further.
        if (options == null)
            this.options = new ClientOptions();
        else
            this.options = new ClientOptions(options);

    }

    /**
     * The main method that starts the main loop of the client program in accordance with the options provided to the class upon construction.
     */
    public void start() {

        ASCProperties properties = ASCProperties.getInstance();

        properties.LOGGER.logInfo(String.format("Client started with serverless value `%s`.", this.options.serverless ? "true" : "false"));

        // Register events with ASCServer (Stub for now)
        if (!this.options.serverless)
            System.out.println("Server registration stub!");

        // TODO: 2020-07-30 perhaps dynamically inject this repo in order to facilitate testing
        ASCRepository repo = ASCRepository.getInstance();

        // Obtain all the servers from the database
        properties.LOGGER.logInfo("Querying all game servers...");
        List<GameServer> serverList = repo.getAllGameServers();
        properties.LOGGER.logInfo(String.format("Got %d game servers.", serverList.size()));

        List<Event> eventList = new ArrayList<>();

        // For every GameServer that is set to autostart, get all their events
        properties.LOGGER.logInfo("Querying all events...");

        for (GameServer gameServer : serverList)
            if (gameServer.isAutostart())
                eventList.addAll(repo.getAllEvents(gameServer));

        properties.LOGGER.logInfo(String.format("Got %d events.", eventList.size()));

        // Get new event scheduler and schedule all events
        EventScheduler scheduler = new EventScheduler();

        properties.LOGGER.logInfo("Scheduling all events...");
        scheduler.scheduleEvents(eventList);

        // Here we add a shutdownHook in order to gracefully shutdown the client program if:
        //  1) The program exits normally
        //  2) A user interrupt is made, such as ^C.
        // This is according to the docs: https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#addShutdownHook-java.lang.Thread-
        // TODO: 2020-08-19 Replace with SignalHandler so that the JVM doesn't shut itself down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            System.out.println("Shutdown hook invoked, shutting down gracefully...");

            try {

                scheduler.shutdownNow();

            } catch (InterruptedException e) {

                System.err.println("The shutdown process was interrupted! Scheduled events might not execute correctly!");
                e.printStackTrace();

            }

        }));

        // Block main thread by waiting for all the threads to finish executing
        // TODO: 2020-07-30 Run main algorithm in a loop
        properties.LOGGER.logInfo("Done. Currently running...");

        try {

            scheduler.shutdown();

        } catch (InterruptedException e) {

            System.err.println("Main thread was interrupted while waiting for threads to execute. Scheduled events might not execute correctly!");
            e.printStackTrace();

        }

    }

}
