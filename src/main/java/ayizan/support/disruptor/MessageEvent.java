package ayizan.support.disruptor;

import ayizan.message.Messages.Packet;
import ayizan.message.Messages.PacketOrBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.lmax.disruptor.EventFactory;

import static ayizan.util.Exceptions.rethrow;
import static ayizan.util.Preconditions.checkArgument;

public class MessageEvent
{

    public static EventFactory<MessageEvent> newEventFactory()
    {
        return new EventFactory<MessageEvent>()
        {
            @Override
            public MessageEvent newInstance()
            {
                return new MessageEvent();
            }
        };
    }

    private int type;
    private ByteString buffer;

    private MessageEvent()
    {
        this.buffer = null;
    }


    public int type()
    {
        return type;
    }


    public void translateTo(final int type, final Message.Builder message)
    {
        checkArgument(this.type == type, "Type mismatch '%s' != '%s'", this.type, type);
        try {
            message.mergeFrom(buffer);
        }
        catch(final InvalidProtocolBufferException e) {
            throw rethrow(e);
        }
    }


    public void translateFrom(final int type, final Message.Builder message)
    {
        this.type = type;
        this.buffer = message.build().toByteString();
    }

    public void translateFrom(final PacketOrBuilder packet)
    {
        type = packet.getSchema();
        buffer = packet.getPayload();
    }

    public void translateTo(final Packet.Builder packet)
    {
        packet.setSchema(type).setPayload(buffer);
    }
}
