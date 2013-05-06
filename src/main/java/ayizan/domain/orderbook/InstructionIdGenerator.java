package ayizan.domain.orderbook;

import ayizan.domain.IdGenerator;

public class InstructionIdGenerator implements IdGenerator<String>
{
    private long id;

    public InstructionIdGenerator()
    {
        id = 0;
    }

    @Override
    public String next()
    {
        return String.valueOf(++id);
    }
}
