package conversions;

import com.sun.istack.internal.NotNull;

import java.util.ArrayList;

public class Spectrum {
    private ArrayList<double[]> myPowerSpectrum;

    private double myTimeZeroPoint;
    private double myFrequencyZeroPoint;

    private double myTimeStep;
    private double myFrequencyStep;

    private static final int ALIGNMENT_LOCALIZATION_FACTOR = 20;

    public Spectrum(@NotNull ArrayList<double[]> power, double timeZeroPoint, double frequencyZeroPoint, double timeStep, double frequencyStep) {
        myPowerSpectrum = power;

        myTimeZeroPoint = timeZeroPoint;
        myFrequencyZeroPoint = frequencyZeroPoint;

        myTimeStep = timeStep;
        myFrequencyStep = frequencyStep;
    }

    public void addFrame(double[] frame) {
        myPowerSpectrum.add(frame);
    }

    public ArrayList<double[]> getPowerSpectrum() {
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

    public ArrayList<double[]> getAignmentPowerSpectrum() {
        ArrayList<double[]> alignmentPowerSpectrum = new ArrayList<double[]>();

        int spectrumSize = myPowerSpectrum.get(0).length;
        int range = spectrumSize / ALIGNMENT_LOCALIZATION_FACTOR;

        for (int i = 0; i < myPowerSpectrum.size(); ++i) {
            double[] section = myPowerSpectrum.get(i);

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

            double[] alignmentSection = new double[spectrumSize];

            for (int k = 0; k < spectrumSize; ++k) {
                alignmentSection[k] = section[k] - rangeMean[k];
            }

            alignmentPowerSpectrum.add(i, alignmentSection);
        }

        return alignmentPowerSpectrum;
    }

}
