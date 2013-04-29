package ayizan.domain;

import static java.lang.Math.round;

public class StandardUnits
{
    public static final double ONE_LOT = 0.00000001;
    public static final double ONE_TICK = 0.00001;

    private StandardUnits(){}

    public static long toLots(final double value)
    {
        return round(value / ONE_LOT);
    }

    public static long toLots(final double value, final double lotSize)
    {
        return round(value * lotSize / ONE_LOT);
    }

    public static double fromLots(final long lots)
    {
        return lots * ONE_LOT;
    }

    public static long toTicks(final double value)
    {
        return round(value / ONE_TICK);
    }

    public static long toTicks(final double value, final double tickSize)
    {
        return round(value * tickSize / ONE_TICK);
    }

    public static double fromTicks(final long ticks)
    {
        return ticks * ONE_TICK;
    }
}
