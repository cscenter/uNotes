package сonversions.fourier;

import com.sun.istack.internal.NotNull;
import сonversions.Spectrum;
import сonversions.TimeSeries;
import сonversions.Transformation;

import java.util.Vector;

/**
 * Created by Денис on 04.10.2014.
 */
public class STFT implements Transformation {
    private final static double ourEpsilon = 1.0e-09;

    private int myNfft;
    private int myNhop;

    private TimeWindow myWindow;

    public STFT(int nfft, int nhop, @NotNull TimeWindow window) {//TODO: rename variables
        myNfft = nfft;
        myNhop = nhop;
        myWindow = window;
    }

    @Override
    public Spectrum makeTransformation(TimeSeries ownSeries) {
        double frequencyStep = ownSeries.getSampleRate() * 1.0 / myNfft;
        double timeStep = myNhop * 1.0 / ownSeries.getSampleRate();

        Spectrum currentSpectrum = new Spectrum(new Vector<double[]>(), timeStep, frequencyStep);

        FFT fft = new FFT(myNfft, myWindow);
        double[] window = fft.getWindow();

        double[] wav = new double[myNfft];
        double[] samples = ownSeries.getTrack();

        for (int currFrame = 0; currFrame < ownSeries.getFrameLen() / myNhop; currFrame++) {
            // zero pad if we run out of samples:
            int zeroPadLen = currFrame * myNhop + wav.length - ownSeries.getFrameLen();
            if(zeroPadLen < 0) {
                zeroPadLen = 0;
            }
            int wavLen = wav.length - zeroPadLen;

            //for(int i = 0; i<wav.length; i++)
            //    wav[i] = samples[currFrame*nhop + i];
            System.arraycopy(samples, currFrame * myNhop, wav, 0, wavLen);
            for(int i = wavLen; i < wav.length; i++) {
                wav[i] = 0;
            }
            // window waveform
            double[] re = new double[wav.length];
            double[] im = new double[wav.length];
            for (int i = 0; i < wav.length; i++) {
                re[i] = window[i] * wav[i];
                im[i] = 0;
            }

            // take transform
            fft.transform(re, im);

            // Calculate magnitude

            double[] mag = new double[myNfft / 2];

            for(int i = 0; i <mag.length; i++) {
                mag[i] = 10 * Math.log10(re[i] * re[i] + im[i] * im[i] + ourEpsilon);
            }

            currentSpectrum.addFrame(mag);
        }

        return currentSpectrum;
    }
}
