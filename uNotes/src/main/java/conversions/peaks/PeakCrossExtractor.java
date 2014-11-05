package conversions.peaks;

import com.sun.istack.internal.NotNull;

import java.util.Vector;

public class PeakCrossExtractor {   //TODO rename

    private Vector<double[]> mySpectrum;
    private Vector<Vector<Peak>> myPeaks;
    private double myTimeStep;
    private double mySensitivity;
    private int myTimeSensitivity;


    public PeakCrossExtractor(double timeStep, double sensitivity, int timeSensitivity) {
        myTimeStep = timeStep;
        mySensitivity = sensitivity;
        myPeaks = new Vector<Vector<Peak>>();
        myTimeSensitivity = timeSensitivity;
    }

    public void loadSpectrum(@NotNull Vector<double[]> spectrum) {
        this.mySpectrum = spectrum;
    }

    //For given number of note (slice) find peaks in its time series
    public void extract(int slice) {
        int fsize = mySpectrum.size();
        boolean isNote = false;
        double[] series = new double[fsize];
        int leftIndex = 0;
        double centralPower = 0;

        int fallbackCounter = 0;

        for (int i = 0; i < fsize; i++) {
            series[i] = mySpectrum.elementAt(i)[slice];
        }

        Vector<Peak> result_cur = new Vector<Peak>();
        myPeaks.add(result_cur);
        for (int j = 0; j < fsize; ++j) {
            if (isNote) {
                if (series[j] > centralPower) {
                    centralPower = series[j];
                }
                if (series[j] <= mySensitivity) {
                    fallbackCounter++;
                } else {
                    fallbackCounter = 0;
                }
                if (fallbackCounter >= myTimeSensitivity | j == series.length) {
                    Peak result = new Peak(centralPower, leftIndex * myTimeStep, (j - myTimeSensitivity) * myTimeStep);
                    if (j - leftIndex - myTimeSensitivity > myTimeSensitivity) {
                        result_cur.add(result);
                    }
                    isNote = false;
                }
            } else {
                if (series[j] > mySensitivity) {
                    leftIndex = j;
                    centralPower = series[j];
                    isNote = true;
                    fallbackCounter = 0;
                }
            }
        }
    }

    public Vector<Vector<Peak>> getPeaks() {
        return myPeaks;
    }

}
