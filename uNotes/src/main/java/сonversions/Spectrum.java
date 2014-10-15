package сonversions;

import java.util.Vector;

public class Spectrum {
    private Vector<double[]> myPower;
    private double myTimeStep;
    private double myFrequencyStep;

    public Spectrum(Vector<double[]> newPower, double newTimeStep, double newFrequencyStep) {
        myPower = newPower;
        myTimeStep = newTimeStep;
        myFrequencyStep = newFrequencyStep;
    }

    public void addFrame(double[] newFrame) {
        getPowerSpectrum().add(newFrame);
    }

    public Vector<double[]> getPowerSpectrum() {
        return myPower;
    }

    public double getTimeStep() {
        return myTimeStep;
    }

    public double getFrequencyStep() {
        return myFrequencyStep;
    }
}
