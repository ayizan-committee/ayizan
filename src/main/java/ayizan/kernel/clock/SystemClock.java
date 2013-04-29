package ayizan.kernel.clock;


import java.util.concurrent.TimeUnit;

public class SystemClock extends Clock
{
    @Override
    protected long currentTime(final TimeUnit timeUnit)
    {
        return timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
}
