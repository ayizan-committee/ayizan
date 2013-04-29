package ayizan.util;


public final class Exceptions
{
    private Exceptions(){}

    public static void swallow(final Throwable throwable)
    {
        throwable.printStackTrace();
        if (throwable instanceof Error) throw (Error) throwable;
    }

    public static RuntimeException rethrow(final Throwable throwable)
    {
        if (throwable instanceof Error) throw (Error) throwable;
        throw (throwable instanceof RuntimeException)? (RuntimeException) throwable : new RuntimeException(throwable);
    }
}
