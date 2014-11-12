package conversions;

public interface Transformation {
    public Spectrum transform(TimeSeries ownSeries);

    public Spectrum spectrumTransform(Spectrum input);
}
