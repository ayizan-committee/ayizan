package ayizan.service.exchange.policy;

import ayizan.domain.Identifier;
import ayizan.domain.orderbook.Order;
import ayizan.domain.orderbook.OrderBook;
import ayizan.service.exchange.Exchange.ExecutionPublisher;

public abstract class CancelPolicy<T extends CancelPolicy>
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

    public abstract boolean cancel(OrderBook orderBook, final Identifier identifier, Order order);

    public static class GoodTillCancelPolicy extends CancelPolicy<GoodTillCancelPolicy>
    {
        private GoodTillCancelPolicy() {}

        public static GoodTillCancelPolicy allocate()
        {
            return new GoodTillCancelPolicy();
        }

        @Override
        public boolean cancel(final OrderBook orderBook, final Identifier identifier, final Order order)
        {
            if(order != null) {
                if(!order.isCompleted()) orderBook.commit();
                return true;
            }
            return false;
        }
    }

    public static class ImmediateCancelPolicy extends CancelPolicy<ImmediateCancelPolicy>
    {
        private ImmediateCancelPolicy() {}

        public static ImmediateCancelPolicy allocate()
        {
            return new ImmediateCancelPolicy();
        }

        @Override
        public boolean cancel(final OrderBook orderBook, final Identifier identifier, final Order order)
        {
            if(order != null && !order.isCompleted()) {
                order.cancel();
                executionPublisher.publishCancelExecution(orderBook.commit(), orderBook.getSymbol(), identifier, order);
                return true;
            }
            return false;
        }
    }
}
