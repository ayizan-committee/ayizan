package ayizan.test.matcher;

import ayizan.domain.Orders.Side;
import ayizan.domain.Identifier;
import ayizan.domain.orderbook.Order;
import ayizan.util.Builder;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static ayizan.domain.StandardUnits.toLots;
import static ayizan.domain.StandardUnits.toTicks;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

public class OrderMatcher
{
    public static class OrderMatcherBuilder implements Builder<Matcher<Order>>
    {
        private Identifier identifier;
        private Side side;
        private long price;
        private long quantity;
        private long filledQuantity;
        private long cancelledQuantity;

        public static OrderMatcherBuilder newBuilder()
        {
            return new OrderMatcherBuilder();
        }

        public OrderMatcherBuilder setIdentifier(final Identifier identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public OrderMatcherBuilder setSide(final Side side)
        {
            this.side = side;
            return this;
        }

        public OrderMatcherBuilder setPrice(final double price)
        {
            this.price = toTicks(price);
            return this;
        }

        public OrderMatcherBuilder setQuantity(final double quantity)
        {
            this.quantity = toLots(quantity);
            return this;
        }

        public OrderMatcherBuilder setFilledQuantity(final double quantity)
        {
            this.filledQuantity = toLots(quantity);
            return this;
        }

        public OrderMatcherBuilder setCancelledQuantity(final double quantity)
        {
            this.cancelledQuantity = toLots(quantity);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Matcher<Order> build()
        {
            return allOf(Matchers.<Order>hasProperty("identifier", equalTo(identifier)),
                         Matchers.<Order>hasProperty("side", equalTo(side)),
                         Matchers.<Order>hasProperty("price", equalTo(price)),
                         Matchers.<Order>hasProperty("quantity", equalTo(quantity)),
                         Matchers.<Order>hasProperty("workingQuantity", equalTo(quantity - filledQuantity - cancelledQuantity)),
                         Matchers.<Order>hasProperty("filledQuantity", equalTo(filledQuantity)),
                         Matchers.<Order>hasProperty("cancelledQuantity", equalTo(cancelledQuantity)));

        }

        @Override
        public Builder<Matcher<Order>> clear()
        {
            identifier = null;
            side = null;
            price = 0;
            quantity = 0;
            filledQuantity = 0;
            cancelledQuantity = 0;
            return this;
        }
    }
}
