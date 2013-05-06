package ayizan.kernel.clock;


import java.util.concurrent.TimeUnit;

import static ayizan.util.Exceptions.swallow;

public class SystemClock extends Clock
{

    @Override
    protected void sleepFor(final long duration, final TimeUnit timeUnit)
    {
        try {
            Thread.sleep(timeUnit.toMillis(duration));
        }
        catch(final InterruptedException e) {
            swallow(e);
        }
    }

    @Override
    protected long currentTime(final TimeUnit timeUnit)
    {
        return timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
}
