package ayizan.support.disruptor;

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
import ayizan.kernel.Executors;
import ayizan.message.Messages.Packet;
import ayizan.message.Messages.PacketOrBuilder;
import ayizan.message.exchange.Dictionary.Type;
import ayizan.message.exchange.Events.AcceptExecutionEvent;
import ayizan.message.exchange.Events.CancelExecutionEvent;
import ayizan.message.exchange.Events.ReplaceExecutionEvent;
import ayizan.message.exchange.Events.StatusExecutionEvent;
import ayizan.message.exchange.Events.TradeExecutionEvent;
import ayizan.message.exchange.Instructions.CancelOrderInstruction;
import ayizan.message.exchange.Instructions.CancelReplaceOrderInstruction;
import ayizan.message.exchange.Instructions.OrderStatusInstruction;
import ayizan.message.exchange.Instructions.PlaceOrderInstruction;
import ayizan.service.ExecutionVenue.ExecutionCallback;
import ayizan.service.exchange.Exchange;
import ayizan.support.zeromq.ZmqConnector.Publication;
import ayizan.support.zeromq.ZmqConnector.Subscription;
import ayizan.support.zeromq.ZmqConnector.Subscription.Notifier;
import com.google.protobuf.Message.Builder;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static ayizan.kernel.Executors.closeQuietly;
import static ayizan.util.Exceptions.swallow;

public class MessageService implements Closeable
{
    private final ExecutorService executorService;
    private final RingBuffer<MessageEvent> in;
    private final RingBuffer<MessageEvent> out;
    private final SequenceBarrier outSequenceBarrier;
    private final Set<EventProcessor> eventProcessors;

    public MessageService(final Exchange exchange)
    {
        final EventFactory<MessageEvent> eventFactory = MessageEvent.newEventFactory();
        this.eventProcessors = new HashSet<EventProcessor>();
        this.executorService = Executors.newExecutor();
        this.in = RingBuffer.createMultiProducer(eventFactory, 512 * 1024, new BusySpinWaitStrategy());
        this.out = RingBuffer.createMultiProducer(eventFactory, 1024 * 1024, new BusySpinWaitStrategy());
        this.outSequenceBarrier = out.newBarrier();

        spawn(new BatchEventProcessor<MessageEvent>(in, in.newBarrier(), new Invoker(exchange, out)));
    }

    public void subscribe(final Subscription subscription)
    {
        executorService.submit(new Subscriber(subscription, in));
    }

    public void publish(final Publication publication)
    {
        spawn(new BatchEventProcessor<MessageEvent>(out, outSequenceBarrier, new Publisher(publication)));
    }

    @Override
    public void close() throws IOException
    {
        synchronized(eventProcessors) {
            for(final EventProcessor eventProcessor : eventProcessors) eventProcessor.halt();
            closeQuietly(executorService);
        }
    }

    private void spawn(final EventProcessor eventProcessor)
    {
        executorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    synchronized(eventProcessors) {
                        eventProcessors.add(eventProcessor);
                    }
                    eventProcessor.run();
                }
                finally {
                    synchronized(eventProcessors) {
                        eventProcessors.remove(eventProcessor);
                    }
                }
            }
        });
    }


    private static class Invoker implements EventHandler<MessageEvent>
    {
        private final PlaceOrderInstruction.Builder placeOrderInstruction;
        private final CancelReplaceOrderInstruction.Builder cancelReplaceOrderInstruction;
        private final CancelOrderInstruction.Builder cancelOrderInstruction;
        private final OrderStatusInstruction.Builder orderStatusInstruction;

        private final Invoker.Callback callback;
        private final Exchange exchange;

        private Invoker(final Exchange exchange, final RingBuffer<MessageEvent> ringBuffer)
        {
            this.placeOrderInstruction =  PlaceOrderInstruction.newBuilder();
            this.cancelReplaceOrderInstruction =  CancelReplaceOrderInstruction.newBuilder();
            this.cancelOrderInstruction =  CancelOrderInstruction.newBuilder();
            this.orderStatusInstruction =  OrderStatusInstruction.newBuilder();
            this.callback = new Invoker.Callback(ringBuffer);
            this.exchange = exchange;
        }


        @Override
        public void onEvent(final MessageEvent event, final long sequence, final boolean endOfBatch) throws Exception
        {
            switch(event.type()) {
                case Type.PLACE_ORDER_INSTRUCTION_VALUE:
                    event.translateTo(Type.PLACE_ORDER_INSTRUCTION_VALUE, placeOrderInstruction);
                    exchange.placeOrder(placeOrderInstruction.getPlaceOrderOrBuilder(), callback);
                    return;
                case Type.CANCEL_REPLACE_ORDER_INSTRUCTION_VALUE:
                    event.translateTo(Type.CANCEL_REPLACE_ORDER_INSTRUCTION_VALUE, cancelReplaceOrderInstruction);
                    exchange.cancelReplaceOrder(cancelReplaceOrderInstruction.getCancelReplaceOrderOrBuilder(), callback);
                    return;
                case Type.CANCEL_ORDER_INSTRUCTION_VALUE:
                    event.translateTo(Type.CANCEL_ORDER_INSTRUCTION_VALUE, cancelOrderInstruction);
                    exchange.cancelOrder(cancelOrderInstruction.getCancelOrderOrBuilder(), callback);
                    return;
                case Type.ORDER_STATUS_INSTRUCTION_VALUE:
                    event.translateTo(Type.ORDER_STATUS_INSTRUCTION_VALUE, orderStatusInstruction);
                    exchange.orderStatus(orderStatusInstruction.getOrderStatusOrBuilder(), callback);
                    return;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private static class Callback implements ExecutionCallback, EventTranslatorTwoArg<MessageEvent, Integer, Builder>
        {
            private final AcceptExecutionEvent.Builder acceptExecutionEvent;
            private final TradeExecutionEvent.Builder tradeExecutionEvent;
            private final ReplaceExecutionEvent.Builder replaceExecutionEvent;
            private final CancelExecutionEvent.Builder cancelExecutionEvent;
            private final StatusExecutionEvent.Builder statusExecutionEvent;

            private final RingBuffer<MessageEvent> ringBuffer;

            public Callback(final RingBuffer<MessageEvent> ringBuffer)
            {
                this.acceptExecutionEvent = AcceptExecutionEvent.newBuilder();
                this.tradeExecutionEvent = TradeExecutionEvent.newBuilder();
                this.replaceExecutionEvent = ReplaceExecutionEvent.newBuilder();
                this.cancelExecutionEvent = CancelExecutionEvent.newBuilder();
                this.statusExecutionEvent = StatusExecutionEvent.newBuilder();
                this.ringBuffer = ringBuffer;
            }

            @Override
            public void start()
            {
            }

            @Override
            public void notify(final AcceptExecutionOrBuilder acceptExecution)
            {
                acceptExecutionEvent.setType(Type.ACCEPT_EXECUTION_EVENT).setAccept((AcceptExecution.Builder) acceptExecution);
                ringBuffer.publishEvent(this, Type.ACCEPT_EXECUTION_EVENT_VALUE,  acceptExecutionEvent);
            }

            @Override
            public void notify(final TradeExecutionOrBuilder tradeExecution)
            {
                tradeExecutionEvent.setType(Type.TRADE_EXECUTION_EVENT).setTrade((TradeExecution.Builder) tradeExecution);
                ringBuffer.publishEvent(this, Type.TRADE_EXECUTION_EVENT_VALUE, tradeExecutionEvent);
            }

            @Override
            public void notify(final ReplaceExecutionOrBuilder replaceExecution)
            {
                replaceExecutionEvent.setType(Type.REPLACE_EXECUTION_EVENT).setReplace((ReplaceExecution.Builder) replaceExecution);
                ringBuffer.publishEvent(this, Type.REPLACE_EXECUTION_EVENT_VALUE, replaceExecutionEvent);
            }

            @Override
            public void notify(final CancelExecutionOrBuilder cancelExecution)
            {
                cancelExecutionEvent.setType(Type.CANCEL_EXECUTION_EVENT).setCancel((CancelExecution.Builder) cancelExecution);
                ringBuffer.publishEvent(this, Type.CANCEL_EXECUTION_EVENT_VALUE, cancelExecutionEvent);
            }

            @Override
            public void notify(final StatusExecutionOrBuilder statusExecution)
            {
                statusExecutionEvent.setType(Type.STATUS_EXECUTION_EVENT).setStatus((StatusExecution.Builder) statusExecution);
                ringBuffer.publishEvent(this, Type.STATUS_EXECUTION_EVENT_VALUE, statusExecutionEvent);
            }

            @Override
            public void commit()
            {
            }

            @Override
            public void translateTo(final MessageEvent event, final long sequence, final Integer type, final Builder message)
            {
                event.translateFrom(type, message);
            }
        }
    }

    private static class Publisher  implements EventHandler<MessageEvent>
    {
        private final Publication publication;
        private final Packet.Builder packet;

        private Publisher(final Publication publication)
        {
            this.packet = Packet.newBuilder();
            this.publication = publication;
        }

        @Override
        public void onEvent(final MessageEvent event, final long sequence, final boolean endOfBatch) throws Exception
        {
            event.translateTo(packet);
            publication.publish(packet.setId(sequence));
        }
    }

    private static class Subscriber implements Runnable, Notifier, EventTranslatorOneArg<MessageEvent, PacketOrBuilder>
    {
        private final Subscription subscription;
        private final RingBuffer<MessageEvent> ringBuffer;

        private Subscriber(final Subscription subscription, final RingBuffer<MessageEvent> ringBuffer)
        {
            this.subscription = subscription;
            this.ringBuffer = ringBuffer;
        }

        @Override
        public boolean next(final PacketOrBuilder packet)
        {
            ringBuffer.publishEvent(this, packet);
            return true;
        }

        @Override
        public void run()
        {
            while(isAlive()) {
                try {
                    subscription.receive(this);
                }
                catch(final IOException e) {
                    swallow(e);
                }
            }
        }

        @Override
        public void translateTo(final MessageEvent event, final long sequence, final PacketOrBuilder packet)
        {
            event.translateFrom(packet);
        }

        private boolean isAlive()
        {
            return !Thread.currentThread().isInterrupted();
        }
    }
}
