package ayizan.benchmark;

import java.util.Random;

public class Sequence
{
    private double[] values;
    private int ptr;

    private Sequence(final double[] values)
    {
        this.values = values;
    }

    public static Sequence gaussianSequence(final Random randomNumberGenerator, final int size)
    {
        final double[] values = new double[size];
        for(int i = 0; i < size; i++) values[i] = randomNumberGenerator.nextGaussian();
        return new Sequence(values);
    }


    public static Sequence uniformSequence(final Random randomNumberGenerator, final int size)
    {
        final double[] values = new double[size];
        for(int i = 0; i < size; i++) values[i] = randomNumberGenerator.nextDouble();
        return new Sequence(values);
    }

    public double next()
    {
        return values[ptr++ % values.length];
    }
}
