package ayizan.test.matcher;

import ayizan.domain.Identifier;
import ayizan.domain.Orders.AcceptExecution;
import ayizan.domain.Orders.CancelExecution;
import ayizan.domain.Orders.Execution;
import ayizan.domain.Orders.Execution.Type;
import ayizan.domain.Orders.OrderState;
import ayizan.domain.Orders.ReplaceExecution;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.Orders.TradeExecution;
import ayizan.util.Builder;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;

public class ExecutionMatcher
{

    public static abstract class AbstractExecutionMatcherBuilder<T extends AbstractExecutionMatcherBuilder<T>> implements Builder<Matcher<Execution>>
    {
        protected String symbol;
        protected Side side;
        protected long price;
        protected long quantity;
        protected long filledQuantity;
        protected long workingQuantity;
        protected TimeInForce timeInForce;

        public T setSymbol(final String symbol)
        {
            this.symbol = symbol;
            return self();
        }

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
        protected Matcher<OrderState> buildOrderStateMatcher()
        {
            return allOf(Matchers.<OrderState>hasProperty("orderId", notNullValue()),
                         Matchers.<OrderState>hasProperty("symbol", equalTo(symbol)),
                         Matchers.<OrderState>hasProperty("side", equalTo(side)),
                         Matchers.<OrderState>hasProperty("price", equalTo(price)),
                         Matchers.<OrderState>hasProperty("quantity", equalTo(quantity)),
                         Matchers.<OrderState>hasProperty("filledQuantity", equalTo(filledQuantity)),
                         Matchers.<OrderState>hasProperty("workingQuantity", equalTo(workingQuantity)),
                         Matchers.<OrderState>hasProperty("timeInForce", equalTo(timeInForce)));

        }
    }

    public static class AcceptExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<AcceptExecutionMatcherBuilder>
    {
        private Identifier identifier;

        public static AcceptExecutionMatcherBuilder newBuilder()
        {
            return new AcceptExecutionMatcherBuilder();
        }

        public AcceptExecutionMatcherBuilder setIdentifier(final Identifier identifier)
        {
            this.identifier = identifier;
            return this;
        }


        @Override
        public Matcher<Execution> build()
        {
            final Matcher<AcceptExecution> acceptExecutionMatcher =
                    allOf(Matchers.<AcceptExecution>hasProperty("id", equalTo(identifier.getId())),
                          Matchers.<AcceptExecution>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                          Matchers.<AcceptExecution>hasProperty("executionId", notNullValue()),
                          Matchers.<AcceptExecution>hasProperty("order", buildOrderStateMatcher()));

            return allOf(Matchers.<Execution>hasProperty("type", equalTo(Type.ACCEPT)),
                         Matchers.<Execution>hasProperty("accept", acceptExecutionMatcher));
        }

        @Override
        public Builder<Matcher<Execution>> clear()
        {
            identifier = null;
            return this;
        }
    }


    public static class TradeExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<TradeExecutionMatcherBuilder>
    {
        private Identifier identifier;
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
        public Matcher<Execution> build()
        {
            final Matcher<TradeExecution> tradeExecutionMatcher =
                    allOf(Matchers.<TradeExecution>hasProperty("id", equalTo(identifier.getId())),
                          Matchers.<TradeExecution>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                          Matchers.<TradeExecution>hasProperty("executionId", notNullValue()),
                          Matchers.<TradeExecution>hasProperty("order", buildOrderStateMatcher()),
                          Matchers.<TradeExecution>hasProperty("tradePrice", equalTo(tradePrice)),
                          Matchers.<TradeExecution>hasProperty("tradeQuantity", equalTo(tradeQuantity)));

            return allOf(Matchers.<Execution>hasProperty("type", equalTo(Type.TRADE)),
                         Matchers.<Execution>hasProperty("trade", tradeExecutionMatcher));
        }

        @Override
        public Builder<Matcher<Execution>> clear()
        {
            identifier = null;
            return this;
        }
    }

    public static class CancelExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<CancelExecutionMatcherBuilder>
    {
        private Identifier identifier;
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

        public CancelExecutionMatcherBuilder setCancelId(final String cancelId)
        {
            this.cancelId = cancelId;
            return this;
        }

        @Override
        public Matcher<Execution> build()
        {
            final Matcher<CancelExecution> cancelExecutionMatcher =
                    allOf(Matchers.<CancelExecution>hasProperty("id", equalTo(identifier.getId())),
                          Matchers.<CancelExecution>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                          Matchers.<CancelExecution>hasProperty("executionId", notNullValue()),
                          Matchers.<CancelExecution>hasProperty("cancelId", equalTo(cancelId)),
                          Matchers.<CancelExecution>hasProperty("order", buildOrderStateMatcher()));

            return allOf(Matchers.<Execution>hasProperty("type", equalTo(Type.CANCEL)),
                         Matchers.<Execution>hasProperty("cancel", cancelExecutionMatcher));
        }

        @Override
        public Builder<Matcher<Execution>> clear()
        {
            identifier = null;
            return this;
        }
    }

    public static class ReplaceExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<ReplaceExecutionMatcherBuilder>
    {
        private Identifier identifier;
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
        public Matcher<Execution> build()
        {
            final Matcher<ReplaceExecution> replaceExecutionMatcher =
                    allOf(Matchers.<ReplaceExecution>hasProperty("id", equalTo(identifier.getId())),
                          Matchers.<ReplaceExecution>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                          Matchers.<ReplaceExecution>hasProperty("executionId", notNullValue()),
                          Matchers.<ReplaceExecution>hasProperty("cancelId", equalTo(cancelId)),
                          Matchers.<ReplaceExecution>hasProperty("cancelOrder", buildOrderStateMatcher()),
                          Matchers.<ReplaceExecution>hasProperty("replaceOrder", buildReplaceOrderStateMatcher()));

            return allOf(Matchers.<Execution>hasProperty("type", equalTo(Type.REPLACE)),
                         Matchers.<Execution>hasProperty("replace", replaceExecutionMatcher));
        }

        @Override
        public Builder<Matcher<Execution>> clear()
        {
            identifier = null;
            return this;
        }

        @SuppressWarnings("unchecked")
        private Matcher<OrderState> buildReplaceOrderStateMatcher()
        {
            return allOf(Matchers.<OrderState>hasProperty("orderId", notNullValue()),
                         Matchers.<OrderState>hasProperty("symbol", equalTo(symbol)),
                         Matchers.<OrderState>hasProperty("side", equalTo(side)),
                         Matchers.<OrderState>hasProperty("price", equalTo(replacePrice)),
                         Matchers.<OrderState>hasProperty("quantity", equalTo(replaceQuantity)),
                         Matchers.<OrderState>hasProperty("filledQuantity", equalTo(filledQuantity)),
                         Matchers.<OrderState>hasProperty("workingQuantity", equalTo(replaceQuantity - filledQuantity)),
                         Matchers.<OrderState>hasProperty("timeInForce", equalTo(timeInForce)));
        }

    }

    public static class StatusExecutionMatcherBuilder extends AbstractExecutionMatcherBuilder<StatusExecutionMatcherBuilder>
    {
        private Identifier identifier;
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

        public StatusExecutionMatcherBuilder setStatusId(final String statusId)
        {
            this.statusId = statusId;
            return this;
        }

        @Override
        public Matcher<Execution> build()
        {
            final Matcher<CancelExecution> statusExecutionMatcher =
                    allOf(Matchers.<CancelExecution>hasProperty("id", equalTo(identifier.getId())),
                          Matchers.<CancelExecution>hasProperty("attributionId", equalTo(identifier.getAttributionId())),
                          Matchers.<CancelExecution>hasProperty("statusId", equalTo(statusId)),
                          Matchers.<CancelExecution>hasProperty("order", buildOrderStateMatcher()));

            return allOf(Matchers.<Execution>hasProperty("type", equalTo(Type.STATUS)),
                         Matchers.<Execution>hasProperty("status", statusExecutionMatcher));
        }

        @Override
        public Builder<Matcher<Execution>> clear()
        {
            identifier = null;
            return this;
        }
    }

}
