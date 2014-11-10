package conversions.peaks;

import com.sun.istack.internal.NotNull;

import java.util.ArrayList;

public class PeakExtractor {

    private ArrayList<double[]> mySpectrum;
    private ArrayList<ArrayList<Peak>> myPeaks;
    private double myTimeStep;
    private double myFreqStep;

    public PeakExtractor(double timeStep, double freqStep){
        this.myTimeStep = timeStep;
        this.myFreqStep = freqStep;
    }

    public void loadSpectrum(@NotNull ArrayList<double[]> spectrum){
        this.mySpectrum = spectrum;
    }

    public void extract(){
        int fsize = mySpectrum.get(0).length;
        int tsize = mySpectrum.size();
        boolean decline = false;
        boolean declineNew = false;
        double centralFrequency = 0;
        double leftFrequency = 0;
        double centralPower = 0;
        double leftPower = 0;
        myPeaks = new ArrayList<ArrayList<Peak>>();
        for (double[] cur : mySpectrum) {
            leftFrequency = 0;
            leftPower = cur[0];
            decline = false;
            ArrayList<Peak> result_cur = new ArrayList<Peak>();
            myPeaks.add(result_cur);
            for (int j = 1; j < fsize - 1; ++j) {
                if ((cur[j - 1] <= cur[j]) && (cur[j + 1] <= cur[j])) {
                    declineNew = true;
                }
                if ((cur[j - 1] >= cur[j]) && (cur[j + 1] >= cur[j])) {
                    declineNew = false;
                }
                if (decline != declineNew) {
                    if (declineNew) {
                        centralFrequency = myFreqStep * j;
                        centralPower = cur[j];
                    } else {
                        double noise = (leftPower + cur[j]) * 0.5;
                        result_cur.add(new Peak(centralPower, noise, centralFrequency, myFreqStep * j - leftFrequency));
                        leftFrequency = myFreqStep * j;
                        leftPower = cur[j];
                    }
                    decline = declineNew;
                }
            }
        }
    }

    public ArrayList<ArrayList<Peak>> getPeaks(){
        return myPeaks;
    }

}
