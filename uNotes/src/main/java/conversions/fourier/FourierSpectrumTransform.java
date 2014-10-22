package conversions.fourier;

import conversions.Spectrum;
import conversions.SpectrumTransform;
import conversions.fourier.util.FFT;

public class FourierSpectrumTransform extends SpectrumTransform {

    public FourierSpectrumTransform(Spectrum input){
        this.input = input;
    }

    public double[] transform(int slice){
        int len = input.getPowerSpectrum().elementAt(slice).length;
        double[] re = input.getPowerSpectrum().elementAt(slice).clone();
        double[] im = new double[re.length];
        FFT fft = new FFT(re.length);
        fft.transform(re,im);
        double[] mag = new double[len / 2];

        for (int i = 0; i < mag.length; i++) {
            mag[i] = Math.sqrt (re[i] * re[i] + im[i] * im[i]);
        }
        return mag;
    }
}
