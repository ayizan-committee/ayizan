package ayizan.io;

import java.io.Closeable;
import java.io.IOException;

import static ayizan.util.Exceptions.swallow;

public class IO
{

    private IO() {}

    public static void closeQuietly(final Closeable closeable)
    {
        if(closeable != null) {
            try {
                closeable.close();
            }
            catch(final IOException e) {
                swallow(e);
            }
        }
    }
}
