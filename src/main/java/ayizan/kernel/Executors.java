package ayizan.kernel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static ayizan.util.Exceptions.swallow;

public final class Executors
{
    private static final long DEFAULT_TIMEOUT = 250;

    private Executors() {}

    public static ExecutorService newExecutor()
    {
        return java.util.concurrent.Executors.newCachedThreadPool();
    }

    public static void closeQuietly(final ExecutorService executorService)
    {
        if(executorService != null) {
            executorService.shutdown();
            if(!await(executorService)) {
                executorService.shutdownNow();
                await(executorService);
            }
        }
    }

    private static boolean await(final ExecutorService executorService)
    {
        try {
            return executorService.awaitTermination(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        catch(final Exception e) {
            swallow(e);
            return false;
        }
    }
}
