package conversions;

/**
 * Created by User on 22.10.2014.
 */
public abstract class SpectrumTransform {
    protected Spectrum input;

    public abstract double[] transform(int slice);
}
