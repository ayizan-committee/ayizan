package ayizan.service.exchange;

import ayizan.domain.Executions.AcceptExecution;
import ayizan.domain.Executions.CancelExecution;
import ayizan.domain.Executions.OrderState.Builder;
import ayizan.domain.Executions.RejectReason;
import ayizan.domain.Executions.ReplaceExecution;
import ayizan.domain.Executions.StatusExecution;
import ayizan.domain.Executions.TradeExecution;
import ayizan.domain.Instruments.InstrumentSpecification;
import ayizan.domain.Orders.CancelOrderSpecificationOrBuilder;
import ayizan.domain.Orders.CancelReplaceOrderSpecificationOrBuilder;
import ayizan.domain.Orders.OrderStatusSpecificationOrBuilder;
import ayizan.domain.Orders.PlaceOrderSpecificationOrBuilder;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.Identifier;
import ayizan.domain.orderbook.Order;
import ayizan.domain.orderbook.OrderBook;
import ayizan.domain.orderbook.limit.LimitOrderBook;
import ayizan.kernel.clock.Clock;
import ayizan.service.ExecutionVenue;
import ayizan.service.exchange.policy.AcceptPolicy.OrderAcceptPolicy;
import ayizan.service.exchange.policy.AcceptPolicy.ReplaceOrderAcceptPolicy;
import ayizan.service.exchange.policy.CancelPolicy;
import ayizan.service.exchange.policy.CancelPolicy.GoodTillCancelPolicy;
import ayizan.service.exchange.policy.CancelPolicy.ImmediateCancelPolicy;
import ayizan.service.exchange.policy.FillPolicy;
import ayizan.service.exchange.policy.FillPolicy.PartialFillPolicy;

import java.util.concurrent.TimeUnit;


//TODO: implement snapshot & replay api
//TODO: implement time api
public class Exchange implements ExecutionVenue
{
    public static final InstrumentSpecification DEFAULT_INSTRUMENT = InstrumentSpecification.newBuilder().
             setSymbol("XXX.GOOG").
             setTickSize(0.01).
             setLotSize(1).
             setMultiplier(1).
             build();

    private final ExecutionPublisher executionPublisher;

    private final OrderAcceptPolicy orderAcceptPolicy;
    private final ReplaceOrderAcceptPolicy replaceOrderAcceptPolicy;
    private final PartialFillPolicy partialFillPolicy;
    private final ImmediateCancelPolicy immediateCancelPolicy;
    private final GoodTillCancelPolicy goodTillCancelPolicy;

    private final OrderBook orderBook;

    public Exchange()
    {
        this.executionPublisher = new ExecutionPublisher();
        this.orderAcceptPolicy = OrderAcceptPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.replaceOrderAcceptPolicy = ReplaceOrderAcceptPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.partialFillPolicy = PartialFillPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.goodTillCancelPolicy = GoodTillCancelPolicy.allocate().setExecutionPublisher(executionPublisher);
        this.immediateCancelPolicy = ImmediateCancelPolicy.allocate().setExecutionPublisher(executionPublisher);

        this.orderBook = new LimitOrderBook(DEFAULT_INSTRUMENT); //TODO: replace with OrderBookRepository
    }

    @Override
    public void placeOrder(final PlaceOrderSpecificationOrBuilder placeOrderSpecification, final ExecutionCallback executionCallback)
    {
        RejectReason rejectReason;
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(placeOrderSpecification.getSymbol());
        if(!isRejected(rejectReason = isOpen(orderBook))) {
            final FillPolicy fillPolicy = lookupFillPolicy(placeOrderSpecification.getTimeInForce());
            final CancelPolicy cancelPolicy = lookupCancelPolicy(placeOrderSpecification.getTimeInForce());
            final Order order = orderAcceptPolicy.accept(orderBook,
                                                         new Identifier(placeOrderSpecification.getId(), placeOrderSpecification.getAttributionId()),
                                                         placeOrderSpecification.getSide(),
                                                         placeOrderSpecification.getPrice(),
                                                         placeOrderSpecification.getQuantity(),
                                                         placeOrderSpecification.getTimeInForce());
            if(!isRejected(rejectReason = isValid(order)) && isExecutable(order)) {
                fillPolicy.fill(orderBook, order);
                cancelPolicy.cancel(orderBook, order.getIdentifier(), order);
            }
        }

        if(isRejected(rejectReason)) executionPublisher.publishAcceptRejection(placeOrderSpecification, rejectReason);
        executionPublisher.commit();
    }

    @Override
    public void cancelReplaceOrder(final CancelReplaceOrderSpecificationOrBuilder cancelReplaceOrderSpecification, final ExecutionCallback executionCallback)
    {
        RejectReason rejectReason;
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(cancelReplaceOrderSpecification.getSymbol());
        if(!isRejected(rejectReason = isOpen(orderBook))) {

            final Identifier identifier = new Identifier(cancelReplaceOrderSpecification.getId(), cancelReplaceOrderSpecification.getAttributionId());
            final Identifier cancelIdentifier = new Identifier(cancelReplaceOrderSpecification.getCancelId(), cancelReplaceOrderSpecification.getAttributionId());
            final Order order = replaceOrderAcceptPolicy.accept(orderBook,
                                                                identifier,
                                                                cancelIdentifier,
                                                                cancelReplaceOrderSpecification.getPrice(),
                                                                cancelReplaceOrderSpecification.getQuantity());
            if(!isRejected(rejectReason = isValid(order)) && isExecutable(order)) {
                lookupFillPolicy(order.getTimeInForce()).fill(orderBook, order);
                lookupCancelPolicy(order.getTimeInForce()).cancel(orderBook, identifier, order);
            }
        }

        if(isRejected(rejectReason)) executionPublisher.publishReplaceRejection(cancelReplaceOrderSpecification, rejectReason);
        executionPublisher.commit();
    }

    @Override
    public void cancelOrder(final CancelOrderSpecificationOrBuilder cancelOrderSpecification, final ExecutionCallback executionCallback)
    {
        RejectReason rejectReason;
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(cancelOrderSpecification.getSymbol());
        if(!isRejected(rejectReason = isOpen(orderBook))) {
            final Identifier identifier = new Identifier(cancelOrderSpecification.getId(), cancelOrderSpecification.getAttributionId());
            final Identifier cancelIdentifier = new Identifier(cancelOrderSpecification.getCancelId(), cancelOrderSpecification.getAttributionId());
            final Order order = orderBook.cancel(cancelIdentifier);

            if(!isRejected(rejectReason = isValid(order)) && isExecutable(order)) {
                immediateCancelPolicy.cancel(orderBook, identifier, order);
            }
        }
        if(isRejected(rejectReason)) executionPublisher.publishCancelRejection(cancelOrderSpecification, rejectReason);
        executionPublisher.commit();
    }

    @Override
    public void orderStatus(final OrderStatusSpecificationOrBuilder orderStatusSpecification, final ExecutionCallback executionCallback)
    {
        RejectReason rejectReason;
        executionPublisher.start(executionCallback);

        final OrderBook orderBook = lookupOrderBook(orderStatusSpecification.getSymbol());
        if(!isRejected(rejectReason = isOpen(orderBook))) {

            final Identifier identifier = new Identifier(orderStatusSpecification.getId(), orderStatusSpecification.getAttributionId());
            final Identifier statusIdentifier = new Identifier(orderStatusSpecification.getStatusId(), orderStatusSpecification.getAttributionId());
            final Order order = orderBook.status(statusIdentifier);
            if(!isRejected(rejectReason = isValid(order))) {
                executionPublisher.publishOrderStatusExecution(identifier, orderStatusSpecification.getSymbol(), order);
            }
        }

        if(isRejected(rejectReason)) executionPublisher.publishOrderStatusRejection(orderStatusSpecification, rejectReason);
        executionPublisher.commit();
    }

    private boolean isRejected(final RejectReason rejectReason)
    {
        return !RejectReason.NONE.equals(rejectReason);
    }


    private RejectReason isValid(final Order order)
    {
        return (order == null)? RejectReason.UNKNOWN_ORDER :
               (isRejected(order.getRejectReason()))? order.getRejectReason() :
               RejectReason.NONE;
    }

    private boolean isExecutable(final Order order)
    {
        return order != null && !order.isCompleted();
    }

    private RejectReason isOpen(final OrderBook orderBook)
    {
        return (orderBook == null)? RejectReason.UNKNOWN_INSTRUMENT :
               (!orderBook.isOpen())? RejectReason.ORDER_BOOK_CLOSED :
                RejectReason.NONE;
    }

    private OrderBook lookupOrderBook(final String symbol)
    {
        return (orderBook.getInstrument().getSymbol().equals(symbol))? orderBook : null;
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


    public static class ExecutionPublisher
    {
        private AcceptExecution.Builder acceptExecution;
        private TradeExecution.Builder tradeExecution;
        private ReplaceExecution.Builder replaceExecution;
        private CancelExecution.Builder cancelExecution;
        private StatusExecution.Builder statusExecution;

        private ExecutionCallback executionCallback;

        public ExecutionPublisher()
        {
            this.acceptExecution = AcceptExecution.newBuilder();
            this.tradeExecution = TradeExecution.newBuilder();
            this.replaceExecution = ReplaceExecution.newBuilder();
            this.cancelExecution = CancelExecution.newBuilder();
            this.statusExecution = StatusExecution.newBuilder();
        }

        public ExecutionPublisher start(final ExecutionCallback executionCallback)
        {
            this.executionCallback = executionCallback;
            executionCallback.start();
            return this;
        }

        public ExecutionPublisher commit()
        {
            executionCallback.commit();
            executionCallback = null;
            return this;
        }

        public ExecutionPublisher publishAcceptExecution(final long executionId, final String symbol, final Order acceptOrder)
        {
            executionCallback.notify(toAcceptExecution(executionId, symbol, acceptOrder));
            return this;
        }

        public ExecutionPublisher publishAcceptRejection(final PlaceOrderSpecificationOrBuilder placeOrderSpecification, final RejectReason rejectReason)
        {
            executionCallback.notify(toAcceptRejection(placeOrderSpecification, rejectReason));
            return this;
        }

        public ExecutionPublisher publishTradeExecution(final long executionId, final String symbol, final Order aggressiveOrder, final Order passiveOrder, final long tradePrice, final long tradeQuantity)
        {
            executionCallback.notify(toTradeExecution(executionId, symbol, aggressiveOrder, tradePrice, tradeQuantity));
            executionCallback.notify(toTradeExecution(executionId, symbol, passiveOrder, tradePrice, tradeQuantity));
            return this;
        }

        public ExecutionPublisher publishReplaceExecution(final long executionId, final String symbol, final Order cancelOrder, final Order replaceOrder)
        {
            executionCallback.notify(toReplaceExecution(executionId, symbol, cancelOrder, replaceOrder));
            return this;
        }

        public ExecutionPublisher publishReplaceRejection(final CancelReplaceOrderSpecificationOrBuilder cancelReplaceOrderSpecification, final RejectReason rejectReason)
        {
            executionCallback.notify(toReplaceRejection(cancelReplaceOrderSpecification, rejectReason));
            return this;
        }

        public ExecutionPublisher publishCancelExecution(final long executionId, final String symbol, final Identifier identifier, final Order cancelOrder)
        {
            executionCallback.notify(toCancelExecution(executionId, symbol, identifier, cancelOrder));
            return this;
        }


        public ExecutionPublisher publishCancelRejection(final CancelOrderSpecificationOrBuilder cancelOrderSpecification, final RejectReason rejectReason)
        {
            executionCallback.notify(toCancelRejection(cancelOrderSpecification, rejectReason));
            return this;
        }

        public ExecutionPublisher publishOrderStatusExecution(final Identifier identifier, final String symbol, final Order statusOrder)
        {
            executionCallback.notify(toStatusExecution(identifier, symbol, statusOrder));
            return this;
        }

        public ExecutionPublisher publishOrderStatusRejection(final OrderStatusSpecificationOrBuilder orderStatusSpecification, final RejectReason rejectReason)
        {
            executionCallback.notify(toStatusRejection(orderStatusSpecification, rejectReason));
            return this;
        }

        private AcceptExecution.Builder toAcceptExecution(final long executionId, final String symbol, final Order order)
        {
            toOrderState(acceptExecution.getOrderBuilder(), order);
            return acceptExecution.
                    setSymbol(symbol).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(order.getIdentifier().getId()).
                    setAttributionId(order.getIdentifier().getAttributionId()).
                    setExecutionId(executionId).
                    setRejectReason(order.getRejectReason());
        }


        private AcceptExecution.Builder toAcceptRejection(final PlaceOrderSpecificationOrBuilder placeOrderSpecification, final RejectReason rejectReason)
        {
            acceptExecution.getOrderBuilder().
                    setOrderId(0).
                    setSide(placeOrderSpecification.getSide()).
                    setTimeInForce(placeOrderSpecification.getTimeInForce()).
                    setPrice(placeOrderSpecification.getPrice()).
                    setQuantity(placeOrderSpecification.getQuantity()).
                    setWorkingQuantity(0).
                    setFilledQuantity(0);

            return acceptExecution.
                    setExecutionId(0).
                    setSymbol(placeOrderSpecification.getSymbol()).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(placeOrderSpecification.getId()).
                    setAttributionId(placeOrderSpecification.getAttributionId()).
                    setRejectReason(rejectReason);
        }


        private TradeExecution.Builder toTradeExecution(final long executionId, final String symbol, final Order order, final long tradePrice, final long tradeQuantity)
        {
            toOrderState(tradeExecution.getOrderBuilder(), order);
            return tradeExecution.
                    setSymbol(symbol).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(order.getIdentifier().getId()).
                    setAttributionId(order.getIdentifier().getAttributionId()).
                    setExecutionId(executionId).
                    setTradePrice(tradePrice).
                    setTradeQuantity(tradeQuantity);
        }

        private ReplaceExecution.Builder toReplaceExecution(final long executionId, final String symbol, final Order cancelOrder, final Order replaceOrder)
        {
            toOrderState(replaceExecution.getCancelOrderBuilder(), cancelOrder);
            toOrderState(replaceExecution.getReplaceOrderBuilder(), replaceOrder);
            return replaceExecution.
                    setSymbol(symbol).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(replaceOrder.getIdentifier().getId()).
                    setAttributionId(replaceOrder.getIdentifier().getAttributionId()).
                    setExecutionId(executionId).
                    setCancelId(cancelOrder.getIdentifier().getId());
        }

        private ReplaceExecution.Builder toReplaceRejection(final CancelReplaceOrderSpecificationOrBuilder cancelReplaceOrderSpecification, final RejectReason rejectReason)
        {
            return replaceExecution.
                    setSymbol(cancelReplaceOrderSpecification.getSymbol()).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(cancelReplaceOrderSpecification.getId()).
                    setAttributionId(cancelReplaceOrderSpecification.getAttributionId()).
                    setCancelId(cancelReplaceOrderSpecification.getCancelId()).
                    setRejectReason(rejectReason);
        }

        private CancelExecution.Builder toCancelExecution(final long executionId, final String symbol, final Identifier identifer, final Order order)
        {
            toOrderState(cancelExecution.getOrderBuilder(), order);
            return cancelExecution.
                    setSymbol(symbol).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(identifer.getId()).
                    setAttributionId(order.getIdentifier().getAttributionId()).
                    setExecutionId(executionId).
                    setCancelId(order.getIdentifier().getId());
        }

        private CancelExecution.Builder toCancelRejection(final CancelOrderSpecificationOrBuilder cancelOrderSpecification, final RejectReason rejectReason)
        {
            return cancelExecution.
                    setSymbol(cancelOrderSpecification.getSymbol()).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(cancelOrderSpecification.getId()).
                    setAttributionId(cancelOrderSpecification.getAttributionId()).
                    setCancelId(cancelOrderSpecification.getCancelId()).
                    setRejectReason(rejectReason);
        }

        private StatusExecution.Builder toStatusExecution(final Identifier identifier, final String symbol, final Order order)
        {
            toOrderState(statusExecution.getOrderBuilder(), order);
            return statusExecution.
                    setSymbol(symbol).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(identifier.getId()).
                    setAttributionId(order.getIdentifier().getAttributionId()).
                    setStatusId(order.getIdentifier().getId());
        }

        private StatusExecution.Builder toStatusRejection(final OrderStatusSpecificationOrBuilder orderStatusSpecification, final RejectReason rejectReason)
        {
            return statusExecution.
                    setSymbol(orderStatusSpecification.getSymbol()).
                    setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                    setId(orderStatusSpecification.getId()).
                    setAttributionId(orderStatusSpecification.getAttributionId()).
                    setStatusId(orderStatusSpecification.getStatusId()).
                    setRejectReason(rejectReason);
        }

        private Builder toOrderState(final Builder orderState, final Order order)
        {
            return orderState.
                    setOrderId(order.getOrderId()).
                    setSide(order.getSide()).
                    setTimeInForce(order.getTimeInForce()).
                    setPrice(order.getPrice()).
                    setQuantity(order.getQuantity()).
                    setWorkingQuantity(order.getWorkingQuantity()).
                    setFilledQuantity(order.getFilledQuantity());
        }
    }
}
