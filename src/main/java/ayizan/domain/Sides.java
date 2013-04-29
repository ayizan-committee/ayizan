package ayizan.domain;

import ayizan.domain.Orders.Side;

import java.util.Comparator;

public final class Sides
{
    private static final Comparator<Long> BUY_COMPARATOR = new Comparator<Long>()
    {
        @Override
        public int compare(final Long valueA, final Long valueB)
        {
            return valueA.compareTo(valueB);
        }
    };

    private static final Comparator<Long> SELL_COMPARATOR = new Comparator<Long>()
    {
        @Override
        public int compare(final Long valueA, final Long valueB)
        {
            return -valueA.compareTo(valueB);
        }
    };

    private Sides() {}

    public static Side flip(final Side side)
    {
        switch(side) {
            case BUY: return Side.SELL;
            case SELL: return Side.BUY;
        }
        throw new UnsupportedOperationException();
    }


    public static Comparator<Long> priceComparator(final Side side)
    {
        switch(side) {
            case BUY: return BUY_COMPARATOR;
            case SELL: return SELL_COMPARATOR;
        }
        throw new UnsupportedOperationException();
    }

}
