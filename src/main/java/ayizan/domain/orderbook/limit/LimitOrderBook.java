package ayizan.domain.orderbook.limit;

import ayizan.domain.Executions.RejectReason;
import ayizan.domain.Instruments.InstrumentSpecification;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.orderbook.ExecutionIdGenerator;
import ayizan.domain.Identifier;
import ayizan.domain.orderbook.OrderBook;
import ayizan.domain.orderbook.OrderIdGenerator;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static ayizan.domain.Sides.priceComparator;

public class LimitOrderBook implements OrderBook
{
    private final OrderIdGenerator orderIdGenerator;
    private final ExecutionIdGenerator executionIdGenerator;
    private final Map<Identifier, LimitOrder> limitOrdersByIdentifier;
    private final Map<Long, Limit> bidsByPrice;
    private final Map<Long, Limit> asksByPrice;
    private final InstrumentSpecification instrument;

    private enum State { ACCEPTING, EXECUTING, ADDING, CANCELING, WAITING }

    private Limit bestBid;
    private Limit bestAsk;
    private LimitOrder acceptLimitOrder;
    private LimitOrder cancelLimitOrder;
    private State state;

    public LimitOrderBook(final InstrumentSpecification instrumentSpecification)
    {
        this.limitOrdersByIdentifier = new HashMap<Identifier, LimitOrder>();
        this.bidsByPrice = new HashMap<Long, Limit>();
        this.asksByPrice = new HashMap<Long, Limit>();
        this.orderIdGenerator = new OrderIdGenerator();
        this.executionIdGenerator = new ExecutionIdGenerator();
        this.acceptLimitOrder = new LimitOrder();
        this.state = State.WAITING;

        this.instrument = instrumentSpecification;
    }

    public String getSymbol()
    {
        return instrument.getSymbol();
    }

    public InstrumentSpecification getInstrument()
    {
        return instrument;
    }

    public boolean isOpen()
    {
        return true;
    }

    @Override
    public LimitOrder status(final Identifier identifier)
    {
        return limitOrdersByIdentifier.get(identifier);
    }

    @Override
    public LimitOrder accept(final Identifier identifier,
                             final Side side,
                             final long price,
                             final long quantity,
                             final long filledQuantity,
                             final TimeInForce timeInForce)
    {
        state = State.ACCEPTING;
        return acceptLimitOrder.setIdentifier(identifier).
                setOrderId(orderIdGenerator.next()).
                setSide(side).
                setPrice(price).
                setQuantity(quantity).
                setWorkingQuantity(quantity - filledQuantity).
                setFilledQuantity(filledQuantity).
                setTimeInForce(timeInForce).
                setRejectReason(RejectReason.NONE);
    }

    @Override
    public LimitOrder cancel(final Identifier identifier)
    {
        final LimitOrder limitOrder = limitOrdersByIdentifier.get(identifier);
        if(limitOrder != null) {
            state = State.CANCELING;
            cancelLimitOrder = limitOrder;
        }
        return limitOrder;
    }

    @Override
    public void execute(final Side side, final Matcher matcher)
    {
        final State current  = state;
        switch(side) {
            case BUY:
                state = State.EXECUTING;
                execute(bestAsk, matcher);
                state = current;
                return;
            case SELL:
                state = State.EXECUTING;
                execute(bestBid, matcher);
                state = current;
                return;
        }
        throw new UnsupportedOperationException();
    }


    private void execute(final Limit limit, final Matcher matcher)
    {
        for(Limit depth = limit; depth != null; depth = depth._next) {
            for(LimitOrder order = depth._head; order != null; order = order._next) {
                final boolean next = matcher.next(order);
                if(order.isCompleted()) removeLimitOrder(order);
                if(!next) return;
            }
        }
    }

    @Override
    public long commit()
    {
        switch(state) {
            case ACCEPTING:
                state = (acceptLimitOrder.isRejected())? State.WAITING : State.ADDING;
                break;
            case CANCELING:
                state = State.WAITING;
                if(cancelLimitOrder != null && cancelLimitOrder.isCompleted()) {
                    removeLimitOrder(cancelLimitOrder);
                    cancelLimitOrder = null;
                }
                break;
            case ADDING:
                state = State.WAITING;
                if(!acceptLimitOrder.isCompleted()) {
                    addLimitOrder(acceptLimitOrder);
                    acceptLimitOrder = new LimitOrder();
                }
                break;
        }
        return executionIdGenerator.next();
    }

    private void addLimitOrder(final LimitOrder limitOrder)
    {
        final Side side = limitOrder.getSide();
        final Map<Long, Limit> limits = limits(side);
        final Limit limit = limits.get(limitOrder.getPrice());

        if(limit == null) updateBestBidAsk(side, addLimit(bestLimit(side), priceComparator(side), newLimit(limits, limitOrder.getPrice())).add(limitOrder));
        else limit.add(limitOrder);

        limitOrdersByIdentifier.put(limitOrder.getIdentifier(), limitOrder);
    }

    private void removeLimitOrder(final LimitOrder limitOrder)
    {
        final Side side = limitOrder.getSide();
        final Map<Long, Limit> limitDepths = limits(side);
        final Limit limit = limitDepths.get(limitOrder.getPrice()).remove(limitOrder);
        if(limit.isEmpty()) updateBestBidAsk(side, removeLimit(limitDepths, limit));

        limitOrdersByIdentifier.remove(limitOrder.getIdentifier());
    }

    private Limit updateBestBidAsk(final Side side, final Limit limit)
    {
        switch(side) {
            case BUY:
                if(bestBid == null || bestBid._previous != null) bestBid = limit;
                else if(bestBid == limit) bestBid = limit._next;
                break;
            case SELL:
                if(bestAsk == null || bestAsk._previous != null) bestAsk = limit;
                else if(bestAsk == limit) bestAsk = limit._next;
                break;
        }
        return limit;
    }

    private Limit addLimit(final Limit bestLimit, final Comparator<Long> priceComparator, final Limit newLimit)
    {
        if(bestLimit != null) {
            Limit candidate = bestLimit;
            do {
                if(priceComparator.compare(newLimit.getPrice(), candidate.getPrice()) == 1) {
                    newLimit._previous = candidate._previous;
                    candidate._previous = newLimit;
                    newLimit._next = candidate;
                    return newLimit;
                }
                if(candidate._next == null) {
                    candidate._next = newLimit;
                    newLimit._previous = candidate;
                    return newLimit;
                }
            }
            while((candidate = candidate._next) != null);
        }
        return newLimit;
    }

    private Limit removeLimit(final Map<Long, Limit> map, final Limit limit)
    {
        if(limit._previous != null) limit._previous._next = limit._next;
        if(limit._next != null) limit._next._previous = limit._previous;
        return map.remove(limit.getPrice());
    }


    private Limit newLimit(final Map<Long, Limit> map, final long price)
    {
        final Limit limit = new Limit().setPrice(price);
        map.put(price, limit);
        return limit;
    }


    private Map<Long, Limit> limits(final Side side)
    {
        switch(side) {
            case BUY:  return bidsByPrice;
            case SELL: return asksByPrice;
        }
        throw new UnsupportedOperationException();
    }

    private Limit bestLimit(final Side side)
    {
        switch(side) {
            case BUY:  return bestBid;
            case SELL: return bestAsk;
        }
        throw new UnsupportedOperationException();
    }
}
