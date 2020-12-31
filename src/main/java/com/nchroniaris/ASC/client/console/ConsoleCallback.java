package com.nchroniaris.ASC.client.console;

/**
 * This interface serves as a callback for the console. Specifically, each method would be a common action that the console needs to perform on the ASCClient.
 */
public interface ConsoleCallback {

    /**
     * Shuts down the ASCClient gracefully, respecting events that are currently executing
     */
    void shutdown();

    /**
     * Shuts down the ASCClient as fast as possible, disregarding the status of events that may be in the middle of executing.
     */
    void shutdownNow();

}
