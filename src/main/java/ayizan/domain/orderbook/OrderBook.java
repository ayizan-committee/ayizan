package ayizan.domain.orderbook;

import ayizan.domain.Identifier;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;

public interface OrderBook
{
    public interface Matcher
    {

        boolean next(Order order);
    }

    String getSymbol();

    boolean isOpen();

    Order status(Identifier identifier);

    Order accept(Identifier identifier, Side side, long price, long quantity, long filledQuantity, TimeInForce timeInForce);

    Order cancel(Identifier identifier);

    void execute(Side side, Matcher matcher);

    long commit();
}
