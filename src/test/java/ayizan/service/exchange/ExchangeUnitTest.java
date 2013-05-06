package ayizan.service.exchange;


import ayizan.domain.Identifier;
import ayizan.domain.Executions.AcceptExecution;
import ayizan.domain.Executions.AcceptExecutionOrBuilder;
import ayizan.domain.Executions.CancelExecution;
import ayizan.domain.Executions.CancelExecutionOrBuilder;
import ayizan.domain.Executions.ReplaceExecution;
import ayizan.domain.Executions.ReplaceExecutionOrBuilder;
import ayizan.domain.Executions.StatusExecution;
import ayizan.domain.Executions.StatusExecutionOrBuilder;
import ayizan.domain.Executions.TradeExecution;
import ayizan.domain.Executions.TradeExecutionOrBuilder;
import ayizan.domain.Orders.CancelOrderSpecification;
import ayizan.domain.Orders.CancelReplaceOrderSpecification;
import ayizan.domain.Orders.OrderStatusSpecification;
import ayizan.domain.Orders.PlaceOrderSpecification;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.service.ExecutionVenue.ExecutionCallback;
import ayizan.test.matcher.ExecutionMatcher.AbstractExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.AcceptExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.CancelExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.ReplaceExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.StatusExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.TradeExecutionMatcherBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.runners.MockitoJUnitRunner;

import static ayizan.domain.StandardUnits.toLots;
import static ayizan.domain.StandardUnits.toTicks;
import static ayizan.service.exchange.Exchange.DEFAULT_INSTRUMENT;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ExchangeUnitTest
{
    private static final String SYMBOL = DEFAULT_INSTRUMENT.getSymbol();

    private static final Identifier ID_1 = new Identifier("1", 1);
    private static final Identifier ID_2 = new Identifier("2", 2);
    private static final Identifier ID_3 = new Identifier("3", 3);
    private static final Identifier ID_4 = new Identifier("4", 4);
    private static final Identifier CANCEL_ID_1 = new Identifier("CANCEL_1", 1);
    private static final Identifier CANCEL_REPLACE_ID_1 = new Identifier("CANCEL_REPLACE_1", 1);
    private static final Identifier STATUS_ID_1 = new Identifier("STATUS_1", 1);


    private ExecutionCallbackWrapper executionCallback;
    private Exchange exchange;

    @Before
    public void setUp()
    {
        executionCallback = new ExecutionCallbackWrapper();
        exchange = new Exchange();
    }


    @Test
    public void placeBuyLimitOrder()
    {
        exchange.placeOrder(limitOrder(ID_1, Side.BUY, 100.0, 1.0), executionCallback);

        executionCallback.verifyExecutions(equalsAcceptExecution(ID_1, Side.BUY, 100.0, 1.0));
    }

    @Test
    public void placeBuyLimitOrderPartiallyFills()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 0.5);
        exchange.placeOrder(limitOrder(ID_2, Side.BUY, 100.0, 1.0), executionCallback);

        executionCallback.verifyExecutions(equalsAcceptExecution(ID_2, Side.BUY, 100.0, 1.0),
                                           equalsTradeExecution(ID_2, Side.BUY, 100.0, 1.0, 100.0, 0.5),
                                           equalsTradeExecution(ID_1, Side.SELL, 100.0, 0.5, 100.0, 0.5));
    }

    @Test
    public void placeBuyLimitOrderFullyFills()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0);
        exchange.placeOrder(limitOrder(ID_2, Side.BUY, 100.0, 1.0), executionCallback);

        executionCallback.verifyExecutions(equalsAcceptExecution(ID_2, Side.BUY, 100.0, 1.0),
                                           equalsTradeExecution(ID_2, Side.BUY, 100.0, 1.0, 100.0, 1.0),
                                           equalsTradeExecution(ID_1, Side.SELL, 100.0, 1.0, 100.0, 1.0));
    }


    @Test
    public void placeBuyLimitOrderFillsWithBestExecution()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0);
        setupOrder(ID_2, Side.SELL, 99.0, 1.0);
        exchange.placeOrder(limitOrder(ID_3, Side.BUY, 100.0, 1.0), executionCallback);

        executionCallback.verifyExecutions(equalsAcceptExecution(ID_3, Side.BUY, 100.0, 1.0),
                                           equalsTradeExecution(ID_3, Side.BUY, 100.0, 1.0, 99.0, 1.0),
                                           equalsTradeExecution(ID_2, Side.SELL, 99.0, 1.0, 99.0, 1.0));
    }

    @Test
    public void placeBuyLimitOrderFillsAtMultiplePrices()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0);
        setupOrder(ID_2, Side.SELL, 99.0, 1.0);
        exchange.placeOrder(limitOrder(ID_3, Side.BUY, 100.0, 2.0), executionCallback);

        executionCallback.verifyExecutions(equalsAcceptExecution(ID_3, Side.BUY, 100.0, 2.0),
                                           equalsTradeExecution(ID_3, Side.BUY, 100.0, 2.0, 99.0, 1.0),
                                           equalsTradeExecution(ID_2, Side.SELL, 99.0, 1.0, 99.0, 1.0),
                                           equalsTradeExecution(ID_3, Side.BUY, 100.0, 2.0, 100.0, 1.0).setWorkingQuantity(0).setFilledQuantity(toLots(2.0)),
                                           equalsTradeExecution(ID_1, Side.SELL, 100.0, 1.0, 100.0, 1.0));
    }


    @Test
    public void placeBuyLimitOrderFillsAtOrBetterThanLimitPrice()
    {
        setupOrder(ID_1, Side.SELL, 101.0, 1.0);
        setupOrder(ID_2, Side.SELL, 100.0, 1.0);
        setupOrder(ID_3, Side.SELL, 99.0, 1.0);
        exchange.placeOrder(limitOrder(ID_4, Side.BUY, 100.0, 3.0), executionCallback);

        executionCallback.verifyExecutions(equalsAcceptExecution(ID_4, Side.BUY, 100.0, 3.0),
                                           equalsTradeExecution(ID_4, Side.BUY, 100.0, 3.0, 99.0, 1.0),
                                           equalsTradeExecution(ID_3, Side.SELL, 99.0, 1.0, 99.0, 1.0),
                                           equalsTradeExecution(ID_4, Side.BUY, 100.0, 3.0, 100.0, 1.0).setWorkingQuantity(toLots(1.0)).setFilledQuantity(toLots(2.0)),
                                           equalsTradeExecution(ID_2, Side.SELL, 100.0, 1.0, 100.0, 1.0));
    }

    @Test
    public void cancelReplaceBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0);
        exchange.cancelReplaceOrder(cancelReplaceOrder(CANCEL_REPLACE_ID_1, ID_1, 101.0, 2.0), executionCallback);

        executionCallback.verifyExecutions(equalsReplaceExecution(CANCEL_REPLACE_ID_1, ID_1, Side.BUY, 100.0, 1.0, 101.0, 2.0));
    }

    @Test
    public void cancelReplacePartiallyFilledBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 2.0);
        setupOrder(ID_2, Side.SELL, 100, 1.0);
        exchange.cancelReplaceOrder(cancelReplaceOrder(CANCEL_REPLACE_ID_1, ID_1, 101.0, 3.0), executionCallback);

        executionCallback.verifyExecutions(equalsReplaceExecution(CANCEL_REPLACE_ID_1, ID_1, Side.BUY, 100.0, 2.0, 101.0, 3.0).setFilledQuantity(toLots(1.0)));
    }

    @Test
    public void cancelBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0);

        exchange.cancelOrder(cancelOrder(CANCEL_ID_1, ID_1), executionCallback);
        executionCallback.verifyExecutions(equalsCancelExecution(CANCEL_ID_1, ID_1, Side.BUY, 100.0, 1.0));
    }

    @Test
    public void cancelPartiallyFilledBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 2.0);
        setupOrder(ID_2, Side.SELL, 100.0, 1.0);

        exchange.cancelOrder(cancelOrder(CANCEL_ID_1, ID_1), executionCallback);
        executionCallback.verifyExecutions(equalsCancelExecution(CANCEL_ID_1, ID_1, Side.BUY, 100.0, 2.0).setWorkingQuantity(0).setFilledQuantity(toLots(1.0)));
    }


    @Test
    public void orderStatusForBuyOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0);

        exchange.orderStatus(orderStatus(STATUS_ID_1, ID_1), executionCallback);
        executionCallback.verifyExecutions(equalsStatusExecution(STATUS_ID_1, ID_1, Side.BUY, 100.0, 1.0));
    }

    private AcceptExecutionMatcherBuilder equalsAcceptExecution(final Identifier identifier, final Side side, final double price, final double quantity)
    {
        return AcceptExecutionMatcherBuilder.newBuilder().
                setIdentifier(identifier).
                setSymbol(DEFAULT_INSTRUMENT.getSymbol()).
                setSide(side).
                setPrice(toTicks(price)).
                setQuantity(toLots(quantity)).
                setWorkingQuantity(toLots(quantity)).
                setFilledQuantity(0).
                setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
    }

    private TradeExecutionMatcherBuilder equalsTradeExecution(final Identifier identifier, final Side side, final double price, final double quantity, final double tradePrice, final double tradeQuantity)
    {
        return TradeExecutionMatcherBuilder.newBuilder().
                setIdentifier(identifier).
                setSymbol(SYMBOL).
                setSide(side).
                setPrice(toTicks(price)).
                setQuantity(toLots(quantity)).
                setWorkingQuantity(toLots(quantity) - toLots(tradeQuantity)).
                setFilledQuantity(toLots(tradeQuantity)).
                setTradePrice(toTicks(tradePrice)).
                setTradeQuantity(toLots(tradeQuantity)).
                setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
    }

    private CancelExecutionMatcherBuilder equalsCancelExecution(final Identifier identifier, final Identifier cancelIdentifier, final Side side, final double price, final double quantity)
    {
        return CancelExecutionMatcherBuilder.newBuilder().
                setIdentifier(identifier).
                setSymbol(SYMBOL).
                setCancelId(cancelIdentifier.getId()).
                setSide(side).
                setPrice(toTicks(price)).
                setQuantity(toLots(quantity)).
                setWorkingQuantity(0).
                setFilledQuantity(0).
                setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
    }

    private ReplaceExecutionMatcherBuilder equalsReplaceExecution(final Identifier identifier, final Identifier cancelIdentifier, final Side side, final double cancelPrice, final double cancelQuantity, final double replacePrice, final double replaceQuantity)
    {
        return ReplaceExecutionMatcherBuilder.newBuilder().
                setIdentifier(identifier).
                setSymbol(SYMBOL).
                setCancelId(cancelIdentifier.getId()).
                setSide(side).
                setPrice(toTicks(cancelPrice)).
                setQuantity(toLots(cancelQuantity)).
                setWorkingQuantity(0).
                setFilledQuantity(0).
                setTimeInForce(TimeInForce.GOOD_TILL_CANCEL).
                setReplacePrice(toTicks(replacePrice)).
                setReplaceQuantity(toLots(replaceQuantity));
    }

    private StatusExecutionMatcherBuilder equalsStatusExecution(final Identifier identifier, final Identifier statusIdentifier, final Side side, final double price, final double quantity)
    {
        return StatusExecutionMatcherBuilder.newBuilder().
                setIdentifier(identifier).
                setSymbol(SYMBOL).
                setStatusId(statusIdentifier.getId()).
                setSide(side).
                setPrice(toTicks(price)).
                setQuantity(toLots(quantity)).
                setWorkingQuantity(toLots(quantity)).
                setFilledQuantity(0).
                setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
    }

    private PlaceOrderSpecification.Builder limitOrder(final Identifier identifier, final Side side, final double price, final double quantity)
    {
        return PlaceOrderSpecification.newBuilder().
                       setId(identifier.getId()).
                       setAttributionId(identifier.getAttributionId()).
                       setSymbol(SYMBOL).
                       setSide(side).
                       setPrice(toTicks(price)).
                       setQuantity(toLots(quantity)).
                       setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
    }

    private CancelReplaceOrderSpecification.Builder cancelReplaceOrder(final Identifier identifier, final Identifier cancelIdentifier, final double price, final double quantity)
    {
        return CancelReplaceOrderSpecification.newBuilder().
                         setId(identifier.getId()).
                         setAttributionId(identifier.getAttributionId()).
                         setSymbol(SYMBOL).
                         setCancelId(cancelIdentifier.getId()).
                         setPrice(toTicks(price)).
                         setQuantity(toLots(quantity));
    }


    private CancelOrderSpecification.Builder cancelOrder(final Identifier identifier, final Identifier cancelIdentifier)
    {
        return CancelOrderSpecification.newBuilder().
                       setId(identifier.getId()).
                       setAttributionId(identifier.getAttributionId()).
                       setSymbol(SYMBOL).
                       setCancelId(cancelIdentifier.getId());
}

    private OrderStatusSpecification.Builder orderStatus(final Identifier identifier, final Identifier statusIdentifier)
    {
        return OrderStatusSpecification.newBuilder().
                       setId(identifier.getId()).
                       setAttributionId(identifier.getAttributionId()).
                       setStatusId(statusIdentifier.getId()).
                       setSymbol(SYMBOL);
}

    private void setupOrder(final Identifier identifier, final Side side, final double price, final double quantity)
    {
        final ExecutionCallback ignoreExecutionCallback = mock(ExecutionCallback.class);
        exchange.placeOrder(limitOrder(identifier, side, price, quantity), ignoreExecutionCallback);
    }

    public static class ExecutionCallbackWrapper implements ExecutionCallback
    {
        private ExecutionCallback executionCallback = mock(ExecutionCallback.class);

        public void verifyExecutions(final AbstractExecutionMatcherBuilder<?>... matchers)
        {
            final InOrder inOrder = inOrder(executionCallback);
            inOrder.verify(executionCallback).start();

            for(final AbstractExecutionMatcherBuilder<?> matcher : matchers) {
                if(matcher instanceof AcceptExecutionMatcherBuilder) inOrder.verify(executionCallback).notify((AcceptExecutionOrBuilder) argThat(matcher.build()));
                else if(matcher instanceof TradeExecutionMatcherBuilder) inOrder.verify(executionCallback).notify((TradeExecutionOrBuilder) argThat(matcher.build()));
                else if(matcher instanceof CancelExecutionMatcherBuilder) inOrder.verify(executionCallback).notify((CancelExecutionOrBuilder) argThat(matcher.build()));
                else if(matcher instanceof ReplaceExecutionMatcherBuilder) inOrder.verify(executionCallback).notify((ReplaceExecutionOrBuilder) argThat(matcher.build()));
                else if(matcher instanceof StatusExecutionMatcherBuilder) inOrder.verify(executionCallback).notify((StatusExecutionOrBuilder) argThat(matcher.build()));
            }
            inOrder.verify(executionCallback).commit();
            verifyNoMoreInteractions(executionCallback);
        }

        public void start()
        {
            executionCallback.start();
        }

        public void notify(final AcceptExecutionOrBuilder acceptExecution)
        {
            executionCallback.notify(((AcceptExecution.Builder) acceptExecution).clone());
        }

        public void notify(final TradeExecutionOrBuilder tradeExecution)
        {
            executionCallback.notify(((TradeExecution.Builder) tradeExecution).clone());
        }

        public void notify(final ReplaceExecutionOrBuilder replaceExecution)
        {
            executionCallback.notify(((ReplaceExecution.Builder) replaceExecution).clone());
        }

        public void notify(final CancelExecutionOrBuilder cancelExecution)
        {
            executionCallback.notify(((CancelExecution.Builder) cancelExecution).clone());
        }

        public void notify(final StatusExecutionOrBuilder statusExecution)
        {
            executionCallback.notify(((StatusExecution.Builder) statusExecution).clone());
        }

        public void commit()
        {
            executionCallback.commit();
        }

    }
}
