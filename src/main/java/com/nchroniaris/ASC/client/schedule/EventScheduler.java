package com.nchroniaris.ASC.client.schedule;

import com.nchroniaris.ASC.client.model.Event;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class handles scheduling events to a particular Java thread pool using the LocalTime as a reference for when that particular event must execute.
 */
public class EventScheduler {

    private final ScheduledExecutorService executorService;

    private static final Duration DURATION_24H = Duration.ofHours(24);

    /**
     * Creates a new EventScheduler by instantiating a version of ScheduledExecutorService using the factory methods in Executors. Use the methods in here to schedule events to your heart's content, but **MAKE SURE TO CALL shutdown() when you are done.**
     */
    public EventScheduler() {

        // We get a **single thread** scheduled executor in order to facilitate the events that we have to run. The reason why I chose this option over any other thread pool is because the events that we are running are generally few and far between. Since you cannot instantiate a scheduled version of a cached thread pool, it does not make much sense to keep many threads alive. This may change later, but given that most if not all events (even custom ones by the user) can finish in under a second one thread should be plenty.
        // The more important part is having some sort of structure to *schedule* tasks.
        this.executorService = Executors.newSingleThreadScheduledExecutor();

    }

    /**
     * This is the main function of the class: That being to take in a list of events and schedule them using a previously instantiated thread pool.
     *
     * @param eventList List of Event objects to schedule
     */
    public void scheduleEvents(List<Event> eventList) {

        for (Event event : eventList) {

            // We call the executorService and schedule each event (which implements Runnable) using the LocalTime in the event to calculate the precise duration (to the millisecond) between now and the time specified in the LocalTime.
            this.executorService.schedule(
                    event,
                    this.calculateDelay(event.getTime(), LocalTime.now()),
                    TimeUnit.MILLISECONDS
            );

        }

    }

    /**
     * Call this method if you want to immediately execute a particular event using the scheduler. Cannot guarantee that the passed event will start executing at the time of the call, as there could be other jobs in the queue. This behaviour should be very rare though
     *
     * @param event The event to be executed
     */
    public void executeEventNow(Event event) {

        this.executorService.execute(event);

    }

    /**
     * When you are done with the EventScheduler instance, call this method. **This method is blocking, as it waits until the ScheduledExecutorService is shutdown**. If it is not called, the ScheduledExecutorService will be perpetually alive and will block the main thread forever.
     *
     * @throws InterruptedException Passes the Exception that could be thrown by awaitTermination().
     */
    public void shutdown() throws InterruptedException {

        this.executorService.shutdown();
        this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

    }

    /**
     * Calculates the amount of time (in ms) between `currentTime` and `scheduledTime`, "rounded" (see comment in function) to the next day. In other words, this is the shortest duration possible that you can ADD to `currentTime` which will make it will run at `scheduledTime`.
     *
     * @param scheduledTime LocalTime object that represents at what hour/minute/second of the day the event has to run.
     * @param currentTime   LocalTime object that represents the hour/minute/second at the time of the call
     * @return A long representing the number of milliseconds between the two events.
     */
    private long calculateDelay(LocalTime scheduledTime, LocalTime currentTime) {

        /*

        This comment will be my attempt to (not formally) prove the algorithm I implement below. I have convinced myself it is correct but since I haven't proved it formally I cannot be 100% sure. But it seems pretty sound. (That's all that matters, right? :P)

        In order to understand this method, imagine a number line where the left boundary is time 00:00:00 and the right boundary is time 23:59:59. For context, the intended way that that the scheduler and by extension the program is meant to be used is by scheduling all events given ONCE per 24 hour period. Therefore, whenever all the events are executed once, the expectation is that they will be scheduled once more for the next 24 hour period.

        I'll define a couple of terms that'll make it a bit easier to illustrate why we're doing the things in this method:

        start: the exact time that the program STARTS scheduling events.
        end:   the end of the 24h period (exactly 24 hours after the start marker). To reduce confusion, I'll label it as such but with one second before (so 00:00 and 23:59, 01:00 and 00:59, and so on).
        event: a particular event in question that we want to schedule.

           00:00                         23:59
            /                             /
           |-----------------------------|
         start                          end

        Suppose we have en event at 01:00. If we start scheduling at 00:00, by deduction we need a duration of exactly 1 hour. Using our diagrams this is how it is illustrated:

           00:00  01:00                  23:59
            /      /                      /
           |######|----------------------|          (the hashes represent the actual duration between events)
         start  event                   end

        Now suppose that we start adding events at some other time other than 00:00 -- say 02:00. In this case, we have a window between 00:00 and the start time. Any events in this window will be BEFORE the start time and will thusly get the WRONG duration when using the Duration static functions. What we want is to have a duration that will wait until 23:59 and then wait the additional time from 00:00 to the event time. Using our diagrams:

           00:00  01:59 02:00                    23:59
            /        /  /                 /
           |####|XXXX|-|#################|          (the X's represent the duration given by the static functions)
                \  end start
               01:00
               event

         The duration given by the static functions is technically correct at -1h, but won't make sense for our program. As explained above, we need the duration that "wraps around" to the event. In this case, we need the duration shown with hashes, which is precisely 23h.

         **Since we know we are working in a 24 hour cycle, we can do very simple arithmetic to convert the "correct" duration of -1h to the actually correct duration with is 23h. Precisely, we must do something like `24h - abs(duration)` to get the correct result.**

         You can extrapolate this information with any pair of start/end times and you'll notice that the further right the start/end time is, the bigger the window grows for that if statement below to be TRUE. In other words, the further right the start/end time, the more events on the left need to have the `24h - abs(duration)` operation performed.

         Hopefully this cleared up some confusion with this method.

         */

        // First we want to get the absolute delta between the two events. It is possible that scheduledTime < currentTime so then the duration would be negative. This is the purpose of the .abs() call.
        Duration delta = Duration.between(currentTime, scheduledTime).abs();

        // If it so happens that scheduledTime < currentTime, we KNOW that this event MUST be scheduled the NEXT DAY, so we must take the inverse of the duration by subtracting the 24 hour time with the delta.
        if (scheduledTime.isBefore(currentTime))
            delta = EventScheduler.DURATION_24H.minus(delta);

        return delta.toMillis();

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        // https://stackoverflow.com/questions/171952/is-there-a-destructor-for-java
        // As pointed out in ^^, this is for sanity checking in case the caller does not call shutdown().

        if (!this.executorService.isShutdown()) {
            System.err.println("[ERROR] There was an EventScheduler instance created, but not shutdown! Please call shutdown() after you are done with the class to avoid any weirdness.");

            this.shutdown();

        }

    }

}
