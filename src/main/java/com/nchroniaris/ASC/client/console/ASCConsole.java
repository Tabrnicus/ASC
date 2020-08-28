package com.nchroniaris.ASC.client.console;

import com.nchroniaris.ASC.client.schedule.EventScheduler;
import com.nchroniaris.ASC.util.terminal.ASCTerminal;

/**
 * This class is the main driver code for the console part of the program. Specifically, it uses the an ASCTerminal instance to read/write to the screen and provide interaction between the user and the database/program via some simple commands. It implements Runnable since it's meant to be run in a separate thread. As such, the run() call is blocking as it will run in an infinite I/O loop unless an exit command is input or the calling thread gets a kill signal.
 */
public class ASCConsole implements Runnable {

    private final ASCTerminal terminal;
    private final EventScheduler eventScheduler;

    /**
     * Main constructor for ASCConsole.
     * @param terminal       A valid ASCTerminal instance. Messages will be read from and written to this object.
     * @param eventScheduler A valid EventScheduler instance. Spontaneous user events will be scheduled to this object.
     */
    public ASCConsole(ASCTerminal terminal, EventScheduler eventScheduler) {

        if (terminal == null || terminal.isClosed())
            throw new IllegalArgumentException("The terminal argument should not be null or closed!");

        if (eventScheduler == null)    // TODO: 2020-08-27 ( ... || eventScheduler.isShutdown()) <-- implement this
            throw new IllegalArgumentException("The eventScheduler cannot be null or closed!");

        this.terminal = terminal;
        this.eventScheduler = eventScheduler;

    }

    @Override
    public void run() {

        // Temporary code for testing
        while (true) {

            String command = this.terminal.readLine();

            if (command.trim().toLowerCase().equals("exit"))
                break;

            terminal.printSuccess("echo: " + command);

        }

    }

}
