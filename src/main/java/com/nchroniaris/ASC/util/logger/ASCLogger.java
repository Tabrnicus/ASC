package com.nchroniaris.ASC.util.logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class interacts with the filesystem and the standard output to log certain classes of events.
 */
public class ASCLogger {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd  HH:mm:ss";

    private final File logFile;

    /**
     * This creates an ASCLogger class that will interact with whatever file is specified by logFilePath
     *
     * @param logFilePath A valid file path that determines the location of a log file. This file need not exist as it will be created if not present.
     */
    public ASCLogger(String logFilePath) {

        if (logFilePath == null)
            throw new IllegalArgumentException("The file path cannot be null!");

        this.logFile = new File(logFilePath);

        // Basic error checking. This way if something goes wrong the program will definitely crash and print a more verbose and potentially more useful error message.
        if (this.logFile.exists()) {

            if (!this.logFile.isFile())
                throw new IllegalArgumentException(String.format("The file path specified (%s) is valid, but it is not a file! Please choose another file path.", logFilePath));

            if (!this.logFile.canWrite())
                throw new IllegalArgumentException(String.format("The file path specified (%s) is valid, but does not have correct write permissions! You can delete the file to regenerate it -- or use something like `chmod` to change the file permissions", logFilePath));

        }

    }

    /**
     * General purpose log command that logs both to the file and one of the standard output streams simultaneously. This is synchronized to prevent potential weirdness with multiple threads writing to a file at once.
     *
     * @param message The message to output, as a string.
     * @param stream  Any of the standard output streams to simultaneously print to.
     */
    private synchronized void log(String message, PrintStream stream) {

        // Each message will have a datetime readout in a particular format, followed by the message
        message = LocalDateTime.now().format(DateTimeFormatter.ofPattern(ASCLogger.DATETIME_FORMAT)) + " - " + message;

        // Prints to any of the standard output streams. This includes System.out and System.err.
        stream.println(message);

        // Use try with-resources block to create a BufferedWriter using the File object created upon instantiation. We want to append to this file, so we set the append flag to true.
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.logFile, true))) {

            // Since write() just writes the contents of the string to the buffer, we have to call newLine() to output a \n.
            bw.write(message);
            bw.newLine();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    /**
     * Logs an info style message to the file and standard output. These messages are prepended with "[INFO]". These messages are typically informational, useful for keeping track of what events executed at what time.
     *
     * @param message The contents of the message.
     */
    public synchronized void logInfo(String message) {

        this.log("[INFO]:\t\t" + message, System.out);

    }

    /**
     * Logs an warning style message to the file and standard error. These messages are prepended with "[WARNING]". These messages typically convey issues that should be resolved but don't currently massively impact the execution of the entire program.
     *
     * @param message The contents of the message.
     */
    public synchronized void logWarning(String message) {

        this.log("[WARNING]:\t" + message, System.err);

    }

    /**
     * Logs an error style message to the file and standard error. These messages are prepended with "[CRITICAL]". These messages typically convey critical issues or program-wide errors that impact the execution of the program as a whole.
     *
     * @param message The contents of the message.
     */
    public synchronized void logError(String message) {

        this.log("[CRITICAL]:\t" + message, System.err);

    }


}
