package conversions.fourier;

import conversions.Spectrum;
import conversions.fourier.util.FFT;

public class SpectrumTransformer {

    private Spectrum input;

    public SpectrumTransformer(Spectrum input){
        this.input = input;
    }

    public double[] transform(int slice){
        int len = input.getPowerSpectrum().elementAt(slice).length;
        double[] spectrumSlice = input.getPowerSpectrum().elementAt(slice);
        double[] re = new double[len * 2];
        for (int i = 0; i < len; ++i){
            re[i] = spectrumSlice[i];
            re[2 * len - 1 - i] = spectrumSlice[i];
        }
        double[] im = new double[re.length];
        FFT fft = new FFT(re.length);
        fft.transform(re,im);
        double[] mag = new double[len];

        for (int i = 0; i < mag.length; i++) {
            mag[i] = Math.sqrt (re[i] * re[i] + im[i] * im[i]);
        }
        return mag;
    }
}
