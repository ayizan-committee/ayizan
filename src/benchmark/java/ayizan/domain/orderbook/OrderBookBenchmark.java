package ayizan.domain.orderbook;

import ayizan.benchmark.Sequence;
import ayizan.domain.Identifier;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.Sides;
import ayizan.domain.orderbook.OrderBook.Matcher;
import ayizan.domain.orderbook.limit.LimitOrderBook;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Random;

import static ayizan.domain.StandardUnits.toLots;
import static ayizan.domain.StandardUnits.toTicks;

public class OrderBookBenchmark extends SimpleBenchmark
{
    private static final Identifier ID = new Identifier("1", 1);
    private static final Matcher IGNORE_MATCHES = new Matcher()
    {
        @Override
        public boolean next(final Order order)
        {
            return false;
        }
    };

    private BenchmarkState benchmarkState;

    public static void main(String... arguments) throws Exception
    {
        Runner.main(OrderBookBenchmark.class, arguments);
    }

    @Override
    public void setUp()
    {
        this.benchmarkState = new BenchmarkState();
    }

    public void timeAcceptCancelWithOrders_100(final int iterations)
    {
        final OrderBook orderBook = benchmarkState.getOrderBook(100);
        for(int i = iterations; i-- != 0;) {
            final double price = benchmarkState.nextPrice();
            final double quantity = benchmarkState.nextQuantity();
            acceptCancel(orderBook, benchmarkState.sideForPrice(price), price, quantity);
        }
    }

    public void timeAcceptCancelWithOrders_100000(final int iterations)
    {
        final OrderBook orderBook = benchmarkState.getOrderBook(100000);
        for(int i = iterations; i-- !=0;) {
            final double price = benchmarkState.nextPrice();
            final double quantity = benchmarkState.nextQuantity();
            acceptCancel(orderBook, benchmarkState.sideForPrice(price), price, quantity);
        }
    }

    private void acceptCancel(final OrderBook orderBook, final Side side, final double price, final double quantity)
    {
        orderBook.accept(ID, side, toTicks(price), toLots(quantity), 0, TimeInForce.GOOD_TILL_CANCEL);
        orderBook.commit();
        orderBook.execute(side, IGNORE_MATCHES);
        orderBook.commit();

        orderBook.cancel(ID).cancel();
        orderBook.commit();
    }

    private static OrderBook setupOrderBook(final BenchmarkState benchmarkState, final int size)
    {
        final OrderBook orderBook = new LimitOrderBook();
        for(int i = 0; i < size; i++) {
            final double price = benchmarkState.nextPrice();
            final double quantity = benchmarkState.nextQuantity();
            final Side side = benchmarkState.sideForPrice(price);
            setupOrder(orderBook, new Identifier(String.valueOf(i), 2), side, price, quantity);
        }
        return orderBook;
    }

    private static void setupOrder(final OrderBook orderBook, final Identifier identifier, final Side side, final double price, final double quantity)
    {
        orderBook.accept(identifier, side, toTicks(price), toLots(quantity), 0, TimeInForce.GOOD_TILL_CANCEL);
        orderBook.commit();
        orderBook.execute(Sides.flip(side), IGNORE_MATCHES);
        orderBook.commit();
    }

    private static class BenchmarkState
    {

        private final Random randomNumberGenerator = new Random(0);
        private final Sequence priceSequence = Sequence.gaussianSequence(randomNumberGenerator, 1000000);
        private final Sequence quantitySequence = Sequence.uniformSequence(randomNumberGenerator, 1000000);
        private final OrderBook[] orderBooks = new OrderBook[]
        {
                setupOrderBook(this, 0),
                setupOrderBook(this, 10),
                setupOrderBook(this, 100),
                setupOrderBook(this, 1000),
                setupOrderBook(this, 10000),
                setupOrderBook(this, 100000),
        };

        public OrderBook getOrderBook(final int size)
        {
            if(size < 10) return orderBooks[0];
            else if(size < 100) return orderBooks[1];
            else if(size < 1000) return orderBooks[2];
            else if(size < 10000) return orderBooks[3];
            else if(size < 100000) return orderBooks[4];
            return orderBooks[5];
        }

        public double nextPrice()
        {
            return 100.0 + 10.0 * priceSequence.next();
        }

        public double nextQuantity()
        {
            return 10.0 * (1 + (10 * quantitySequence.next()));
        }

        public Side sideForPrice(final double price)
        {
            return (price >= 100.0)? Side.SELL : Side.BUY;
        }
    }
}
