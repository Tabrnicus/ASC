package com.nchroniaris.ASC.client.console;

import com.nchroniaris.ASC.util.terminal.ASCTerminal;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

/**
 * This class is the main driver code for the console part of the program. Specifically, it uses the an ASCTerminal instance to read/write to the screen and provide interaction between the user and the database/program via some simple commands. It implements Runnable since it's meant to be run in a separate thread. As such, the run() call is blocking as it will run in an infinite I/O loop unless an exit command is input or the calling thread gets a kill signal.
 */
public class ASCConsole implements Runnable {

    private final ASCTerminal terminal;
    private ConsoleCallback callback;

    /**
     * Main constructor for ASCConsole.
     *
     * @param terminal A valid ASCTerminal instance. Messages will be read from and written to this object.
     * @param callback A valid ConsoleCallback implementation. Spontaneous user events will use the callback's methods.
     */
    public ASCConsole(ASCTerminal terminal, ConsoleCallback callback) {

        if (terminal == null || terminal.isClosed())
            throw new IllegalArgumentException("The terminal argument should not be null or closed!");

        this.terminal = terminal;

        if (callback == null)
            throw new IllegalArgumentException("The callback must have some implementation!");

        this.callback = callback;

    }

    @Override
    public void run() {

        // Temporary code for testing
        while (true) {

            try {

                String command = this.terminal.readLine().trim().toLowerCase();

                if (command.equals("exit"))
                    break;
                else if (command.equals("help"))
                    terminal.printDefault("Commands:\n\thelp\t\tShows this help text\n\texit\t\tExits the program");
                else
                    terminal.printSuccess("echo: " + command);

            } catch (UserInterruptException e) {

                // This catches ^C. We have to call shutdownNow() because JLine3's LineReader seems to swallow SIGINT if it's not caught from readLine(), unlike SIGTERM. We also return so that the regular shutdown() does not get called unnecessarily
                this.callback.shutdownNow();
                return;

            } catch (EndOfFileException ignored) {
                // Catching ^D. We don't want to do anything if this happens so we ignore this and keep the thread from crashing
            }

        }

        // "Normal" shutdown via the `exit` command.
        this.callback.shutdown();

    }

}
