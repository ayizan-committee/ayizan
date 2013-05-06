package ayizan.service.exchange;

import ayizan.benchmark.Sequence;
import ayizan.domain.Executions.AcceptExecutionOrBuilder;
import ayizan.domain.Executions.CancelExecutionOrBuilder;
import ayizan.domain.Executions.ReplaceExecutionOrBuilder;
import ayizan.domain.Executions.StatusExecutionOrBuilder;
import ayizan.domain.Executions.TradeExecutionOrBuilder;
import ayizan.domain.Orders.PlaceOrderSpecificationOrBuilder;
import ayizan.domain.Orders.Side;
import ayizan.domain.Orders.TimeInForce;
import ayizan.domain.orderbook.InstructionIdGenerator;
import ayizan.message.exchange.Instructions.CancelOrderInstruction;
import ayizan.message.exchange.Instructions.CancelOrderInstructionOrBuilder;
import ayizan.message.exchange.Instructions.PlaceOrderInstruction;
import ayizan.message.exchange.Instructions.PlaceOrderInstructionOrBuilder;
import ayizan.service.ExecutionVenue.ExecutionCallback;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Random;

import static ayizan.domain.StandardUnits.toLots;
import static ayizan.domain.StandardUnits.toTicks;

public class ExchangeBenchmark extends SimpleBenchmark
{
    private static final String SYMBOL = Exchange.DEFAULT_INSTRUMENT.getSymbol();
    private static final ExecutionCallback IGNORE_EXECUTIONS = new StubExecutionCallback();

    @Param({"100", "1000000"}) private int size;
    private BenchmarkState benchmarkState;

    public static void main(String... arguments) throws Exception
    {
        Runner.main(ExchangeBenchmark.class, arguments);
    }

    @Override
    public void setUp()
    {
        benchmarkState = new BenchmarkState();
    }

    public void timePlaceAndCancelWithOrders(final int iterations)
    {
        for(int i = iterations; i-- != 0;) {
            final PlaceOrderInstruction.Builder placeOrderInstruction = benchmarkState.nextLimitOrder();
            final CancelOrderInstruction.Builder cancelOrderInstruction = benchmarkState.cancelForOrder(placeOrderInstruction.getPlaceOrderOrBuilder());

            placeAndCancel(benchmarkState.getExchange(size), placeOrderInstruction, cancelOrderInstruction);
        }
    }


    private void placeAndCancel(final Exchange exchange, final PlaceOrderInstructionOrBuilder placeOrderInstruction, final CancelOrderInstructionOrBuilder cancelOrderInstruction)
    {
        exchange.placeOrder(placeOrderInstruction.getPlaceOrderOrBuilder(), IGNORE_EXECUTIONS);
        exchange.cancelOrder(cancelOrderInstruction.getCancelOrderOrBuilder(), IGNORE_EXECUTIONS);
    }

    private static Exchange setupExchange(final BenchmarkState benchmarkState, final int size)
    {
        final Exchange exchange = new Exchange();
        for(int i = 0; i < size; i++) {
            final PlaceOrderInstruction.Builder placeOrderInstruction = benchmarkState.nextLimitOrder();
            placeOrderInstruction.getPlaceOrderBuilder().setId(String.valueOf(i)).setAttributionId(2);
            setupOrder(exchange, placeOrderInstruction);
        }
        return exchange;
    }

    private static void setupOrder(final Exchange exchange, final PlaceOrderInstructionOrBuilder placeOrderInstruction)
    {
        exchange.placeOrder(placeOrderInstruction.getPlaceOrderOrBuilder(), IGNORE_EXECUTIONS);
    }

    private static class BenchmarkState
    {
        private final Random randomNumberGenerator;
        private final Sequence priceSequence;
        private final Sequence quantitySequence;

        private final InstructionIdGenerator instructionIdGenerator;
        private final PlaceOrderInstruction.Builder placeOrderInstruction;
        private final CancelOrderInstruction.Builder cancelOrderInstruction;
        private final Exchange[] exchanges;

        private BenchmarkState()
        {
            this.randomNumberGenerator = new Random(0);
            this.priceSequence = Sequence.gaussianSequence(randomNumberGenerator, 1000000);
            this.quantitySequence = Sequence.gaussianSequence(randomNumberGenerator, 1000000);
            this.instructionIdGenerator = new InstructionIdGenerator();
            this.placeOrderInstruction = PlaceOrderInstruction.newBuilder();
            this.cancelOrderInstruction = CancelOrderInstruction.newBuilder();
            this.exchanges  = new Exchange[] {
                    setupExchange(this, 0),
                    setupExchange(this, 10),
                    setupExchange(this, 100),
                    setupExchange(this, 1000),
                    setupExchange(this, 10000),
                    setupExchange(this, 100000)
            };
        }


        public Exchange getExchange(final int size)
        {
            if(size < 10) return exchanges[0];
            else if(size < 100) return exchanges[1];
            else if(size < 1000) return exchanges[2];
            else if(size < 10000) return exchanges[3];
            else if(size < 100000) return exchanges[4];
            return exchanges[5];
        }

        public PlaceOrderInstruction.Builder nextLimitOrder()
        {
            final double price = nextPrice();
            final double quantity = nextQuantity();
            placeOrderInstruction.getPlaceOrderBuilder().
                    setId(instructionIdGenerator.next()).
                    setAttributionId(1).
                    setSymbol(SYMBOL).
                    setSide(sideForPrice(price)).
                    setPrice(toTicks(price)).
                    setQuantity(toLots(quantity)).
                    setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
            return placeOrderInstruction;
        }

        public CancelOrderInstruction.Builder cancelForOrder(final PlaceOrderSpecificationOrBuilder placeOrderSpecification)
        {
            cancelOrderInstruction.getCancelOrderBuilder().
                    setId(instructionIdGenerator.next()).
                    setAttributionId(placeOrderSpecification.getAttributionId()).
                    setSymbol(placeOrderSpecification.getSymbol()).
                    setCancelId(placeOrderSpecification.getId());
            return cancelOrderInstruction;
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
    private static class StubExecutionCallback implements ExecutionCallback
    {
        @Override
        public void start()
        {
        }

        @Override
        public void notify(final AcceptExecutionOrBuilder acceptExecution)
        {
        }

        @Override
        public void notify(final TradeExecutionOrBuilder acceptExecution)
        {
        }

        @Override
        public void notify(final ReplaceExecutionOrBuilder acceptExecution)
        {
        }

        @Override
        public void notify(final CancelExecutionOrBuilder acceptExecution)
        {
        }

        @Override
        public void notify(final StatusExecutionOrBuilder acceptExecution)
        {
        }

        @Override
        public void commit()
        {
        }
    }

}


