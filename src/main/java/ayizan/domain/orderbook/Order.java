package ayizan.domain.orderbook;

import ayizan.domain.Executions.RejectReason;
import ayizan.domain.Identifier;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;

public interface Order
{
    Identifier getIdentifier();

    long getOrderId();

    Side getSide();

    TimeInForce getTimeInForce();

    long getPrice();

    long getQuantity();

    long getWorkingQuantity();

    long getFilledQuantity();

    long getCancelledQuantity();

    boolean isCompleted();

    boolean isRejected();

    RejectReason getRejectReason();

    Order fill(long quantity);

    Order cancel();
}
