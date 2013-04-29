package ayizan.domain.orderbook.limit;

public class Limit
{
    private long price;

    LimitOrder _head;
    LimitOrder _tail;
    Limit _next;
    Limit _previous;

    public long getPrice()
    {
        return price;
    }

    public Limit setPrice(final long price)
    {
        this.price = price;
        return this;
    }

    public Limit add(final LimitOrder limitOrder)
    {
        if(isEmpty()) _head = limitOrder;
        else {
            limitOrder._previous = _tail;
            _tail._next = limitOrder;
        }
        _tail = limitOrder;
        return this;
    }

    public Limit remove(final LimitOrder limitOrder)
    {
        if(_tail == limitOrder) _tail = limitOrder._previous;
        if(_head == limitOrder) _head = limitOrder._next;
        if(limitOrder._previous != null) limitOrder._previous = limitOrder._next;
        return this;
    }

    public boolean isEmpty()
    {
        return _head == null;
    }
}
