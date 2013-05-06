package ayizan.service.exchange.policy;

import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.Identifier;
import ayizan.domain.orderbook.Order;
import ayizan.domain.orderbook.OrderBook;
import ayizan.service.exchange.Exchange.ExecutionPublisher;

public abstract class AcceptPolicy<T extends AcceptPolicy<T>>
{
    protected ExecutionPublisher executionPublisher;

    public T setExecutionPublisher(final ExecutionPublisher executionPublisher)
    {
        this.executionPublisher = executionPublisher;
        return policy();
    }

    @SuppressWarnings("unchecked")
    private T policy()
    {
        return (T) this;
    }

    public static class OrderAcceptPolicy extends AcceptPolicy<OrderAcceptPolicy>
    {
        private OrderAcceptPolicy() {}

        public static OrderAcceptPolicy allocate()
        {
            return new OrderAcceptPolicy();
        }

        public Order accept(final OrderBook orderBook,
                            final Identifier identifier,
                            final Side side,
                            final long price,
                            final long quantity,
                            final TimeInForce timeInForce)
        {
            final Order order = orderBook.accept(identifier, side, price, quantity, 0, timeInForce);
            executionPublisher.publishAcceptExecution(orderBook.commit(), orderBook.getSymbol(), order);
            return order;
        }
    }

    public static class ReplaceOrderAcceptPolicy extends AcceptPolicy<ReplaceOrderAcceptPolicy>
    {
        private ReplaceOrderAcceptPolicy() {}

        public static ReplaceOrderAcceptPolicy allocate()
        {
            return new ReplaceOrderAcceptPolicy();
        }

        public Order accept(final OrderBook orderBook,
                            final Identifier identifier,
                            final Identifier cancelIdentifier,
                            final long price,
                            final long quantity)
        {
            final Order cancelOrder = orderBook.cancel(cancelIdentifier);
            if(cancelOrder != null) {

                final Order replaceOrder = orderBook.accept(identifier, cancelOrder.getSide(), price, quantity, cancelOrder.getFilledQuantity(), cancelOrder.getTimeInForce());
                executionPublisher.publishReplaceExecution(orderBook.commit(), orderBook.getSymbol(), cancelOrder.cancel(), replaceOrder);
                return replaceOrder;
            }
            return null;
        }
    }

}
