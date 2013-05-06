package ayizan.support.zeromq;

public class ZmqTopic
{
    private final String uri;

    //TODO: register dictionary
    public ZmqTopic(final String uri)
    {
        this.uri = uri;
    }

    public String getUri()
    {
        return uri;
    }

    public String toString()
    {
        return uri;
    }
}
