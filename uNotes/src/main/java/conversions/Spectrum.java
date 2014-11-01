package conversions;

import java.util.Vector;

public class Spectrum {
    private Vector<double[]> myPowerSpectrum;

    private double myTimeZeroPoint;
    private double myFrequencyZeroPoint;

    private double myTimeStep;
    private double myFrequencyStep;

    public Spectrum(Vector<double[]> power, double timeZeroPoint, double frequencyZeroPoint, double timeStep, double frequencyStep) {
        myPowerSpectrum = power;

        myTimeZeroPoint = timeZeroPoint;
        myFrequencyZeroPoint = frequencyZeroPoint;

        myTimeStep = timeStep;
        myFrequencyStep = frequencyStep;
    }

    public void addFrame(double[] frame) {
        myPowerSpectrum.add(frame);
    }

    public Vector<double[]> getPowerSpectrum() {
        return myPowerSpectrum;
    }

    public double getTimeZeroPoint() {
        return myTimeZeroPoint;
    }

    public double getFrequencyZeroPoint() {
        return myFrequencyZeroPoint;
    }

    public double getTimeStep() {
        return myTimeStep;
    }

    public double getFrequencyStep() {
        return myFrequencyStep;
    }

    public void alignment(int localization) {
        int spectrumSize = myPowerSpectrum.elementAt(0).length;
        int range = spectrumSize / localization; //TODO: NPE

        System.out.println(range);

        for (int i = 0; i < myPowerSpectrum.size(); ++i) {
            double[] section = myPowerSpectrum.elementAt(i);

            double[] rangeMean = new double[spectrumSize];

            for (int k = 0; k < range; ++k) {
                rangeMean[0] += section[k];
            }

            for (int k = 1; k < range; ++k) {
                rangeMean[k] = rangeMean[k - 1] + section[k + range];
            }

            for (int k = range; k < spectrumSize - range; ++k) {
                rangeMean[k] = rangeMean[k - 1] - section[k - range] + section[k + range];
            }

            for (int k = spectrumSize - range; k < spectrumSize; ++k) {
                rangeMean[k] = rangeMean[k - 1] - section[k - range];
            }
            //////////
            for (int k = 0; k < range; ++k) {
                rangeMean[k] = rangeMean[k] / (range + k);
            }

            for (int k = range; k < spectrumSize - range; ++k) {
                rangeMean[k] = rangeMean[k] / (2 * range);
            }

            for (int k = spectrumSize - range; k < spectrumSize; ++k) {
                rangeMean[k] = rangeMean[k] / (range + (spectrumSize - 1 - k));
            }
            //////////
            for (int k = 0; k < spectrumSize; ++k) {
                section[k] -= rangeMean[k];
            }
        }
    }
}
