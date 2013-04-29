package ayizan.service;

import ayizan.benchmark.Sequence;
import ayizan.domain.Orders.CancelOrderSpecification;
import ayizan.domain.Orders.Execution;
import ayizan.domain.Orders.OrderSpecification;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.service.ExecutionVenue.ExecutionCallback;
import ayizan.service.exchange.Exchange;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Random;

import static ayizan.domain.StandardUnits.toLots;
import static ayizan.domain.StandardUnits.toTicks;

public class ExecutionVenueBenchmark extends SimpleBenchmark
{
    private static final ExecutionCallback IGNORE_EXECUTIONS = new ExecutionCallback()
    {
        @Override
        public void notify(final Execution execution)
        {
        }
    };

    private BenchmarkState benchmarkState;

    public static void main(String... arguments) throws Exception
    {
        Runner.main(ExecutionVenueBenchmark.class, arguments);
    }

    @Override
    public void setUp()
    {
        benchmarkState = new BenchmarkState();
    }

    public void timePlaceAndCancelWithOrders_100(final int iterations)
    {
        for(int i = iterations; i-- != 0;) {
            final OrderSpecification limitOrderSpecification = benchmarkState.nextLimitOrder().setId("1").setAttributionId(1).build();
            final CancelOrderSpecification cancelOrderSpecification = benchmarkState.cancelForLimitOrder(limitOrderSpecification).setId("2").setAttributionId(1).build();

            placeAndCancel(benchmarkState.getExchange(100), limitOrderSpecification, cancelOrderSpecification);
        }
    }

    public void timePlaceAndCancelWithOrders_100000(final int iterations)
    {
        for(int i = iterations; i-- != 0;) {
            final OrderSpecification limitOrderSpecification = benchmarkState.nextLimitOrder().setId("1").setAttributionId(1).build();
            final CancelOrderSpecification cancelOrderSpecification = benchmarkState.cancelForLimitOrder(limitOrderSpecification).setId("2").setAttributionId(1).build();

            placeAndCancel(benchmarkState.getExchange(100000), limitOrderSpecification, cancelOrderSpecification);
        }
    }

    private void placeAndCancel(final Exchange exchange, final OrderSpecification limitOrderSpecification, final CancelOrderSpecification cancelOrderSpecification)
    {
        exchange.placeOrder(limitOrderSpecification, IGNORE_EXECUTIONS);
        exchange.cancelOrder(cancelOrderSpecification, IGNORE_EXECUTIONS);
    }

    private static Exchange setupExchange(final BenchmarkState benchmarkState, final int size)
    {
        final Exchange exchange = new Exchange();
        for(int i = 0; i < size; i++) setupOrder(exchange, benchmarkState.nextLimitOrder().setId(String.valueOf(i)).setAttributionId(2).build());
        return exchange;
    }

    private static void setupOrder(final Exchange exchange, final OrderSpecification limitOrderSpecification)
    {
        exchange.placeOrder(limitOrderSpecification, IGNORE_EXECUTIONS);
    }

    private static class BenchmarkState
    {
        private final Random randomNumberGenerator = new Random(0);
        private final Sequence priceSequence = Sequence.gaussianSequence(randomNumberGenerator, 1000000);
        private final Sequence quantitySequence = Sequence.uniformSequence(randomNumberGenerator, 1000000);

        private final OrderSpecification.Builder limitOrderSpecification = OrderSpecification.newBuilder();
        private final CancelOrderSpecification.Builder cancelOrderSpecification = CancelOrderSpecification.newBuilder();

        private final Exchange[] exchanges = new Exchange[]
        {
                setupExchange(this, 0),
                setupExchange(this, 10),
                setupExchange(this, 100),
                setupExchange(this, 1000),
                setupExchange(this, 10000),
                setupExchange(this, 100000)
        };

        public Exchange getExchange(final int size)
        {
            if(size < 10) return exchanges[0];
            else if(size < 100) return exchanges[1];
            else if(size < 1000) return exchanges[2];
            else if(size < 10000) return exchanges[3];
            else if(size < 100000) return exchanges[4];
            return exchanges[5];
        }

        public OrderSpecification.Builder nextLimitOrder()
        {
            final double price = nextPrice();
            final double quantity = nextQuantity();
            return limitOrderSpecification.
                    clearId().
                    clearAttributionId().
                    setSymbol("???").
                    setSide(sideForPrice(price)).
                    setPrice(toTicks(price)).
                    setQuantity(toLots(quantity)).
                    setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
        }

        public CancelOrderSpecification.Builder cancelForLimitOrder(final OrderSpecification limitOrderSpecification)
        {
            return cancelOrderSpecification.
                    clearId().
                    clearAttributionId().
                    setSymbol("???").
                    setCancelId(limitOrderSpecification.getId());
        }

        private double nextPrice()
        {
            return 100.0 + 10.0 * priceSequence.next();
        }

        private double nextQuantity()
        {
            return 10.0 * (1 + (10 * quantitySequence.next()));
        }

        private Side sideForPrice(final double price)
        {
            return (price >= 100.0)? Side.SELL : Side.BUY;
        }
    }
}


