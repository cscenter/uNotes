package conversions;

import com.sun.istack.internal.NotNull;
import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;

public class WaveletSpectrumTransform implements Transformation {

    public static final double ALPHA = Math.sqrt(2.0);
    private static final double B = 2.0;

    private int myWindowLength;
    private int myTimeStepLength;

    private ArrayList<double[]> myWaveletsArguments;
    private double[] myNormalizingFactor;
    private ArrayList<Complex[]> myWavelet;

    public WaveletSpectrumTransform(int windowLength, int timeStepLength) {
        myWindowLength = windowLength;
        myTimeStepLength = timeStepLength;
        beforeCounting(true);
    }

    public WaveletSpectrumTransform(@NotNull Spectrum input) {

        int windowLength;
        if (!input.getPowerSpectrum().isEmpty()) {
            windowLength = input.getPowerSpectrum().get(0).length;
        } else return;

        myWindowLength = windowLength;
        myTimeStepLength = 1;
        beforeCounting(false);
    }

    public WaveletSpectrumTransform(@NotNull Spectrum input, double[] counts) {

        if (counts == null) {
            throw new NullPointerException("double[] counts is null");
        }

        int windowLength;
        if (!input.getPowerSpectrum().isEmpty()) {
            windowLength = input.getPowerSpectrum().get(0).length;
        } else return;

        myWindowLength = windowLength;
        myTimeStepLength = 1;

        double frequencyStep = input.getFrequencyStep();

        int countsLength = counts.length;

        if ((counts[0] < 1.9 * frequencyStep / ALPHA) || (counts[countsLength - 1] > (myWindowLength - 0.9) * frequencyStep / ALPHA)) {
            throw new IllegalArgumentException("your counts are out of wavelet range");
        }

        for (int i = 1; i < countsLength; ++i) {
            if (counts[i] < counts[i - 1]) {
                throw new IllegalArgumentException("your counts are not sorted");
            }
        }

        myWaveletsArguments = new ArrayList<double[]>();
        int timeShift = myWindowLength / 2;

        for (double count : counts) {
            double[] arguments = new double[myWindowLength];
            for (int k = 0; k < myWindowLength; ++k) {
                arguments[k] = (k - timeShift) * frequencyStep / count;
            }
            myWaveletsArguments.add(arguments);
        }

        myNormalizingFactor = new double[countsLength];

        for (int i = 0; i < myNormalizingFactor.length; ++i) {
            double[] arguments = myWaveletsArguments.get(i);
            for (int k = 0; k < myWindowLength; ++k) {
                myNormalizingFactor[i] += Math.exp(-1.0 * Math.pow(arguments[k], 2.0) / B);
            }
        }

        myWavelet = new ArrayList<Complex[]>();
        Complex[] section;

        for (int i = 0; i < countsLength; ++i) {
            double[] arguments = myWaveletsArguments.get(i);
            section = new Complex[myWindowLength];
            for (int k = 0; k < myWindowLength; ++k) {
                double arg = 2.0 * Math.PI * arguments[k];
                section[k] = new Complex(Math.cos(arg), -Math.sin(arg));
            }
            myWavelet.add(section);
        }

    }

    public ArrayList<double[]> spectrumTransformWithCounts(Spectrum input) {

        ArrayList<double[]> currentSpectrum = new ArrayList<double[]>();

        int scaleLength = myWaveletsArguments.size();

        for (int sectionNum = 0; sectionNum < input.getPowerSpectrum().size(); ++sectionNum) {
            double[] wav = new double[myWindowLength];
            double[] scalogramsSection = new double[scaleLength];

            System.arraycopy(input.getPowerSpectrum().get(sectionNum), 0, wav, 0, myWindowLength);

            Complex waveletAmplitude;
            Complex[] section;

            for (int i = 0; i < scaleLength; ++i) {
                section = myWavelet.get(i);
                waveletAmplitude = new Complex(0.0, 0.0);
                for (int k = 0; k < myWindowLength; ++k) {
                    Complex arg = new Complex(section[k].getReal(), section[k].getImaginary());
                    arg = arg.multiply(wav[k]);
                    waveletAmplitude = waveletAmplitude.add(arg);
                }
                waveletAmplitude = waveletAmplitude.divide(myNormalizingFactor[i]);

                scalogramsSection[i] = Math.pow(waveletAmplitude.abs(), 2.0);
            }

            currentSpectrum.add(scalogramsSection);
        }
        return currentSpectrum;
    }

    private void beforeCounting(boolean isModulate) {

        int scaleLength = myWindowLength - 2;

        myWaveletsArguments = new ArrayList<double[]>();
        int timeShift = myWindowLength / 2;

        for (int i = 0; i < scaleLength; ++i) {
            double[] arguments = new double[myWindowLength];
            for (int k = 0; k < myWindowLength; ++k) {
                arguments[k] = (k - timeShift) * ALPHA / (i + 2);
            }
            myWaveletsArguments.add(arguments);
        }

        myNormalizingFactor = new double[scaleLength];

        for (int i = 0; i < myNormalizingFactor.length; ++i) {
            double[] arguments = myWaveletsArguments.get(i);
            for (int k = 0; k < myWindowLength; ++k) {
                myNormalizingFactor[i] += Math.exp(-1.0 * Math.pow(arguments[k], 2.0) / B);
            }
        }

        myWavelet = new ArrayList<Complex[]>();
        Complex[] section;

        if (isModulate) {
            for (int i = 0; i < scaleLength; ++i) {
                double[] arguments = myWaveletsArguments.get(i);
                section = new Complex[myWindowLength];
                for (int k = 0; k < myWindowLength; ++k) {
                    double arg = 2.0 * Math.PI * arguments[k];
                    double rate;
                    rate = Math.exp(-1.0 * Math.pow(arguments[k] / B, 2.0));
                    section[k] = new Complex(rate * Math.cos(arg), -rate * Math.sin(arg));
                }
                myWavelet.add(section);
            }
        } else {
            for (int i = 0; i < scaleLength; ++i) {
                double[] arguments = myWaveletsArguments.get(i);
                section = new Complex[myWindowLength];
                for (int k = 0; k < myWindowLength; ++k) {
                    double arg = 2.0 * Math.PI * arguments[k];
                    section[k] = new Complex(Math.cos(arg), -Math.sin(arg));
                }
                myWavelet.add(section);
            }
        }
    }

    @Override
    public Spectrum spectrumTransform(Spectrum input) {

        double scaleStep = input.getFrequencyStep() / ALPHA;
        double timeStep = input.getTimeStep();

        Spectrum currentSpectrum = new Spectrum(new ArrayList<double[]>(), input.getTimeZeroPoint(),
                2 * input.getFrequencyStep() / ALPHA, timeStep, scaleStep);

        int scaleLength = myWaveletsArguments.size();

        for (int sectionNum = 0; sectionNum < input.getPowerSpectrum().size(); ++sectionNum) {
            double[] wav = new double[myWindowLength];
            double[] scalogramsSection = new double[scaleLength];

            System.arraycopy(input.getPowerSpectrum().get(sectionNum), 0, wav, 0, myWindowLength);

            Complex waveletAmplitude;
            Complex[] section;

            for (int i = 0; i < scaleLength; ++i) {
                section = myWavelet.get(i);
                waveletAmplitude = new Complex(0.0, 0.0);
                for (int k = 0; k < myWindowLength; ++k) {
                    Complex arg = new Complex(section[k].getReal(), section[k].getImaginary());
                    arg = arg.multiply(wav[k]);
                    waveletAmplitude = waveletAmplitude.add(arg);
                }
                waveletAmplitude = waveletAmplitude.divide(myNormalizingFactor[i]);

                scalogramsSection[i] = Math.pow(waveletAmplitude.abs(), 2.0);
            }

            currentSpectrum.addFrame(scalogramsSection);
        }
        return currentSpectrum;
    }

    @Override
    public Spectrum transform(TimeSeries ownSeries) {
        double scaleStep = 1.0 / ownSeries.getSampleRate() / ALPHA;
        double timeStep = myTimeStepLength * 1.0 / ownSeries.getSampleRate();

        Spectrum currentSpectrum = new Spectrum(new ArrayList<double[]>(), myWindowLength / 2.0 / ownSeries.getSampleRate()
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
                section = myWavelet.get(i);
                waveletAmplitude = new Complex(0.0, 0.0);
                for (int k = 0; k < myWindowLength; ++k) {
                    Complex arg = new Complex(section[k].getReal(), section[k].getImaginary());
                    arg = arg.multiply(wav[k]);
                    //System.out.println(i + "  " + k + "  " + arg);
                    waveletAmplitude = waveletAmplitude.add(arg);
                    //System.out.println(i + "  " + k + "  " + waveletAmplitude);
                }
                waveletAmplitude = waveletAmplitude.divide(myNormalizingFactor[i]);
                //System.out.println(currFrame + "  " + i + "  " +  waveletAmplitude);

                scalogramsSection[i] = Math.pow(waveletAmplitude.abs(), 2.0);
                //System.out.println(currFrame + "  " + i + "  " +  scalogramsSection[i]);
            }

            currentSpectrum.addFrame(scalogramsSection);
        }

        return currentSpectrum;
    }
}
