package сonversions;

import java.util.Vector;

/**
 * Created by Денис on 04.10.2014.
 */
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
        getPower().add(newFrame);
    }

    public Vector<double[]> getPower() {
        return myPower;
    }

    public double getTimeStep() {
        return myTimeStep;
    }

    public double getFrequencyStep() {
        return myFrequencyStep;
    }
}
