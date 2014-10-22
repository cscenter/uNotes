package conversions;

import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by User on 18.10.2014.
 */
public class MorletWavelet implements Transformation {

    private static final double ALPHA = Math.sqrt(2.0);
    private static final double B = 2.0;

    private int myWindowLength;
    private int myTimeStepLength;

    private ArrayList<double[]> waveletsArguments;
    private double[] normalizingFactor;
    private ArrayList<Complex[]> wavelet;

    /**
     * @param windowLength   number of samples taken for one wavelet transform. Must be uneven.
     * @param timeStepLength number of samples in one time step
     */
    public MorletWavelet(int windowLength, int timeStepLength) {
        myWindowLength = windowLength;
        myTimeStepLength = timeStepLength;

        int scaleLength = myWindowLength;

        /*
        double maxQuasiFrequency = ALPHA / 2.0;
        double minQuasiFrequency = ALPHA / (myWindowLength - 1.0);
        double dQuasiFrequency = maxQuasiFrequency - minQuasiFrequency / (scaleLength - 1);

        double[] quasiFrequencies = new double [scaleLength];

        for (int i = 0; i < scaleLength; ++i) {
            quasiFrequencies[i] = minQuasiFrequency + i * dQuasiFrequency;
        }
        */
        waveletsArguments = new ArrayList<double[]>();
        int timeShift = myWindowLength / 2;

        for (int i = 0; i < scaleLength; ++i) {
            double[] arguments = new double[myWindowLength];
            for (int k = 0; k < myWindowLength; ++k) {
                arguments[k] = (k - timeShift) * ALPHA ;
                //System.out.println(i + "  " + k + "  " +  arguments[k]);
            }
            waveletsArguments.add(arguments);
        }

        normalizingFactor = new double[scaleLength];

        for (int i = 0; i < normalizingFactor.length; ++i) {
            double[] arguments = waveletsArguments.get(i);
            for (int k = 0; k < myWindowLength; ++k) {
                //System.out.println(i + "  " + k + "  " +  arguments[k]);
                normalizingFactor[i] += Math.exp(-1.0 * Math.pow(arguments[k], 2.0) / B);
            }
            //System.out.println(i + "  " + normalizingFactor[i]);
        }

        wavelet = new ArrayList<Complex[]>();
        Complex[] section;

        for (int i = 0; i < scaleLength; ++i) {
            double[] arguments = waveletsArguments.get(i);
            section = new Complex[myWindowLength];
            for (int k = 0; k < myWindowLength; ++k) {
                double arg = 2.0 * Math.PI * arguments[k];
                double rate = Math.exp(-1.0 * Math.pow(arguments[k] / B, 2.0));
                section[k] = new Complex(rate * Math.cos(arg), -rate * Math.sin(arg));
            }
            wavelet.add(section);
        }

    }

    @Override
    public Spectrum transform(TimeSeries ownSeries) {
        double scaleStep = 1.0 / ownSeries.getSampleRate() / ALPHA;
        double timeStep = myTimeStepLength * 1.0 / ownSeries.getSampleRate();

        Spectrum currentSpectrum = new Spectrum(new Vector<double[]>(), myWindowLength / 2.0 / ownSeries.getSampleRate()
                                                , 0.0, timeStep, scaleStep);

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

            int scaleLength = myWindowLength - 2;
            double[] scalogramsSection = new double[scaleLength];
            Complex waveletAmplitude;
            Complex[] section;

            for (int i = 0; i < scaleLength; ++i) {
                section = wavelet.get(i);
                waveletAmplitude = new Complex(0.0, 0.0);
                for (int k = 0; k < myWindowLength; ++k) {
                    Complex arg = new Complex(section[k].getReal(), section[k].getImaginary());
                    arg = arg.multiply(wav[k]);
                    //System.out.println(i + "  " + k + "  " + arg);
                    waveletAmplitude = waveletAmplitude.add(arg);
                    //System.out.println(i + "  " + k + "  " + waveletAmplitude);
                }
                waveletAmplitude = waveletAmplitude.divide(normalizingFactor[i]);
                //System.out.println(currFrame + "  " + i + "  " +  waveletAmplitude);

                scalogramsSection[i] = Math.pow(waveletAmplitude.abs(), 2.0);
                //System.out.println(currFrame + "  " + i + "  " +  scalogramsSection[i]);
            }

            currentSpectrum.addFrame(scalogramsSection);
        }

        return currentSpectrum;

    }

    public double[] getNormalizingFactor() {
        return normalizingFactor;
    }

    public ArrayList<double[]> getWaveletsArguments() {
        return waveletsArguments;
    }

    public ArrayList<Complex[]> getWavelet() {
        return wavelet;
    }
}
