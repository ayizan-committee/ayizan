package ayizan.domain.orderbook.limit;

import ayizan.domain.Executions.RejectReason;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.Identifier;
import ayizan.domain.orderbook.Order;

public class LimitOrder implements Order
{
    private Identifier identifier;
    private long orderId;
    private Side side;
    private TimeInForce timeInForce;
    private long price;
    private long quantity;
    private long workingQuantity;
    private long filledQuantity;
    private RejectReason rejectReason;

    LimitOrder _next;
    LimitOrder _previous;

    @Override
    public Identifier getIdentifier()
    {
        return identifier;
    }

    LimitOrder setIdentifier(final Identifier identifier)
    {
        this.identifier = identifier;
        return this;
    }

    @Override
    public long getOrderId()
    {
        return orderId;
    }

    LimitOrder setOrderId(final long orderId)
    {
        this.orderId = orderId;
        return this;
    }

    @Override
    public Side getSide()
    {
        return side;
    }

    LimitOrder setSide(final Side side)
    {
        this.side = side;
        return this;
    }

    @Override
    public TimeInForce getTimeInForce()
    {
        return timeInForce;
    }

    @Override
    public long getPrice()
    {
        return price;
    }

    LimitOrder setPrice(final long price)
    {
        this.price = price;
        return this;
    }

    LimitOrder setTimeInForce(final TimeInForce timeInForce)
    {
        this.timeInForce = timeInForce;
        return this;
    }

    @Override
    public long getQuantity()
    {
        return quantity;
    }

    LimitOrder setQuantity(final long quantity)
    {
        this.quantity = quantity;
        return this;
    }

    @Override
    public long getWorkingQuantity()
    {
        return workingQuantity;
    }

    LimitOrder setWorkingQuantity(final long workingQuantity)
    {
        this.workingQuantity = workingQuantity;
        return this;
    }

    @Override
    public long getFilledQuantity()
    {
        return filledQuantity;
    }

    LimitOrder setFilledQuantity(final long filledQuantity)
    {
        this.filledQuantity = filledQuantity;
        return this;
    }

    @Override
    public long getCancelledQuantity()
    {
        return quantity - workingQuantity - filledQuantity;
    }

    @Override
    public RejectReason getRejectReason()
    {
        return rejectReason;
    }

    LimitOrder setRejectReason(final RejectReason rejectReason)
    {
        this.rejectReason = rejectReason;
        return this;
    }

    @Override
    public boolean isCompleted()
    {
        return isRejected() || (workingQuantity == 0);
    }


    @Override
    public boolean isRejected()
    {
        return (rejectReason != RejectReason.NONE);
    }


    @Override
    public LimitOrder fill(final long quantity)
    {
        filledQuantity += quantity;
        workingQuantity -= quantity;
        return this;
    }

    @Override
    public LimitOrder cancel()
    {
        workingQuantity = 0;
        return this;
    }


    @Override
    public String toString()
    {
        return "LimitOrder{" +
                "identifier=" + identifier +
                ", orderId=" + orderId +
                ", side=" + side +
                ", price=" + getPrice() +
                ", quantity=" + quantity +
                ", filledQuantity=" + filledQuantity +
                ", workingQuantity=" + workingQuantity +
                ", cancelledQuantity=" + getCancelledQuantity() +
                ", timeInForce=" + timeInForce +
                ", rejectReason=" + rejectReason +
                '}';
    }
}

