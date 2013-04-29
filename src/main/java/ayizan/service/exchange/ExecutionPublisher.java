package ayizan.service.exchange;

import ayizan.domain.Identifier;
import ayizan.domain.Orders.AcceptExecution;
import ayizan.domain.Orders.CancelExecution;
import ayizan.domain.Orders.Execution;
import ayizan.domain.Orders.Execution.Type;
import ayizan.domain.Orders.OrderState;
import ayizan.domain.Orders.OrderState.Builder;
import ayizan.domain.Orders.RejectReason;
import ayizan.domain.Orders.ReplaceExecution;
import ayizan.domain.Orders.StatusExecution;
import ayizan.domain.Orders.TradeExecution;
import ayizan.domain.orderbook.Order;
import ayizan.domain.orderbook.OrderBook;
import ayizan.kernel.clock.Clock;
import ayizan.service.ExecutionVenue.ExecutionCallback;

import java.util.concurrent.TimeUnit;

public class ExecutionPublisher
{
    private final Execution.Builder execution;
    private final AcceptExecution.Builder acceptExecution;
    private final TradeExecution.Builder tradeExecution;
    private final CancelExecution.Builder cancelExecution;
    private final ReplaceExecution.Builder replaceExecution;
    private final StatusExecution.Builder statusExecution;

    private final OrderState.Builder oldOrderState;
    private final OrderState.Builder newOrderState;


    private ExecutionCallback executionCallback;
    private OrderBook orderBook;

    public ExecutionPublisher()
    {
        this.oldOrderState = OrderState.newBuilder();
        this.newOrderState = OrderState.newBuilder();
        this.acceptExecution = AcceptExecution.newBuilder();
        this.tradeExecution = TradeExecution.newBuilder();
        this.cancelExecution = CancelExecution.newBuilder();
        this.replaceExecution = ReplaceExecution.newBuilder();
        this.statusExecution = StatusExecution.newBuilder();
        this.execution = Execution.newBuilder();
    }

    public ExecutionPublisher withOrderBook(final OrderBook orderBook)
    {
        this.orderBook = orderBook;
        return this;
    }

    public ExecutionPublisher start(final ExecutionCallback executionCallback)
    {
        this.executionCallback = executionCallback;
        return this;
    }

    public ExecutionPublisher commit()
    {
        executionCallback = null;
        orderBook = null;
        return this;
    }

    public ExecutionPublisher publishAcceptExecution(final long executionId, final Order acceptOrder)
    {
        if(isEnabled()) notifyAcceptExecution(asAcceptExecution(executionId, acceptOrder));
        return this;
    }

    public ExecutionPublisher publishTradeExecution(final long executionId, final Order aggressiveOrder, final Order passiveOrder, final long tradePrice, final long tradeQuantity)
    {
        if(isEnabled()) {
            notifyTradeExecution(asTradeExecution(executionId, aggressiveOrder, tradePrice, tradeQuantity));
            notifyTradeExecution(asTradeExecution(executionId, passiveOrder, tradePrice, tradeQuantity));
        }
        return this;
    }

    public ExecutionPublisher publishReplaceExecution(final long executionId, final Order cancelOrder, final Order replaceOrder)
    {
        if(isEnabled()) notifyReplaceExecution(asReplaceExecution(executionId, cancelOrder, replaceOrder));
        return this;
    }

    public ExecutionPublisher publishReplaceRejection(final Identifier identifier, final Identifier cancelIdentifier, final RejectReason rejectReason)
    {
        if(isEnabled()) notifyReplaceExecution(asReplaceRejection(identifier, cancelIdentifier, rejectReason));
        return this;
    }

    public ExecutionPublisher publishCancelExecution(final long executionId, final Identifier identifier, final Order cancelOrder)
    {
        if(isEnabled()) notifyCancelExecution(asCancelExecution(executionId, identifier, cancelOrder));
        return this;
    }


    public ExecutionPublisher publishCancelRejection(final Identifier identifier, final Identifier cancelIdentifier, final RejectReason rejectReason)
    {
        if(isEnabled()) notifyCancelExecution(asCancelRejection(identifier, cancelIdentifier, rejectReason));
        return this;
    }

    public ExecutionPublisher publishOrderStatusExecution(final Identifier identifier, final Order statusOrder)
    {
        if(isEnabled()) notifyStatusExecution(asStatusExecution(identifier, statusOrder));
        return this;
    }

    public ExecutionPublisher publishOrderStatusRejection(final Identifier identifier, final Identifier statusIdentifier, final RejectReason rejectReason)
    {
        if(isEnabled()) notifyStatusExecution(asStatusRejection(identifier, statusIdentifier, rejectReason));
        return this;
    }

    private boolean isEnabled()
    {
        return (executionCallback != null && orderBook != null);
    }

    private void notifyAcceptExecution(final AcceptExecution.Builder acceptExecution)
    {
        try {
            executionCallback.notify(execution.setType(Type.ACCEPT).setAccept(acceptExecution).build());
        }
        finally {
            execution.clear();
        }
    }

    private void notifyTradeExecution(final TradeExecution.Builder tradeExecution)
    {
        try {
            executionCallback.notify(execution.setType(Type.TRADE).setTrade(tradeExecution).build());
        }
        finally {
            execution.clear();
        }
    }

    private void notifyCancelExecution(final CancelExecution.Builder cancelExecution)
    {
        try {
            executionCallback.notify(execution.setType(Type.CANCEL).setCancel(cancelExecution).build());
        }
        finally {
            execution.clear();
        }
    }

    private void notifyReplaceExecution(final ReplaceExecution.Builder replaceExecution)
    {
        try {
            executionCallback.notify(execution.setType(Type.REPLACE).setReplace(replaceExecution).build());
        }
        finally {
            execution.clear();
        }
    }

    private void notifyStatusExecution(final StatusExecution.Builder statusExecution)
    {
        try {
            executionCallback.notify(execution.setType(Type.STATUS).setStatus(statusExecution).build());
        }
        finally {
            execution.clear();
        }
    }

    private AcceptExecution.Builder asAcceptExecution(final long executionId, final Order order)
    {
        return acceptExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(order.getIdentifier().getId()).
                setAttributionId(order.getIdentifier().getAttributionId()).
                setExecutionId(executionId).
                setOrder(asOrderState(newOrderState, order)).
                setRejectReason(order.getRejectReason());
    }

    private TradeExecution.Builder asTradeExecution(final long executionId, final Order order, final long tradePrice, final long tradeQuantity)
    {
        return tradeExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(order.getIdentifier().getId()).
                setAttributionId(order.getIdentifier().getAttributionId()).
                setExecutionId(executionId).
                setOrder(asOrderState(newOrderState, order)).
                setTradePrice(tradePrice).
                setTradeQuantity(tradeQuantity);
    }

    private ReplaceExecution.Builder asReplaceExecution(final long executionId, final Order cancelOrder, final Order replaceOrder)
    {
        return replaceExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(replaceOrder.getIdentifier().getId()).
                setAttributionId(replaceOrder.getIdentifier().getAttributionId()).
                setExecutionId(executionId).
                setCancelId(cancelOrder.getIdentifier().getId()).
                setCancelOrder(asOrderState(oldOrderState, cancelOrder)).
                setReplaceOrder(asOrderState(newOrderState, replaceOrder));
    }

    private ReplaceExecution.Builder asReplaceRejection(final Identifier identifier, final Identifier cancelIdentifier, final RejectReason rejectReason)
    {
        return replaceExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(identifier.getId()).
                setAttributionId(identifier.getAttributionId()).
                setCancelId(cancelIdentifier.getId()).
                setRejectReason(rejectReason);
    }

    private CancelExecution.Builder asCancelExecution(final long executionId, final Identifier identifer, final Order order)
    {
        return cancelExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(identifer.getId()).
                setAttributionId(order.getIdentifier().getAttributionId()).
                setExecutionId(executionId).
                setCancelId(order.getIdentifier().getId()).
                setOrder(asOrderState(newOrderState, order));
    }

    private CancelExecution.Builder asCancelRejection(final Identifier identifier, final Identifier cancelIdentifier, final RejectReason rejectReason)
    {
        return cancelExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(identifier.getId()).
                setAttributionId(identifier.getAttributionId()).
                setCancelId(cancelIdentifier.getId()).
                setRejectReason(rejectReason);
    }

    private StatusExecution.Builder asStatusExecution(final Identifier identifier, final Order order)
    {
        return statusExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(identifier.getId()).
                setAttributionId(order.getIdentifier().getAttributionId()).
                setStatusId(order.getIdentifier().getId()).
                setOrder(asOrderState(oldOrderState, order));
    }

    private StatusExecution.Builder asStatusRejection(final Identifier identifier, final Identifier statusIdentifier, final RejectReason rejectReason)
    {
        return statusExecution.
                setTimestamp(Clock.now(TimeUnit.MILLISECONDS)).
                setId(identifier.getId()).
                setAttributionId(identifier.getAttributionId()).
                setStatusId(statusIdentifier.getId()).
                setRejectReason(rejectReason);
    }


    private Builder asOrderState(final OrderState.Builder orderState, final Order order)
    {
        return orderState.
                setOrderId(order.getOrderId()).
                setSymbol(orderBook.getSymbol()).
                setSide(order.getSide()).
                setTimeInForce(order.getTimeInForce()).
                setPrice(order.getPrice()).
                setQuantity(order.getQuantity()).
                setWorkingQuantity(order.getWorkingQuantity()).
                setFilledQuantity(order.getFilledQuantity());
    }
}
