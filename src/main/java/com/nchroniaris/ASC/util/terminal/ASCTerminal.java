package com.nchroniaris.ASC.util.terminal;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.IOException;

/**
 * This class mainly serves as an abstraction for the Jline3 Terminal and LineReader classes. In general, it provides a limited interface for interacting with the JLine3 terminal, but crucially it hides all its implementation details which makes the interactions cleaner and will make it easier to swap out the terminal software via dependency injection if required in the future.
 */
public class ASCTerminal implements AutoCloseable {

    private static final String PROMPT = "> ";
    private static final String ERROR_CLOSED = "[ERROR] This ASCTerminal instance has been closed already! Please create a new instance in order to use this function.";

    // AttributedStyle constants for the different print "modes". These stay constant throughout the program so there is no need to re-instantiate them every time
    private static final AttributedStyle STYLE_DEFAULT = AttributedStyle.DEFAULT;

    private static final AttributedStyle STYLE_SUCCESS = new AttributedStyle()
            .foreground(AttributedStyle.GREEN);

    private static final AttributedStyle STYLE_WARNING = new AttributedStyle()
            .foreground(AttributedStyle.YELLOW);

    private static final AttributedStyle STYLE_ERROR = new AttributedStyle()
            .foreground(AttributedStyle.RED)
            .bold();

    private Terminal terminal;
    private LineReader reader;

    // This tracks the state of the Terminal object. If it is closed all of the useful instance methods will not work since it is not defined what happens when you run methods on the LineReader of a closed Terminal.
    private boolean closed;

    /**
     * Constructs an ASCTerminal instance, and will attempt to spawn a Jline3 system terminal instance. Through this class' interface you will be able to read and log messages with a certain style.
     * <p>
     * Upon instantiation, if Jline3 <b>cannot</b> find a system terminal to interact with it will create a dumb terminal. Such terminals have limited functionality and the program can behave weirdly if such a terminal is used. Thus it is not advisable to use a dumb terminal but it is in no means not allowed. Set <code>allowDumbTerminal</code> to true to allow dumb terminals as a fallback for system ones
     *
     * @param allowDumbTerminal If set to true, the constructor will allow a Jline3 dumb terminal to be used and will not throw an error. See above for details.
     */
    public ASCTerminal(boolean allowDumbTerminal) {

        try {

            // Get system terminal
            this.terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Get line reader associated with the terminal to perform I/O operations on
            this.reader = LineReaderBuilder.builder()
                    .terminal(this.terminal)
                    .build();

        } catch (IOException e) {

            System.err.println("There was an issue creating the terminal!");
            e.printStackTrace();
            System.exit(1);

        }

        // If allowDumbTerminal is true, this expression short circuits on the first statement and does not check the type. Otherwise, it checks the type and if seen to be any of the dumb terminal types, it throws the error
        if (!allowDumbTerminal && (terminal.getType().equals(Terminal.TYPE_DUMB) || terminal.getType().equals(Terminal.TYPE_DUMB_COLOR)))
            throw new IllegalStateException("A system terminal was not available to attach to! A dumb terminal can be created to avoid this error, but this can introduce some weird behaviour. Use a command line flag to enable such behaviour.");

        this.closed = false;

    }

    /**
     * This prints the contents of the string using the default terminal style (no changes)
     *
     * @param content The contents of the message
     */
    public synchronized void printDefault(String content) {

        this.printLine(new AttributedString(content, ASCTerminal.STYLE_DEFAULT));

    }

    /**
     * This prints the contents of the string using the "success" terminal style (green text)
     *
     * @param content The contents of the message
     */
    public synchronized void printSuccess(String content) {

        this.printLine(new AttributedString(content, ASCTerminal.STYLE_SUCCESS));

    }

    /**
     * This prints the contents of the string using the "warning" terminal style (yellow text)
     *
     * @param content The contents of the message
     */
    public synchronized void printWarning(String content) {

        this.printLine(new AttributedString(content, ASCTerminal.STYLE_WARNING));

    }

    /**
     * This prints the contents of the string using the "error" terminal style (red text, bold)
     *
     * @param content The contents of the message
     */
    public synchronized void printError(String content) {

        this.printLine(new AttributedString(content, ASCTerminal.STYLE_ERROR));

    }

    /**
     * This is the actual driver code for printing the line to the terminal. It takes an AttributedString (org.jline.utils) which is a string with a style already attached to it.
     *
     * @param content The contents of the message
     */
    private synchronized void printLine(AttributedString content) {

        // We can't use the terminal or reader resource if it's closed already
        if (this.closed)
            throw new IllegalStateException(ASCTerminal.ERROR_CLOSED);

        // This prints the attributed string's content to the terminal and redraws the entire terminal in order to preserve the readLine() input
        this.reader.printAbove(content);

    }

    /**
     * Reads the line using the default prompt string "> " and returns the contents of what the user input. This method is blocking on the calling thread.
     *
     * @return The user input as a result of the call.
     */
    public synchronized String readLine() throws UserInterruptException, EndOfFileException {

        // We can't use the terminal or reader resource if it's closed already
        if (this.closed)
            throw new IllegalStateException(ASCTerminal.ERROR_CLOSED);

        // This call is blocking on the calling thread
        return this.reader.readLine(ASCTerminal.PROMPT);

    }

    /**
     * Returns the status of the resource. This is useful for checking whether to use the resource or not
     *
     * @return true if the resource has been closed, false otherwise.
     */
    public synchronized boolean isClosed() {
        return this.closed;
    }

    @Override
    public synchronized void close() throws IOException {

        // It is not well defined in the JLine3 docs what happens when you attempt to close an already closed terminal. Therefore to avoid such a condition we test for it first
        if (this.closed)
            throw new IllegalStateException("Terminal has already been closed!");

        this.terminal.close();
        this.closed = true;
    }

}
