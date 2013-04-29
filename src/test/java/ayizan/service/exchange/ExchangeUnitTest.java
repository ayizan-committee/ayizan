package ayizan.service.exchange;


import ayizan.domain.Identifier;
import ayizan.domain.Orders.CancelOrderSpecification;
import ayizan.domain.Orders.CancelReplaceOrderSpecification;
import ayizan.domain.Orders.Execution;
import ayizan.domain.Orders.OrderSpecification;
import ayizan.domain.Orders.OrderStatusSpecification;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.service.ExecutionVenue.ExecutionCallback;
import ayizan.test.matcher.ExecutionMatcher.AcceptExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.CancelExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.ReplaceExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.StatusExecutionMatcherBuilder;
import ayizan.test.matcher.ExecutionMatcher.TradeExecutionMatcherBuilder;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static ayizan.domain.StandardUnits.toLots;
import static ayizan.domain.StandardUnits.toTicks;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ExchangeUnitTest
{
    private static final String SYMBOL = "???";

    private static final Identifier ID_1 = new Identifier("1", 1);
    private static final Identifier ID_2 = new Identifier("2", 2);
    private static final Identifier ID_3 = new Identifier("3", 3);
    private static final Identifier ID_4 = new Identifier("4", 4);
    private static final Identifier CANCEL_ID_1 = new Identifier("CANCEL_1", 1);
    private static final Identifier CANCEL_REPLACE_ID_1 = new Identifier("CANCEL_REPLACE_1", 1);
    private static final Identifier STATUS_ID_1 = new Identifier("STATUS_1", 1);

    @Mock
    private ExecutionCallback executionCallback;

    private Exchange exchange;

    @Before
    public void setUp()
    {
        exchange = new Exchange();
    }

    @Test
    public void placeBuyLimitOrder()
    {
        exchange.placeOrder(limitOrder(ID_1, Side.BUY, 100.0, 1.0).build(), executionCallback);

        verifyExecutions(equalsAcceptExecution(ID_1, Side.BUY, 100.0, 1.0).build());
    }

    @Test
    public void placeBuyLimitOrderPartiallyFills()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 0.5);
        exchange.placeOrder(limitOrder(ID_2, Side.BUY, 100.0, 1.0).build(), executionCallback);

        verifyExecutions(equalsAcceptExecution(ID_2, Side.BUY, 100.0, 1.0).build(),
                         equalsTradeExecution(ID_2, Side.BUY, 100.0, 1.0, 100.0, 0.5).build(),
                         equalsTradeExecution(ID_1, Side.SELL, 100.0, 0.5, 100.0, 0.5).build());
    }

    @Test
    public void placeBuyLimitOrderFullyFills()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0);
        exchange.placeOrder(limitOrder(ID_2, Side.BUY, 100.0, 1.0).build(), executionCallback);

        verifyExecutions(equalsAcceptExecution(ID_2, Side.BUY, 100.0, 1.0).build(),
                         equalsTradeExecution(ID_2, Side.BUY, 100.0, 1.0, 100.0, 1.0).build(),
                         equalsTradeExecution(ID_1, Side.SELL, 100.0, 1.0, 100.0, 1.0).build());
    }


    @Test
    public void placeBuyLimitOrderFillsWithBestExecution()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0);
        setupOrder(ID_2, Side.SELL, 99.0, 1.0);
        exchange.placeOrder(limitOrder(ID_3, Side.BUY, 100.0, 1.0).build(), executionCallback);

        verifyExecutions(equalsAcceptExecution(ID_3, Side.BUY, 100.0, 1.0).build(),
                         equalsTradeExecution(ID_3, Side.BUY, 100.0, 1.0, 99.0, 1.0).build(),
                         equalsTradeExecution(ID_2, Side.SELL, 99.0, 1.0, 99.0, 1.0).build());
    }

    @Test
    public void placeBuyLimitOrderFillsAtMultiplePrices()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0);
        setupOrder(ID_2, Side.SELL, 99.0, 1.0);
        exchange.placeOrder(limitOrder(ID_3, Side.BUY, 100.0, 2.0).build(), executionCallback);

        verifyExecutions(equalsAcceptExecution(ID_3, Side.BUY, 100.0, 2.0).build(),
                         equalsTradeExecution(ID_3, Side.BUY, 100.0, 2.0, 99.0, 1.0).build(),
                         equalsTradeExecution(ID_2, Side.SELL, 99.0, 1.0, 99.0, 1.0).build(),
                         equalsTradeExecution(ID_3, Side.BUY, 100.0, 2.0, 100.0, 1.0).setWorkingQuantity(0).setFilledQuantity(toLots(2.0)).build(),
                         equalsTradeExecution(ID_1, Side.SELL, 100.0, 1.0, 100.0, 1.0).build());
    }


    @Test
    public void placeBuyLimitOrderFillsAtOrBetterThanLimitPrice()
    {
        setupOrder(ID_1, Side.SELL, 101.0, 1.0);
        setupOrder(ID_2, Side.SELL, 100.0, 1.0);
        setupOrder(ID_3, Side.SELL, 99.0, 1.0);
        exchange.placeOrder(limitOrder(ID_4, Side.BUY, 100.0, 3.0).build(), executionCallback);

        verifyExecutions(equalsAcceptExecution(ID_4, Side.BUY, 100.0, 3.0).build(),
                         equalsTradeExecution(ID_4, Side.BUY, 100.0, 3.0, 99.0, 1.0).build(),
                         equalsTradeExecution(ID_3, Side.SELL, 99.0, 1.0, 99.0, 1.0).build(),
                         equalsTradeExecution(ID_4, Side.BUY, 100.0, 3.0, 100.0, 1.0).setWorkingQuantity(toLots(1.0)).setFilledQuantity(toLots(2.0)).build(),
                         equalsTradeExecution(ID_2, Side.SELL, 100.0, 1.0, 100.0, 1.0).build());
    }

    @Test
    public void cancelReplaceBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0);
        exchange.cancelReplaceOrder(cancelReplaceOrder(CANCEL_REPLACE_ID_1, ID_1, 101.0, 2.0).build(), executionCallback);

        verifyExecutions(equalsReplaceExecution(CANCEL_REPLACE_ID_1, ID_1, Side.BUY, 100.0, 1.0, 101.0, 2.0).build());
    }

    @Test
    public void cancelReplacePartiallyFilledBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 2.0);
        setupOrder(ID_2, Side.SELL, 100, 1.0);
        exchange.cancelReplaceOrder(cancelReplaceOrder(CANCEL_REPLACE_ID_1, ID_1, 101.0, 3.0).build(), executionCallback);

        verifyExecutions(equalsReplaceExecution(CANCEL_REPLACE_ID_1, ID_1, Side.BUY, 100.0, 2.0, 101.0, 3.0).setFilledQuantity(toLots(1.0)).build());
    }

    @Test
    public void cancelBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0);

        exchange.cancelOrder(cancelOrder(CANCEL_ID_1, ID_1).build(), executionCallback);
        verifyExecutions(equalsCancelExecution(CANCEL_ID_1, ID_1, Side.BUY, 100.0, 1.0).build());
    }

    @Test
    public void cancelPartiallyFilledBuyLimitOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 2.0);
        setupOrder(ID_2, Side.SELL, 100.0, 1.0);

        exchange.cancelOrder(cancelOrder(CANCEL_ID_1, ID_1).build(), executionCallback);
        verifyExecutions(equalsCancelExecution(CANCEL_ID_1, ID_1, Side.BUY, 100.0, 2.0).setWorkingQuantity(0).setFilledQuantity(toLots(1.0)).build());
    }


    @Test
    public void orderStatusForBuyOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0);

        exchange.orderStatus(orderStatus(STATUS_ID_1, ID_1).build(), executionCallback);
        verifyExecutions(equalsStatusExecution(STATUS_ID_1, ID_1, Side.BUY, 100.0, 1.0).build());
    }

    private void verifyExecutions(final Matcher<?>... matchers)
    {
        final InOrder inOrder = inOrder(executionCallback);
        for(final Matcher<?> matcher : matchers) {
            inOrder.verify(executionCallback).notify((Execution) argThat(matcher));
        }
        verifyNoMoreInteractions(executionCallback);
    }

    private AcceptExecutionMatcherBuilder equalsAcceptExecution(final Identifier identifier, final Side side, final double price, final double quantity)
    {
        return AcceptExecutionMatcherBuilder.newBuilder().
                setIdentifier(identifier).
                setSymbol(SYMBOL).
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

    private OrderSpecification.Builder limitOrder(final Identifier identifier, final Side side, final double price, final double quantity)
    {
        return OrderSpecification.newBuilder().
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
        exchange.placeOrder(limitOrder(identifier, side, price, quantity).build(), ignoreExecutionCallback);
    }
}
