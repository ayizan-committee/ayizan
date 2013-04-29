package ayizan.util;

import ayizan.domain.Orders.Side;

public interface Builder<T>
{

    T build();

    Builder<T> clear();
}
