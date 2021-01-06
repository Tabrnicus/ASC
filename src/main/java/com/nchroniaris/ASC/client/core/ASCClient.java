package com.nchroniaris.ASC.client.core;

import com.nchroniaris.ASC.client.concurrent.SynchronizedFutureList;
import com.nchroniaris.ASC.client.console.ASCConsole;
import com.nchroniaris.ASC.client.console.ConsoleCallback;
import com.nchroniaris.ASC.client.database.ASCRepository;
import com.nchroniaris.ASC.client.model.Event;
import com.nchroniaris.ASC.client.schedule.EventScheduler;
import com.nchroniaris.ASC.util.model.GameServer;
import com.nchroniaris.ASC.util.terminal.ASCTerminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final ClientOptions options;

    private EventScheduler scheduler;
    private ScheduledExecutorService consoleExecutor;

    private final SynchronizedFutureList synchronizedFutureList;

    // This is to gracefully exit the main scheduling loop, when this is set to false. Atomic because this will be modified by multiple threads.
    private final AtomicBoolean continueScheduling;

    // Define a callback implementation for ASCConsole. This is done in favor of making ASCClient implement ConsoleCallback since I don't want the console to have a reference to this object, since that can cause GC headaches
    private final ConsoleCallback consoleCallback = new ConsoleCallback() {
        @Override
        public Future<?> scheduleEvent(Event event) {

            return ASCClient.this.scheduler.submitEventNow(event);

        }

        @Override
        public void shutdown() {

            ASCClient.this.shutdown();

        }

        @Override
        public void shutdownNow() {

            ASCClient.this.shutdownNow();

        }
    };

    public ASCClient(ClientOptions options) {

        // If options are null (which they shouldn't be) create a default set. Otherwise clone the object to prevent it be mutated further.
        if (options == null)
            this.options = new ClientOptions();
        else
            this.options = new ClientOptions(options);

        this.scheduler = null;
        this.consoleExecutor = null;

        this.synchronizedFutureList = new SynchronizedFutureList();

        this.continueScheduling = new AtomicBoolean(true);

    }

    /**
     * The main method that starts the main loop of the client program in accordance with the options provided to the class upon construction.
     */
    public void start() {

        ASCProperties properties = ASCProperties.getInstance();

        // Here we add a shutdownHook in order to gracefully shutdown the client program if:
        //  1) The program exits normally
        //  2) A user interrupt is made, such as ^C.
        // This is according to the docs: https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#addShutdownHook-java.lang.Thread-
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownNow));

        // Spawn a terminal using try-with-resources. In any case, the close() method should be invoked but in the case of a kill signal it's not guaranteed (see note at end of try block)
        try (ASCTerminal terminal = new ASCTerminal(this.options.allowDumbTerminal)) {

            // Attach terminal (user facing UI) to the logger so that it shows up there
            properties.LOGGER.setTerminal(terminal);

            // Register events with ASCServer (Stub for now)
            if (!this.options.serverless)
                properties.LOGGER.logInfo("Server registration stub!");

            // Spawn EventScheduler and a console instance. We pass consoleCallback to ASCConsole in order to allow it to schedule manual async events requested by the user. We do this after the instantiation of EventScheduler() in order to guarantee that the callback has a non-null scheduler to call.
            this.scheduler = new EventScheduler();
            ASCConsole console = new ASCConsole(terminal, this.consoleCallback);

            // We want the console to be on its own thread so that it doesn't block the main thread with IO
            this.consoleExecutor = Executors.newSingleThreadScheduledExecutor();
            this.consoleExecutor.execute(console);

            // Main loop for scheduling events. This should continue until the user decides to exit or the program gets a kill signal.
            if (!this.options.consoleOnly) {

                // The atomic boolean controls whether to keep scheduling more events when the first batch has completed.
                while (this.continueScheduling.get()) {

                    // Schedule events, and pass the list of futures returned by the scheduler to the synced future list so that we can call waitForCompletion() on it. If another thread calls the cancelAllEvents() method the waitForCompletion() will return immediately since it will process all the remaining futures and realize they are cancelled.
                    this.synchronizedFutureList.clearAndAddAll(this.scheduleEvents());
                    this.synchronizedFutureList.waitForCompletion();

                }

            }

            // NOTE: Anything after this point is fair game to not get executed by the main thread (most of the time). As far as I can tell, the JVM forcibly terminates the main thread *at some point* when receiving an interrupt, like SIGTERM. This means that while shutdownNow() is definitely invoked, the rest of the code here might not be. This isn't a problem because shutdownNow() pretty much does what the following does, but what might be a problem is that the implicit finally{} block in this try-with-resources usually does not execute, meaning that ASCTerminal is not closed properly. I have no idea how "destructive" not closing the terminal is when the JVM is shutting down, but I imagine it's not that bad. I am not sure how else to restructure this such that the terminal is guaranteed to shut down on SIGTERM.

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

    /**
     * Gracefully tries to exit the program by cancelling all the events currently scheduled but not yet executed.
     */
    public void shutdown() {

        ASCProperties.getInstance().LOGGER.logInfo("Shutting down (user request)...");

        // Disable looping and cancel all events
        this.continueScheduling.set(false);
        this.synchronizedFutureList.cancelEvents(false);

    }

    /**
     * Attempts to exit out of the program as fast as possible, while *trying* to be as graceful as possible. This should ideally only be called from a shutdown hook.
     */
    public void shutdownNow() {

        ASCProperties.getInstance().LOGGER.logInfo("Shutting down (OS request)...");

        // Disable looping and cancel all events, forcefully
        this.continueScheduling.set(false);
        this.synchronizedFutureList.cancelEvents(true);

        // Attempt to shutdown the executors immediately, not waiting after calling
        if (this.scheduler != null)
            this.scheduler.shutdownNow();

        if (this.consoleExecutor != null)
            this.consoleExecutor.shutdownNow();

    }

    /**
     * Uses the database classes to get all the game servers and their associated events. It will then schedule (using EventScheduler) any and all events from game servers who have been flagged to be autostarted.
     *
     * @return A list of {@code Future}s that each represent the future state of each scheduled event.
     */
    private List<Future<?>> scheduleEvents() {

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

        return futureList;

    }

}
