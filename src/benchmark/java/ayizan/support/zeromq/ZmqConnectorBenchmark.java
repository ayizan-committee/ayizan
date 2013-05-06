package ayizan.support.zeromq;

import ayizan.kernel.Executors;
import ayizan.message.Messages.Packet;
import ayizan.message.Messages.PacketOrBuilder;
import ayizan.support.zeromq.ZmqConnector.Publication;
import ayizan.support.zeromq.ZmqConnector.Subscription;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.protobuf.ByteString;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class ZmqConnectorBenchmark extends SimpleBenchmark
{
    private static final ZmqTopic TOPIC = new ZmqTopic("tcp://127.0.0.1:5555");

    @Param({"1000000"}) private int messageCount;
    private BenchmarkState benchmarkState;

    public static void main(String... arguments) throws Exception
    {
        Runner.main(ZmqConnectorBenchmark.class, arguments);
    }

    @Override
    protected void setUp() throws Exception
    {
        benchmarkState = new BenchmarkState();
    }

    public void timeSendReceive(final int iterations) throws Exception
    {
        try {
            for(int i = iterations; i-- != 0;) {
                final Subscription subscription = benchmarkState.subscribe();
                final Notifier notifier = new Notifier(messageCount);
                while(notifier.count < messageCount) subscription.receive(notifier);
            }
        }
        finally {
            benchmarkState.publish().countDown();
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        benchmarkState.close();
    }

    private static class Notifier implements Subscription.Notifier
    {
        private final int messageCount;
        private int count;

        public Notifier(final int messageCount)
        {
            this.messageCount = messageCount;
        }

        @Override
        public boolean next(final PacketOrBuilder packet)
        {
            return (++count < messageCount);
        }
    }

    private static class BenchmarkState implements Closeable
    {
        private final ExecutorService executorService;
        private final Subscription subscription;
        private final Publication publication;
        private final ZmqConnector connector;
        private final CountDownLatch countDownLatch;

        private BenchmarkState() throws Exception
        {
            this.executorService =  Executors.newExecutor();
            this.connector = new ZmqConnector();
            this.subscription = connector.subscribe(TOPIC);
            this.publication = connector.publish(TOPIC);
            this.countDownLatch = start();
        }

        public ZmqConnector.Subscription subscribe()
        {
            return subscription;
        }

        public CountDownLatch publish()
        {
            return countDownLatch;
        }

        @Override
        public void close() throws IOException
        {
            executorService.shutdownNow();
            publication.close();
            subscription.close();
            connector.close();
        }

        private CountDownLatch start() throws Exception
        {
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch endLatch = new CountDownLatch(1);
            executorService.submit(new Runnable()
            {
                private final Packet.Builder packet = Packet.newBuilder();
                private final ByteBuffer buffer = ByteBuffer.allocate(128);

                @Override
                public void run()
                {
                    startLatch.countDown();
                    for(int i = 1; endLatch.getCount() > 0 && !Thread.currentThread().isInterrupted(); i++) {
                        ((ByteBuffer) buffer.clear()).putLong(System.nanoTime());
                        packet.setId(i).setSchema(0).setPayload(ByteString.copyFrom((ByteBuffer) buffer.flip()));
                        publication.publish(packet);
                    }
                }
            });
            startLatch.await();
            return endLatch;
        }
    }
}
