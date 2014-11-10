package conversions.peaks;

import com.sun.istack.internal.NotNull;

import java.util.ArrayList;

public class PeakCrossExtractor {

    private ArrayList<double[]> mySpectrum;
    private ArrayList<ArrayList<Peak>> myPeaks;
    private double myTimeStep;
    private double myFreqStep;
    private double mySensitivity;

    public PeakCrossExtractor(double timeStep, double freqStep, double sensitivity){
        myTimeStep = timeStep;
        myFreqStep = freqStep;
        mySensitivity = sensitivity;
        myPeaks = new ArrayList<ArrayList<Peak>>();
    }

    public void loadSpectrum(@NotNull ArrayList<double[]> spectrum){
        this.mySpectrum = spectrum;
    }

    public void extract(int slice){
        int fsize = mySpectrum.size();
        double[] series = new double[fsize];
        boolean decline = false;
        boolean declineNew = false;
        double centralFrequency = 0;
        double leftFrequency = 0;
        double centralPower = 0;
        double leftPower = 0;
        double lowerPower = -1000;
        double upperPower = 1000;

        for (int i = 0; i < fsize; i++) {
            series[i] = mySpectrum.get(i)[slice];
        }


        leftFrequency = 0;
        leftPower = series[0];
        decline = false;
        ArrayList<Peak> result_cur = new ArrayList<Peak>();
        myPeaks.add(result_cur);
        for (int j = 1; j < fsize - 1; ++j) {
            if ((series[j - 1] <= series[j]) && (series[j + 1] <= series[j])) {
                if (decline){
                    centralFrequency = myTimeStep * j;
                    centralPower = series[j];
                }
                if (series[j] - lowerPower > mySensitivity) {
                    declineNew = true;
                    upperPower = series[j];
                }
            }
            if ((series[j - 1] >= series[j]) && (series[j + 1] >= series[j])) {
                if (upperPower - series[j] > mySensitivity) {
                    declineNew = false;
                    lowerPower = series[j];
                }
            }
            if (decline != declineNew) {
                if (declineNew) {
                    centralFrequency = myTimeStep * j;
                    centralPower = series[j];
                } else {
                    double noise = (leftPower + series[j]) * 0.5;
                    result_cur.add(new Peak(centralPower, noise, centralFrequency, myTimeStep * j - leftFrequency));
                    leftFrequency = myTimeStep * j;
                    leftPower = series[j];
                }
                decline = declineNew;
            }
        }

    }

    public ArrayList<ArrayList<Peak>> getPeaks(){
        return myPeaks;
    }

}
