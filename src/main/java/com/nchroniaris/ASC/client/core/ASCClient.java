package com.nchroniaris.ASC.client.core;

import com.nchroniaris.ASC.client.database.ASCRepository;
import com.nchroniaris.ASC.client.model.Event;
import com.nchroniaris.ASC.client.schedule.EventScheduler;
import com.nchroniaris.ASC.util.model.GameServer;

import java.util.ArrayList;
import java.util.List;

public class ASCClient {

    private final boolean serverless;

    public ASCClient(boolean serverless) {

        this.serverless = serverless;

    }

    /**
     * The main method that starts the main loop of the client program.
     */
    public void start() {

        // Register events with ASCServer (Stub for now)
        if (!this.serverless)
            System.out.println("Server registration stub!");

        // TODO: 2020-07-30 perhaps dynamically inject this repo in order to facilitate testing
        ASCRepository repo = ASCRepository.getInstance();

        // Obtain all the servers from the database
        List<GameServer> serverList = repo.getAllGameServers();

        List<Event> eventList = new ArrayList<>();

        // For every GameServer that is set to autostart, get all their events
        for (GameServer gameServer : serverList)
            if (gameServer.isAutostart())
                eventList.addAll(repo.getAllEvents(gameServer));

        // Get new event scheduler and schedule all events
        EventScheduler scheduler = new EventScheduler();

        scheduler.scheduleEvents(eventList);

        // Here we add a shutdownHook in order to gracefully shutdown the client program if:
        //  1) The program exits normally
        //  2) A user interrupt is made, such as ^C.
        // This is according to the docs: https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#addShutdownHook-java.lang.Thread-
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
        try {

            scheduler.shutdown();

        } catch (InterruptedException e) {

            System.err.println("Main thread was interrupted while waiting for threads to execute. Scheduled events might not execute correctly!");
            e.printStackTrace();

        }

    }

}
