package ayizan.support.zeromq;

import ayizan.message.Messages.Packet;
import ayizan.message.Messages.PacketOrBuilder;
import org.zeromq.ZMQ;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import static ayizan.util.Exceptions.swallow;

public class ZmqConnector implements Closeable
{
    private static final byte[] ALL_TOPICS = new byte[0];
    private static final int MAX_FRAME_SIZE = 1500;

    private final ZMQ.Context context;

    //TODO: pass in network configuration & wait policy
    public ZmqConnector()
    {
        this.context = ZMQ.context(1);
    }

    public Subscription subscribe(final ZmqTopic topic)
    {
        final ZMQ.Socket socket = context.socket(ZMQ.SUB);
        socket.bind(topic.getUri());
        socket.subscribe(ALL_TOPICS);
        return new Subscription(socket);
    }


    public Publication publish(final ZmqTopic topic)
    {
        final ZMQ.Socket socket = context.socket(ZMQ.PUB);
        socket.connect(topic.getUri());
        return new Publication(socket);
    }

    @Override
    public void close() throws IOException
    {
        //TODO: close all open publications & subscriptions
        context.term();
    }

    public static class Subscription implements Closeable
    {
        private final ZMQ.Socket socket;
        private final Packet.Builder packet;
        private final byte[] buffer;

        public interface Notifier
        {
            boolean next(PacketOrBuilder packet);
        }

        private Subscription(final ZMQ.Socket socket)
        {
            this.buffer = new byte[MAX_FRAME_SIZE];
            this.packet = Packet.newBuilder();
            this.socket = socket;
        }

        public void receive(final Notifier notifier) throws IOException
        {
            int waitPolicy = 0;
            do {
                final int length = socket.recv(buffer, 0, MAX_FRAME_SIZE, waitPolicy);
                if(length <= 0) break;
                else {
                    try {
                        packet.mergeFrom(buffer, 0, length);
                        waitPolicy = ZMQ.DONTWAIT;
                    }
                    catch(final Throwable throwable) {
                        swallow(throwable);
                        break;
                    }
                }
            }
            while(notifier.next(packet));
        }

        @Override
        public void close() throws IOException
        {
            socket.close();
        }
    }


    public static class Publication implements Closeable
    {
        private final ZMQ.Socket socket;
        private final ByteBuffer buffer;

        public Publication(final ZMQ.Socket socket)
        {
            this.buffer = ByteBuffer.allocate(MAX_FRAME_SIZE);
            this.socket = socket;
        }

        public void publish(final Packet.Builder packet)
        {
            packet.build().toByteString().copyTo((ByteBuffer) buffer.clear());
            //socket.sendZeroCopy(buffer, buffer.position(), 0); //Requires DirectByteBuffer
            socket.send(buffer.array(), buffer.arrayOffset(), buffer.position(), 0);
        }

        @Override
        public void close() throws IOException
        {
            socket.close();
        }
    }
}
