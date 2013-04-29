package ayizan.domain.orderbook;

import ayizan.util.IdGenerator;

public class ExecutionIdGenerator implements IdGenerator<Long>
{
    private long id;

    public ExecutionIdGenerator()
    {
        id = 0;
    }

    @Override
    public Long next()
    {
        return ++id;
    }
}
