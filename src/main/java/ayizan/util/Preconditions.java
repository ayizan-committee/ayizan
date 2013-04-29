package ayizan.util;

import java.util.Collection;

public final class Preconditions
{
    private static final Object[] EMPTY = new Object[0];

    private Preconditions() {}

    public static <T> T checkNotNull(final T reference)
    {
        return checkNotNull(reference, "reference is null", EMPTY);
    }

    public static <T> T checkNotNull(final T reference, final String template)
    {
        return checkNotNull(reference, template, EMPTY);
    }

    public static <T> T checkNotNull(final T reference, final String template, final Object... arguments)
    {
        if (reference != null) return reference;
        throw new IllegalArgumentException(format(template, arguments));
    }

    public static void checkArgument(final boolean condition)
    {
        checkArgument(condition, "check failure", EMPTY);
    }

    public static void checkArgument(final boolean condition, final String template, final Object... arguments)
    {
        if (condition) return;
        throw new IllegalArgumentException(format(template, arguments));
    }

    public static void checkState(final boolean condition)
    {
        if (condition) return;
        throw new IllegalStateException();
    }

    public static void checkState(final boolean condition, final String template, final Object... arguments)
    {
        if (condition) return;
        throw new IllegalStateException(format(template, arguments));
    }

    public static <T extends Collection> T checkNotEmpty(final T reference)
    {
        if (!reference.isEmpty()) return reference;
        throw new IllegalArgumentException("reference collection is empty");
    }

    private static String format(final String template, final Object... arguments)
    {
        return (arguments != EMPTY)? String.format(template, arguments) : template;
    }
}
