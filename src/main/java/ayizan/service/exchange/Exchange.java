package ayizan.service.exchange;

import ayizan.domain.Orders.CancelOrderSpecification;
import ayizan.domain.Orders.CancelReplaceOrderSpecification;
import ayizan.domain.Orders.OrderSpecification;
import ayizan.domain.Orders.OrderStatusSpecification;
import ayizan.domain.Orders.RejectReason;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.Identifier;
import ayizan.domain.orderbook.Order;
import ayizan.domain.orderbook.OrderBook;
import ayizan.domain.orderbook.limit.LimitOrderBook;
import ayizan.service.ExecutionVenue;
import ayizan.service.exchange.policy.AcceptPolicy.OrderAcceptPolicy;
import ayizan.service.exchange.policy.AcceptPolicy.ReplaceOrderAcceptPolicy;
import ayizan.service.exchange.policy.CancelPolicy;
import ayizan.service.exchange.policy.CancelPolicy.GoodTillCancelPolicy;
import ayizan.service.exchange.policy.CancelPolicy.ImmediateCancelPolicy;
import ayizan.service.exchange.policy.FillPolicy;
import ayizan.service.exchange.policy.FillPolicy.PartialFillPolicy;

public class Exchange implements ExecutionVenue
{
    private final ExecutionPublisher executionPublisher;

    private final OrderAcceptPolicy orderAcceptPolicy;
    private final ReplaceOrderAcceptPolicy replaceOrderAcceptPolicy;
    private final PartialFillPolicy partialFillPolicy;
    private final ImmediateCancelPolicy immediateCancelPolicy;
    private final GoodTillCancelPolicy goodTillCancelPolicy;

    private final OrderBook orderBook;

    public Exchange() //TODO: pass in orderBookRepository & scheduler & publisher topic
    {
        this.executionPublisher = new ExecutionPublisher();
        this.orderAcceptPolicy = OrderAcceptPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.replaceOrderAcceptPolicy = ReplaceOrderAcceptPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.partialFillPolicy = PartialFillPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.goodTillCancelPolicy = GoodTillCancelPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.immediateCancelPolicy = ImmediateCancelPolicy.allocate().setExecutionPublisher(executionPublisher);

        this.orderBook = new LimitOrderBook(); //TODO: this should be a repository <- how do we handle updates?
    }

    //TODO: add support for adding/removing instruments?? should repository register for messages directly???

    //TODO: should these be directly on orderBook <-
    //TODO: void snapshot(); void replay(); <- multievent

    @Override
    public void placeOrder(final OrderSpecification orderSpecification, final ExecutionCallback executionCallback)
    {
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(orderSpecification.getSymbol());
        final FillPolicy fillPolicy = lookupFillPolicy(orderSpecification.getTimeInForce());
        final CancelPolicy cancelPolicy = lookupCancelPolicy(orderSpecification.getTimeInForce());
        final Order order = orderAcceptPolicy.accept(orderBook,
                                                     new Identifier(orderSpecification.getId(), orderSpecification.getAttributionId()),
                                                     orderSpecification.getSide(),
                                                     orderSpecification.getPrice(),
                                                     orderSpecification.getQuantity(),
                                                     orderSpecification.getTimeInForce());
        if(isExecutable(order)) {
            fillPolicy.fill(orderBook, order);
            cancelPolicy.cancel(orderBook, order.getIdentifier(), order);
        }
        executionPublisher.commit();
    }

    /*
    @Override
    public void placeMarketOrder(final MarketOrderSpecification marketOrderSpecification, final ExecutionCallback executionCallback)
    {
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(marketOrderSpecification.getSymbol());
        final FillPolicy fillPolicy = lookupFillPolicy(marketOrderSpecification.getTimeInForce());
        final CancelPolicy cancelPolicy = lookupCancelPolicy(marketOrderSpecification.getTimeInForce());
        final Order order = marketOrderAcceptPolicy.accept(orderBook,
                                                           new Identifier(marketOrderSpecification.getId(), marketOrderSpecification.getAttributionId()),
                                                           marketOrderSpecification.getSide(),
                                                           marketOrderSpecification.getQuantity(),
                                                           marketOrderSpecification.getTimeInForce());
        if(isExecutable(order)) {
            fillPolicy.fill(orderBook, order);
            cancelPolicy.cancel(orderBook, order.getIdentifier(), order);
        }
        executionPublisher.commit();
    }
    */

    @Override
    public void cancelReplaceOrder(final CancelReplaceOrderSpecification cancelReplaceOrderSpecification, final ExecutionCallback executionCallback)
    {
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(cancelReplaceOrderSpecification.getSymbol());
        final Identifier identifier = new Identifier(cancelReplaceOrderSpecification.getId(), cancelReplaceOrderSpecification.getAttributionId());
        final Identifier cancelIdentifier = new Identifier(cancelReplaceOrderSpecification.getCancelId(), cancelReplaceOrderSpecification.getAttributionId());
        final Order order = replaceOrderAcceptPolicy.accept(orderBook,
                                                            identifier,
                                                            cancelIdentifier,
                                                            cancelReplaceOrderSpecification.getPrice(),
                                                            cancelReplaceOrderSpecification.getQuantity());

        if(isUnknown(order)) executionPublisher.publishReplaceRejection(identifier, cancelIdentifier, RejectReason.UNKNOWN_ORDER);
        else if(isExecutable(order)) {
            lookupFillPolicy(order.getTimeInForce()).fill(orderBook, order);
            lookupCancelPolicy(order.getTimeInForce()).cancel(orderBook, identifier, order);
        }
        executionPublisher.commit();
    }

    @Override
    public void cancelOrder(final CancelOrderSpecification cancelOrderSpecification, final ExecutionCallback executionCallback)
    {
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(cancelOrderSpecification.getSymbol());
        final Identifier identifier = new Identifier(cancelOrderSpecification.getId(), cancelOrderSpecification.getAttributionId());
        final Identifier cancelIdentifier = new Identifier(cancelOrderSpecification.getCancelId(), cancelOrderSpecification.getAttributionId());
        final Order order = orderBook.cancel(cancelIdentifier);

        if(isUnknown(order)) executionPublisher.publishCancelRejection(identifier, cancelIdentifier, RejectReason.UNKNOWN_ORDER);
        else if(isExecutable(order)) immediateCancelPolicy.cancel(orderBook, identifier, order);
        executionPublisher.commit();
    }

    @Override
    public void orderStatus(final OrderStatusSpecification orderStatusSpecification, final ExecutionCallback executionCallback)
    {
        final OrderBook orderBook = lookupOrderBook(orderStatusSpecification.getSymbol());
        final Identifier identifier = new Identifier(orderStatusSpecification.getId(), orderStatusSpecification.getAttributionId());
        final Identifier statusIdentifier = new Identifier(orderStatusSpecification.getStatusId(), orderStatusSpecification.getAttributionId());
        final Order order = orderBook.status(statusIdentifier);

        executionPublisher.start(executionCallback);
        if(isUnknown(order)) executionPublisher.publishOrderStatusRejection(identifier, statusIdentifier, RejectReason.UNKNOWN_ORDER);
        else executionPublisher.publishOrderStatusExecution(identifier, order);
        executionPublisher.commit();
    }

    private boolean isExecutable(final Order order)
    {
        return order != null && !order.isCompleted();
    }

    private boolean isUnknown(final Order order)
    {
        return order == null;
    }

    private OrderBook lookupOrderBook(final String symbol)
    {
        executionPublisher.withOrderBook(orderBook);
        return orderBook;
    }

    private FillPolicy lookupFillPolicy(final TimeInForce timeInForce)
    {
        switch(timeInForce) {
            case IMMEDIATE_OR_CANCEL:
            case GOOD_FOR_DAY:
            case GOOD_TILL_CANCEL:
                return partialFillPolicy;
            case FILL_OR_KILL:
                break;
        }
        throw new UnsupportedOperationException();
    }

    private CancelPolicy lookupCancelPolicy(final TimeInForce timeInForce)
    {
        switch(timeInForce) {
            case GOOD_FOR_DAY:
            case GOOD_TILL_CANCEL:
                return goodTillCancelPolicy;
            case IMMEDIATE_OR_CANCEL:
            case FILL_OR_KILL:
                return immediateCancelPolicy;
        }
        throw new UnsupportedOperationException();
    }

}
