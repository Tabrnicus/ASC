package com.nchroniaris.ASC.client.core;

import com.nchroniaris.ASC.client.console.ASCConsole;
import com.nchroniaris.ASC.client.database.ASCRepository;
import com.nchroniaris.ASC.client.model.Event;
import com.nchroniaris.ASC.client.schedule.EventScheduler;
import com.nchroniaris.ASC.util.model.GameServer;
import com.nchroniaris.ASC.util.terminal.ASCTerminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ASCClient {

    /**
     * A class representing all the flags that can be set for the program's execution. Upon calling ASCClient.start() it will analyze the options and execute properly.
     */
    public static class ClientOptions {

        public boolean serverless;
        public boolean allowDumbTerminal;
        public boolean consoleOnly;


        /**
         * Default constructor. Inserts default values for all primitives.
         */
        public ClientOptions() {

            this.serverless = false;
            this.allowDumbTerminal = false;
            this.consoleOnly = false;

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
            this.allowDumbTerminal = clientOptions.allowDumbTerminal;
            this.consoleOnly = clientOptions.consoleOnly;

        }

    }

    public final ClientOptions options;

    public EventScheduler scheduler;
    public ScheduledExecutorService consoleExecutor;

    public ASCClient(ClientOptions options) {

        // If options are null (which they shouldn't be) create a default set. Otherwise clone the object to prevent it be mutated further.
        if (options == null)
            this.options = new ClientOptions();
        else
            this.options = new ClientOptions(options);

        // This is here temporarily so that the threads get actually shut down
        // Here we add a shutdownHook in order to gracefully shutdown the client program if:
        //  1) The program exits normally
        //  2) A user interrupt is made, such as ^C.
        // This is according to the docs: https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#addShutdownHook-java.lang.Thread-
        // TODO: 2020-08-19 Replace with SignalHandler so that the JVM doesn't shut itself down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            System.out.println("Shutdown hook invoked, shutting down gracefully...");

            try {

                ASCClient.this.scheduler.shutdownNow();

            } catch (InterruptedException e) {

                System.err.println("The shutdown process was interrupted! Scheduled events might not execute correctly!");
                e.printStackTrace();

            }

        }));

    }

    /**
     * The main method that starts the main loop of the client program in accordance with the options provided to the class upon construction.
     */
    public void start() {

        ASCProperties properties = ASCProperties.getInstance();

        // Spawn a terminal using try-with-resources. In any case, the close() method will be invoked even if there is a kill signal
        try (ASCTerminal terminal = new ASCTerminal(this.options.allowDumbTerminal)) {

            // Attach terminal (user facing UI) to the logger so that it shows up there
            properties.LOGGER.setTerminal(terminal);

            properties.LOGGER.logInfo(String.format("Client started with serverless value `%s`.", this.options.serverless ? "true" : "false"));

            // Register events with ASCServer (Stub for now)
            if (!this.options.serverless)
                System.out.println("Server registration stub!");

            // Spawn EventScheduler and a console instance. We pass eventScheduler to ASCConsole in order to allow it to schedule manual async events requested by the user.
            this.scheduler = new EventScheduler();
            ASCConsole console = new ASCConsole(terminal, this.scheduler);

            // We want the console to be on its own thread so that it doesn't interrupt the main thread
            this.consoleExecutor = Executors.newSingleThreadScheduledExecutor();
            this.consoleExecutor.execute(console);

            if (!this.options.consoleOnly)
                this.scheduleEventsAndWait();

            // We wait for the console thread to shut down before shutting down the eventScheduler because the former relies on the latter.
            this.consoleExecutor.shutdown();
            this.consoleExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

            // This call is blocking
            this.scheduler.shutdown();

        } catch (IOException e) {

            // TODO: 2020-08-26 implement
            e.printStackTrace();

        } catch (InterruptedException e) {

            System.err.println("The shutdown process was interrupted! Scheduled events might not execute correctly!");
            e.printStackTrace();

        }

    }

    private void scheduleEventsAndWait() {

        ASCProperties properties = ASCProperties.getInstance();

        // TODO: 2020-07-30 perhaps dynamically inject this repo in order to facilitate testing
        ASCRepository repo = ASCRepository.getInstance();

        // Obtain all the servers from the database
        properties.LOGGER.logInfo("Querying all game servers...");
        List<GameServer> serverList = repo.getAllGameServers();
        properties.LOGGER.logInfo(String.format("Got %d game servers.", serverList.size()));

        List<Event> eventList = new ArrayList<>();

        // For every GameServer that is set to autostart, get all their events
        properties.LOGGER.logInfo("Querying all events...");

        // Only start servers that have the autostart flag enabled
        for (GameServer gameServer : serverList)
            if (gameServer.isAutostart())
                eventList.addAll(repo.getAllEvents(gameServer));

        properties.LOGGER.logInfo(String.format("Got %d events.", eventList.size()));

        properties.LOGGER.logInfo("Scheduling all events...");
        List<Future<?>> futureList = this.scheduler.scheduleEvents(eventList);

        properties.LOGGER.logInfo("Done. Currently running...");

        // Since we scheduled an array of events, we got back a list of futures that each represent one event. By calling .get() on each of them, we block the main thread and effectively wait until all the work is finished before exiting out of this method.
        for (Future<?> future: futureList) {
            try {

                future.get();

            } catch (InterruptedException e) {

                System.err.println("Main thread was interrupted while waiting for threads to execute. Scheduled events might not execute correctly!");
                e.printStackTrace();

            } catch (ExecutionException e) {

                System.err.println("An event threw an exception! The program will continue, but note that not this event did not execute correctly. This may cause future errors.");
                e.printStackTrace();

            }
        }

    }

    private void shutdownNow() {
        System.out.println("Stub shutdownNow()");
    }

}
