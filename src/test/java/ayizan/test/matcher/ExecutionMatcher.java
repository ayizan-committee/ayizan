package ayizan.test.matcher;

import ayizan.domain.Identifier;
import ayizan.domain.Executions.AcceptExecutionOrBuilder;
import ayizan.domain.Executions.CancelExecutionOrBuilder;
import ayizan.domain.Executions.OrderStateOrBuilder;
import ayizan.domain.Executions.ReplaceExecutionOrBuilder;
import ayizan.domain.Executions.StatusExecutionOrBuilder;
import ayizan.domain.Executions.TradeExecutionOrBuilder;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.util.Builder;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;

public class ExecutionMatcher
{

    public static abstract class AbstractExecutionMatcherBuilder<T extends AbstractExecutionMatcherBuilder<T>> implements Builder<Matcher<?>>
    {
        protected Side side;
        protected long price;
        protected long quantity;
        protected long filledQuantity;
        protected long workingQuantity;
        protected TimeInForce timeInForce;

        public T setSide(final Side side)
        {
            this.side = side;
            return self();
        }

        public T setPrice(final long price)
        {
            this.price = price;
            return self();
        }

        public T setQuantity(final long quantity)
        {
            this.quantity = quantity;
            return self();
        }

        public T setFilledQuantity(final long filledQuantity)
        {
            this.filledQuantity = filledQuantity;
            return self();
        }

        public T setWorkingQuantity(final long workingQuantity)
        {
            this.workingQuantity = workingQuantity;
            return self();
        }

        public T setTimeInForce(final TimeInForce timeInForce)
        {
            this.timeInForce = timeInForce;
            return self();
        }

        @SuppressWarnings("unchecked")
        private T self()
        {
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        protected Matcher<OrderStateOrBuilder> buildOrderStateMatcher()
        {
            return allOf(Matchers.<OrderStateOrBuilder>hasProperty("orderId", notNullValue()),
                    Matchers.<OrderStateOrBuilder>hasProperty("side", equalTo(side)),
                    Matchers.<OrderStateOrBuilder>hasProperty("price", equalTo(price)),
                    Matchers.<OrderStateOrBuilder>hasProperty("quantity", equalTo(quantity)),
                    Matchers.<OrderStateOrBuilder>hasProperty("filledQuantity", equalTo(filledQuantity)),
                    Matchers.<OrderStateOrBuilder>hasProperty("workingQuantity", equalTo(workingQuantity)),
                    Matchers.<OrderStateOrBuilder>hasProperty("timeInForce", equalTo(timeInForce)));

        }
    }

    public static class AcceptExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<AcceptExecutionMatcherBuilder>
    {
        private Identifier identifier;
        private String symbol;

        public static AcceptExecutionMatcherBuilder newBuilder()
        {
            return new AcceptExecutionMatcherBuilder();
        }


        public AcceptExecutionMatcherBuilder setIdentifier(final Identifier identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public AcceptExecutionMatcherBuilder setSymbol(final String symbol)
        {
            this.symbol = symbol;
            return this;
        }


        @Override
        public Matcher<AcceptExecutionOrBuilder> build()
        {
            return allOf(Matchers.<AcceptExecutionOrBuilder>hasProperty("id", equalTo(identifier.getId())),
                         Matchers.<AcceptExecutionOrBuilder>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                         Matchers.<AcceptExecutionOrBuilder>hasProperty("executionId", notNullValue()),
                         Matchers.<AcceptExecutionOrBuilder>hasProperty("symbol", equalTo(symbol)),
                         Matchers.<AcceptExecutionOrBuilder>hasProperty("order", buildOrderStateMatcher()));
        }

        @Override
        public Builder<Matcher<?>> clear()
        {
            identifier = null;
            return this;
        }
    }


    public static class TradeExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<TradeExecutionMatcherBuilder>
    {
        private Identifier identifier;
        private String symbol;
        private long tradePrice;
        private long tradeQuantity;

        public static TradeExecutionMatcherBuilder newBuilder()
        {
            return new TradeExecutionMatcherBuilder();
        }

        public TradeExecutionMatcherBuilder setIdentifier(final Identifier identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public TradeExecutionMatcherBuilder setSymbol(final String symbol)
        {
            this.symbol = symbol;
            return this;
        }


        public TradeExecutionMatcherBuilder setTradePrice(final long tradePrice)
        {
            this.tradePrice = tradePrice;
            return this;
        }

        public TradeExecutionMatcherBuilder setTradeQuantity(final long tradeQuantity)
        {
            this.tradeQuantity = tradeQuantity;
            return this;
        }


        @Override
        @SuppressWarnings("unchecked")
        public Matcher<TradeExecutionOrBuilder> build()
        {
            return allOf(Matchers.<TradeExecutionOrBuilder>hasProperty("id", equalTo(identifier.getId())),
                         Matchers.<TradeExecutionOrBuilder>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                         Matchers.<TradeExecutionOrBuilder>hasProperty("executionId", notNullValue()),
                         Matchers.<TradeExecutionOrBuilder>hasProperty("symbol", equalTo(symbol)),
                         Matchers.<TradeExecutionOrBuilder>hasProperty("order", buildOrderStateMatcher()),
                         Matchers.<TradeExecutionOrBuilder>hasProperty("tradePrice", equalTo(tradePrice)),
                         Matchers.<TradeExecutionOrBuilder>hasProperty("tradeQuantity", equalTo(tradeQuantity)));
        }

        @Override
        public Builder<Matcher<?>> clear()
        {
            identifier = null;
            return this;
        }
    }

    public static class CancelExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<CancelExecutionMatcherBuilder>
    {
        private Identifier identifier;
        private String symbol;
        private String cancelId;

        public static CancelExecutionMatcherBuilder newBuilder()
        {
            return new CancelExecutionMatcherBuilder();
        }

        public CancelExecutionMatcherBuilder setIdentifier(final Identifier identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public CancelExecutionMatcherBuilder setSymbol(final String symbol)
        {
            this.symbol = symbol;
            return this;
        }

        public CancelExecutionMatcherBuilder setCancelId(final String cancelId)
        {
            this.cancelId = cancelId;
            return this;
        }

        @Override
        public Matcher<CancelExecutionOrBuilder> build()
        {

            return allOf(Matchers.<CancelExecutionOrBuilder>hasProperty("id", equalTo(identifier.getId())),
                         Matchers.<CancelExecutionOrBuilder>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                         Matchers.<CancelExecutionOrBuilder>hasProperty("executionId", notNullValue()),
                         Matchers.<CancelExecutionOrBuilder>hasProperty("symbol", equalTo(symbol)),
                         Matchers.<CancelExecutionOrBuilder>hasProperty("cancelId", equalTo(cancelId)),
                         Matchers.<CancelExecutionOrBuilder>hasProperty("order", buildOrderStateMatcher()));
        }

        @Override
        public Builder<Matcher<?>> clear()
        {
            identifier = null;
            return this;
        }
    }

    public static class ReplaceExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<ReplaceExecutionMatcherBuilder>
    {
        private Identifier identifier;
        private String symbol;
        private String cancelId;
        private long replacePrice;
        private long replaceQuantity;

        public static ReplaceExecutionMatcherBuilder newBuilder()
        {
            return new ReplaceExecutionMatcherBuilder();
        }

        public ReplaceExecutionMatcherBuilder setIdentifier(final Identifier identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public ReplaceExecutionMatcherBuilder setSymbol(final String symbol)
        {
            this.symbol = symbol;
            return this;
        }

        public ReplaceExecutionMatcherBuilder setCancelId(final String cancelId)
        {
            this.cancelId = cancelId;
            return this;
        }

        public ReplaceExecutionMatcherBuilder setReplacePrice(final long replacePrice)
        {
            this.replacePrice = replacePrice;
            return this;
        }

        public ReplaceExecutionMatcherBuilder setReplaceQuantity(final long replaceQuantity)
        {
            this.replaceQuantity = replaceQuantity;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Matcher<ReplaceExecutionOrBuilder> build()
        {
            return allOf(Matchers.<ReplaceExecutionOrBuilder>hasProperty("id", equalTo(identifier.getId())),
                         Matchers.<ReplaceExecutionOrBuilder>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                         Matchers.<ReplaceExecutionOrBuilder>hasProperty("executionId", notNullValue()),
                         Matchers.<ReplaceExecutionOrBuilder>hasProperty("symbol", equalTo(symbol)),
                         Matchers.<ReplaceExecutionOrBuilder>hasProperty("cancelId", equalTo(cancelId)),
                         Matchers.<ReplaceExecutionOrBuilder>hasProperty("cancelOrder", buildOrderStateMatcher()),
                         Matchers.<ReplaceExecutionOrBuilder>hasProperty("replaceOrder", buildReplaceOrderStateMatcher()));
        }

        @Override
        public Builder<Matcher<?>> clear()
        {
            identifier = null;
            return this;
        }

        @SuppressWarnings("unchecked")
        private Matcher<OrderStateOrBuilder> buildReplaceOrderStateMatcher()
        {
            return allOf(Matchers.<OrderStateOrBuilder>hasProperty("orderId", notNullValue()),
                    Matchers.<OrderStateOrBuilder>hasProperty("side", equalTo(side)),
                    Matchers.<OrderStateOrBuilder>hasProperty("price", equalTo(replacePrice)),
                    Matchers.<OrderStateOrBuilder>hasProperty("quantity", equalTo(replaceQuantity)),
                    Matchers.<OrderStateOrBuilder>hasProperty("filledQuantity", equalTo(filledQuantity)),
                    Matchers.<OrderStateOrBuilder>hasProperty("workingQuantity", equalTo(replaceQuantity - filledQuantity)),
                    Matchers.<OrderStateOrBuilder>hasProperty("timeInForce", equalTo(timeInForce)));
        }

    }

    public static class StatusExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<StatusExecutionMatcherBuilder>
    {
        private Identifier identifier;
        private String symbol;
        private String statusId;

        public static StatusExecutionMatcherBuilder newBuilder()
        {
            return new StatusExecutionMatcherBuilder();
        }

        public StatusExecutionMatcherBuilder setIdentifier(final Identifier identifier)
        {
            this.identifier = identifier;
            return this;
        }

        public StatusExecutionMatcherBuilder setSymbol(final String symbol)
        {
            this.symbol = symbol;
            return this;
        }

        public StatusExecutionMatcherBuilder setStatusId(final String statusId)
        {
            this.statusId = statusId;
            return this;
        }

        @Override
        public Matcher<StatusExecutionOrBuilder> build()
        {
            return allOf(Matchers.<StatusExecutionOrBuilder>hasProperty("id", equalTo(identifier.getId())),
                         Matchers.<StatusExecutionOrBuilder>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                         Matchers.<StatusExecutionOrBuilder>hasProperty("symbol", equalTo(symbol)),
                         Matchers.<StatusExecutionOrBuilder>hasProperty("statusId", equalTo(statusId)),
                         Matchers.<StatusExecutionOrBuilder>hasProperty("order", buildOrderStateMatcher()));
        }

        @Override
        public Builder<Matcher<?>> clear()
        {
            identifier = null;
            return this;
        }
    }

}
