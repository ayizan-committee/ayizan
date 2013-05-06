package ayizan.kernel.clock;

import java.util.concurrent.TimeUnit;

import static ayizan.util.Preconditions.checkNotNull;

public abstract class Clock
{
    private static Clock clock = new SystemClock();

    protected abstract void sleepFor(long duration, TimeUnit timeUnit);

    protected abstract long currentTime(TimeUnit timeUnit);

    public static long now(final TimeUnit timeUnit)
    {
        return clock.currentTime(timeUnit);
    }

    public static void sleep(final long duration, final TimeUnit timeUnit)
    {
        clock.sleepFor(duration, timeUnit);
    }

    public static void setClock(final Clock clock)
    {
        Clock.clock = checkNotNull(clock);
    }
}
