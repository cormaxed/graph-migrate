package omahoco.migrate.util;

public final class ThreadSleepDelayer implements Delayer {
    @Override
    public void delay(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            // Exit quietly if the thread is interrupted.
        }
    }
}
