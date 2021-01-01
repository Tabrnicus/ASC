package com.nchroniaris.ASC.client.concurrent;

import com.nchroniaris.ASC.client.core.ASCProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class essentially acts as a thread-safe wrapper for a {@code List<Future<?>>}. Its main purpose is to store a list of futures so that it can primarily provide a method for blocking on the completion of said futures, all while being thread-aware to updates to the list.
 */
public class SynchronizedFutureList {

    // This list will hold Futures of currently scheduled events. It has an accompanying lock, which is used to avoid threads colliding when it's updated. futureList should NOT be used on its own without the lock, which is why this is in a separate class.
    private final List<Future<?>> futureList;
    private final ReentrantReadWriteLock futureListLock;

    public SynchronizedFutureList() {

        this.futureList = new ArrayList<>();
        this.futureListLock = new ReentrantReadWriteLock();

    }

    /**
     * This method blocks until all the stored events (represented as Futures) are complete.
     */
    public void waitForCompletion() {

        // Get a read lock for the future list. This forces any thread trying to write to the futurelist to wait for the list to process. Other reading threads will not be blocked.
        Lock readLock = this.futureListLock.readLock();
        readLock.lock();

        // Use a try/finally for safety
        try {

            // This keeps track of how many futures have been detected as cancelled so we can print one log message instead of a bunch upon a mass cancellation. However, this prevents a log from being made *exactly* when the future is cancelled, which is a tradeoff that seems worth it to keep the logs clean.
            int numCancelled = 0;

            // Since we scheduled an array of events, we got back a list of futures that each represent one event. By calling .get() on each of them, we block the main thread and effectively wait until all the work is finished before exiting out of this method.
            for (Future<?> future : this.futureList) {

                try {

                    // Block until the future completes and returns a result (the result is not useful so we ignore it)
                    future.get();

                } catch (InterruptedException e) {

                    e.printStackTrace();
                    System.err.println("Thread was interrupted while waiting for a future! This can cause the program to prematurely schedule the next batch of events!");

                } catch (ExecutionException e) {

                    e.printStackTrace();
                    ASCProperties.getInstance().LOGGER.logError("An event threw an exception! The stacktrace has been printed.");

                } catch (CancellationException e) {

                    numCancelled++;

                }

            }

            // If there were any cancelled events, log that.
            if (numCancelled > 0)
                ASCProperties.getInstance().LOGGER.logWarning(numCancelled + " events were cancelled!");

        } finally {

            // Release the read lock for the future list. This potentially unblocks any thread that's waiting to write, assuming this is the last read lock to be unlocked.
            readLock.unlock();

        }

    }

    /**
     * This method cancels all the stored events (represented as Futures). This is useful to call when you want to unblock the thread that is calling waitForCompletion().
     *
     * @param forceCancel Set to true if you want to also cancel events that are in the middle of executing. Otherwise it will only cancel events that are scheduled but have not yet started executing. Useful when you need to suddenly shut down
     */
    public void cancelEvents(boolean forceCancel) {

        // Get a read lock for the future list. This forces any thread trying to write to the futureList to wait for the list to process. Other reading threads will not be blocked.
        Lock readLock = this.futureListLock.readLock();
        readLock.lock();

        // Use a try/finally for safety
        try {

            for (Future<?> future : this.futureList)
                future.cancel(forceCancel);

        } finally {

            // Release the read lock for the future list. This potentially unblocks any thread that's waiting to write, asuming this is the last read lock to be unlocked.
            readLock.unlock();

        }

    }

    /**
     * Clears the list of events (represented as Futures), blocking until no thread is executing one of waitForCompletion() or cancelEvents().
     * Note: Consider using clearAndAddAll() if you need to perform both a clear and add operation sequentially.
     */
    public void clear() {

        Lock writeLock = this.futureListLock.writeLock();
        writeLock.lock();

        // Use a try/finally for safety
        try {

            this.futureList.clear();

        } finally {

            writeLock.unlock();

        }

    }

    /**
     * Adds all the events (represented as Futures) in the parameter to the internal list, blocking until no thread is executing one of waitForCompletion() or cancelEvents().
     * Note: Consider using clearAndAddAll() if you need to perform both a clear and add operation sequentially.
     *
     * @param futureList A list of futures to be added. Contents are copied from this list to the internal representation.
     */
    public void addAll(List<Future<?>> futureList) {

        Lock writeLock = this.futureListLock.writeLock();
        writeLock.lock();

        // Use a try/finally for safety
        try {

            this.futureList.addAll(futureList);

        } finally {

            writeLock.unlock();

        }

    }

    /**
     * Clears the list of events (represented as Futures) and replaces it with a list of new Futures. The behaviour of this method is slightly different than running clear() and addAll() sequentially, as this method will do both operations atomically. In other words, compared to running the aforementioned methods in sequence, this method will avoid the situation where a thread is able to read this list after it's been emptied, but before it's been filled again.
     *
     * @param futureList A list of futures to be added. Contents are copied from this list to the internal representation.
     */
    public void clearAndAddAll(List<Future<?>> futureList) {

        // We acquire the write lock here in order to ensure that both functions are atomic. Since the lock is reentrant on the same thread, we can call .lock() and .unlock() many times and the writelock will only be released until the last .unlock() has been called. This can be inspected with .getWriteHoldCount(). Specifically, we lock, lock, unlock, lock, unlock, and unlock again.
        Lock writeLock = this.futureListLock.writeLock();
        writeLock.lock();

        // Use a try/finally for safety
        try {

            this.clear();
            this.addAll(futureList);

        } finally {

            writeLock.unlock();

        }

    }

}
