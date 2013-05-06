package ayizan.service.exchange.policy;

import ayizan.domain.orderbook.Order;
import ayizan.domain.orderbook.OrderBook;
import ayizan.domain.orderbook.OrderBook.Matcher;
import ayizan.service.exchange.Exchange.ExecutionPublisher;

import java.util.Comparator;

import static ayizan.domain.Sides.priceComparator;
import static java.lang.Math.min;

public abstract class FillPolicy<T extends FillPolicy<T>>
{
    protected ExecutionPublisher executionPublisher;

    public T setExecutionPublisher(final ExecutionPublisher executionPublisher)
    {
        this.executionPublisher = executionPublisher;
        return policy();
    }

    public abstract boolean fill(OrderBook orderBook, Order order);

    @SuppressWarnings("unchecked")
    private T policy()
    {
        return (T) this;
    }

    public static class PartialFillPolicy extends FillPolicy<PartialFillPolicy> implements Matcher
    {
        private Comparator<Long> priceComparator;
        private OrderBook orderBook;
        private Order aggressiveOrder;

        public static PartialFillPolicy allocate()
        {
            return new PartialFillPolicy();
        }
        private PartialFillPolicy() {}


        @Override
        public boolean fill(final OrderBook orderBook, final Order order)
        {
            try {
                this.aggressiveOrder = order;
                this.orderBook = orderBook;
                this.priceComparator = priceComparator(order.getSide());
                orderBook.execute(order.getSide(), this);
            }
            finally {
                this.orderBook = null;
                this.aggressiveOrder = null;
                this.priceComparator = null;
            }
            return true;
        }

        @Override
        public boolean next(final Order passiveOrder)
        {
            final long tradePrice = passiveOrder.getPrice();
            if(isWithinLimit(tradePrice)) {
                if(isMatchable(passiveOrder.getIdentifier().getAttributionId())) {
                    final long tradeQuantity = min(aggressiveOrder.getWorkingQuantity(), passiveOrder.getWorkingQuantity());
                    passiveOrder.fill(tradeQuantity);
                    aggressiveOrder.fill(tradeQuantity);
                    executionPublisher.publishTradeExecution(orderBook.commit(), orderBook.getSymbol(), aggressiveOrder, passiveOrder, tradePrice, tradeQuantity);
                }
                return !aggressiveOrder.isCompleted();
            }
            return false;
        }

        private boolean isWithinLimit(final long tradePrice)
        {
            return priceComparator.compare(aggressiveOrder.getPrice(), tradePrice) != -1;
        }

        private boolean isMatchable(final int attributionId)
        {
            return aggressiveOrder.getIdentifier().getAttributionId() != attributionId;
        }
    }
}
