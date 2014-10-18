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
}
