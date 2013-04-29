package ayizan.domain.orderbook.limit;

import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.Identifier;
import ayizan.domain.Sides;
import ayizan.domain.orderbook.Order;
import ayizan.domain.orderbook.OrderBook;
import ayizan.test.matcher.OrderMatcher.OrderMatcherBuilder;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static ayizan.domain.StandardUnits.toLots;
import static ayizan.domain.StandardUnits.toTicks;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class LimitOrderBookUnitTest
{
    private static final Identifier ID_1 = new Identifier("1", 1);
    private static final Identifier ID_2 = new Identifier("2", 2);
    private static final Identifier ID_3 = new Identifier("3", 3);

    @Mock
    private OrderBook.Matcher orderMatcher;

    private LimitOrderBook limitOrderBook;

    @Before
    public void setUp()
    {
        limitOrderBook = new LimitOrderBook();
    }

    @Test
    public void acceptedAggressiveBuyOrderAdded()
    {
        final Order order = limitOrderBook.accept(ID_1, Side.BUY, toTicks(100.0), toLots(1.0), 0, TimeInForce.GOOD_TILL_CANCEL);
        limitOrderBook.commit();
        assertThat(order, equalsBuyOrder(100.0, 1.0).build());
    }

    @Test
    public void partiallyFilledAggressiveBuyOrderAdded()
    {
        final Order order = limitOrderBook.accept(ID_1, Side.BUY, toTicks(100.0), toLots(1.0), 0, TimeInForce.GOOD_TILL_CANCEL).fill(toLots(0.5));
        limitOrderBook.commit();

        assertThat(order, equalsBuyOrder(100, 1.0).setFilledQuantity(0.5).build());
    }


    @Test
    public void fullyFilledAggressiveBuyOrderRemoved()
    {
        final Order order = limitOrderBook.accept(ID_1, Side.BUY, toTicks(100.0), toLots(1.0), 0, TimeInForce.GOOD_TILL_CANCEL).fill(toLots(1.0));
        limitOrderBook.commit();

        assertThat(order, equalsBuyOrder(100, 1.0).setFilledQuantity(1.0).build());
    }

    @Test
    public void cancelledAggressiveBuyOrderRemoved()
    {
        final Order order = limitOrderBook.accept(ID_1, Side.BUY, toTicks(100.0), toLots(1.0), 0, TimeInForce.GOOD_TILL_CANCEL).cancel();
        limitOrderBook.commit();

        assertThat(order, equalsBuyOrder(100, 1.0).setCancelledQuantity(1.0).build());
    }




    @Test
    public void buyOrdersInPriceTimePriorityOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_2, Side.BUY, 100.0, 2.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_3, Side.BUY, 99.0, 3.0, TimeInForce.GOOD_TILL_CANCEL);

        limitOrderBook.execute(Side.SELL, allOrderMatcher());
        limitOrderBook.commit();

        verifyOrders(equalsBuyOrder(100.0, 1.0).setIdentifier(ID_1).build(),
                     equalsBuyOrder(100.0, 2.0).setIdentifier(ID_2).build(),
                     equalsBuyOrder(99.0, 3.0).setIdentifier(ID_3).build());
    }


    @Test
    public void buyOrdersInPriceTimePriorityOrderWithOutOfOrderArrival()
    {
        setupOrder(ID_1, Side.BUY, 99.0, 1.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_2, Side.BUY, 100.0, 2.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_3, Side.BUY, 100.0, 3.0, TimeInForce.GOOD_TILL_CANCEL);

        limitOrderBook.execute(Side.SELL, allOrderMatcher());
        limitOrderBook.commit();

        verifyOrders(equalsBuyOrder(100.0, 2.0).setIdentifier(ID_2).build(),
                     equalsBuyOrder(100.0, 3.0).setIdentifier(ID_3).build(),
                     equalsBuyOrder(99.0, 1.0).setIdentifier(ID_1).build());
    }

    @Test
    public void sellOrdersInPriceTimePriorityOrder()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_2, Side.SELL, 101.0, 2.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_3, Side.SELL, 101.0, 3.0, TimeInForce.GOOD_TILL_CANCEL);

        limitOrderBook.execute(Side.BUY, allOrderMatcher());
        limitOrderBook.commit();

        verifyOrders(equalsSellOrder(100.0, 1.0).setIdentifier(ID_1).build(),
                     equalsSellOrder(101.0, 2.0).setIdentifier(ID_2).build(),
                     equalsSellOrder(101.0, 3.0).setIdentifier(ID_3).build());
    }

    @Test
    public void sellOrdersInPriceTimePriorityOrderWithOutOfOrderArrival()
    {
        setupOrder(ID_1, Side.SELL, 101.0, 1.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_2, Side.SELL, 101.0, 2.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_3, Side.SELL, 100.0, 3.0, TimeInForce.GOOD_TILL_CANCEL);

        limitOrderBook.execute(Side.BUY, allOrderMatcher());
        limitOrderBook.commit();
        verifyOrders(equalsSellOrder(100.0, 3.0).setIdentifier(ID_3).build(),
                     equalsSellOrder(101.0, 1.0).setIdentifier(ID_1).build(),
                     equalsSellOrder(101.0, 2.0).setIdentifier(ID_2).build());
    }

    @Test
    public void buyOrdersCancelledInPriceTimePriorityOrder()
    {
        setupOrder(ID_1, Side.BUY, 100.0, 1.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_2, Side.BUY, 100.0, 2.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_3, Side.BUY, 99.0, 3.0, TimeInForce.GOOD_TILL_CANCEL);

        limitOrderBook.cancel(ID_1).cancel();
        limitOrderBook.commit();
        limitOrderBook.cancel(ID_2).cancel();
        limitOrderBook.commit();
        limitOrderBook.cancel(ID_3).cancel();
        limitOrderBook.commit();

        limitOrderBook.execute(Side.SELL, allOrderMatcher());
        limitOrderBook.commit();
        verifyNoOrders();
    }

    @Test
    public void sellOrdersCancelledInPriceTimePriorityOrder()
    {
        setupOrder(ID_1, Side.SELL, 100.0, 1.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_2, Side.SELL, 101.0, 2.0, TimeInForce.GOOD_TILL_CANCEL);
        setupOrder(ID_3, Side.SELL, 101.0, 3.0, TimeInForce.GOOD_TILL_CANCEL);

        limitOrderBook.cancel(ID_1).cancel();
        limitOrderBook.commit();
        limitOrderBook.cancel(ID_2).cancel();
        limitOrderBook.commit();
        limitOrderBook.cancel(ID_3).cancel();
        limitOrderBook.commit();

        limitOrderBook.execute(Side.BUY, allOrderMatcher());
        limitOrderBook.commit();
        verifyNoOrders();
    }


    private void setupOrder(final Identifier identifier, final Side side, final double price, final double quantity, final TimeInForce timeInForce)
    {
        limitOrderBook.accept(identifier, side, toTicks(price), toLots(quantity), 0, timeInForce);
        limitOrderBook.commit();
        limitOrderBook.execute(Sides.flip(side), noOrderMatcher());
        limitOrderBook.commit();
    }

    private OrderBook.Matcher noOrderMatcher()
    {
        OrderBook.Matcher orderMatcher = mock(OrderBook.Matcher.class);
        doReturn(false).when(orderMatcher).next(any(Order.class));
        return orderMatcher;
    }

    private OrderBook.Matcher allOrderMatcher()
    {
        doReturn(true).when(orderMatcher).next(any(Order.class));
        return orderMatcher;
    }

    private void verifyNoOrders()
    {
        verifyZeroInteractions(orderMatcher);
        verifyNoMoreInteractions(orderMatcher);
    }

    private void verifyOrders(final Matcher<?>... matchers)
    {
        final InOrder inOrder = inOrder(orderMatcher);
        for(final Matcher<?> matcher : matchers) {
            inOrder.verify(orderMatcher).next((Order) argThat(matcher));
        }
        verifyNoMoreInteractions(orderMatcher);
    }

    private OrderMatcherBuilder equalsBuyOrder(final double price, final double quantity)
    {
        return OrderMatcherBuilder.newBuilder().
                setIdentifier(ID_1).
                setSide(Side.BUY).
                setPrice(price).
                setQuantity(quantity);
    }

    private OrderMatcherBuilder equalsSellOrder(final double price, final double quantity)
    {
        return OrderMatcherBuilder.newBuilder().
                setIdentifier(ID_1).
                setSide(Side.SELL).
                setPrice(price).
                setQuantity(quantity);
    }
}
