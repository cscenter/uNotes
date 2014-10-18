package сonversions.fourier;

import com.sun.istack.internal.NotNull;
import сonversions.Spectrum;
import сonversions.TimeSeries;
import сonversions.Transformation;
import сonversions.fourier.util.FFT;

import java.util.Vector;

/**
 * Short-time Fourier transform
 */
public class STFT implements Transformation {
    private final static double OUR_EPSILON = 1.0e-09;

    private int myWindowLength;
    private int myTimeStepLength;

    private TimeWindow myWindow;

    /**
     * @param windowLength   number of samples taken for one fast Fourier transform. Must be power of 2.
     * @param timeStepLength number of samples in one time step
     * @param window         window function short-time Fourier transform
     */
    public STFT(int windowLength, int timeStepLength, @NotNull TimeWindow window) {
        myWindowLength = windowLength;
        myTimeStepLength = timeStepLength;
        myWindow = window;
    }

    @Override
    public Spectrum transform(TimeSeries ownSeries) {
        double frequencyStep = ownSeries.getSampleRate() * 1.0 / myWindowLength;
        double timeStep = myTimeStepLength * 1.0 / ownSeries.getSampleRate();

        Spectrum currentSpectrum = new Spectrum(new Vector<double[]>(), myWindowLength / 2.0 / ownSeries.getSampleRate(), 0.0, timeStep, frequencyStep);

        FFT fft = new FFT(myWindowLength);
        double[] window = myWindow.makeWindow(myWindowLength);

        double[] wav = new double[myWindowLength];
        double[] samples = ownSeries.getTrack();

        for (int currFrame = 0; currFrame * myTimeStepLength < ownSeries.getFrameLen(); currFrame++) {
            // zero pad if we run out of samples:
            int zeroPadLen = currFrame * myTimeStepLength + wav.length - ownSeries.getFrameLen();
            if (zeroPadLen < 0) {
                zeroPadLen = 0;
            }

            int wavLen = wav.length - zeroPadLen;

            //for(int i = 0; i < wav.length; i++)
            //    wav[i] = samples[currFrame * myTimeStepLength + i];
            System.arraycopy(samples, currFrame * myTimeStepLength, wav, 0, wavLen);
            for (int i = wavLen; i < wav.length; i++) {
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

            double[] mag = new double[myWindowLength / 2];

            for (int i = 0; i < mag.length; i++) {
                mag[i] = 10 * Math.log10(re[i] * re[i] + im[i] * im[i] + OUR_EPSILON);
            }

            currentSpectrum.addFrame(mag);
        }

        return currentSpectrum;
    }
}
