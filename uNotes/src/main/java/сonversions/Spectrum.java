package сonversions;

import java.util.Vector;

public class Spectrum {
    private Vector<double[]> myPowerSpectrum;
    private double myTimeStep;
    private double myFrequencyStep;

    public Spectrum(Vector<double[]> power, double timeStep, double frequencyStep) {
        myPowerSpectrum = power;
        myTimeStep = timeStep;
        myFrequencyStep = frequencyStep;
    }

    public void addFrame(double[] frame) {
        myPowerSpectrum.add(frame);
    }

    public Vector<double[]> getPowerSpectrum() {
        return myPowerSpectrum;
    }

    public double getTimeStep() {
        return myTimeStep;
    }

    public double getFrequencyStep() {
        return myFrequencyStep;
    }
}
