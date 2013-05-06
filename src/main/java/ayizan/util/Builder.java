package ayizan.util;

public interface Builder<T>
{
    T build();

    Builder<T> clear();
}
