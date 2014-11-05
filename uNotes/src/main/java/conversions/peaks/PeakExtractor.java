package conversions.peaks;

import com.sun.istack.internal.NotNull;

import java.util.Vector;

public class PeakExtractor {    //TODO rename

    private Vector<double[]> mySpectrum;
    private Vector<Vector<Peak>> myPeaks;
    private double myTimeStep;  //TODO delete?
    private double myFreqStep;

    public PeakExtractor(double timeStep, double freqStep){
        this.myTimeStep = timeStep;
        this.myFreqStep = freqStep;
    }

    public void loadSpectrum(@NotNull Vector<double[]> spectrum){
        this.mySpectrum = spectrum;
    }

    //For every time step find peaks in frequency spectrum
    public void extract(){
        int fsize = mySpectrum.get(0).length;

        boolean decline;
        boolean declineNew = false;
        double centralFrequency = 0;
        double leftFrequency;
        double centralPower = 0;
        double leftPower;
        myPeaks = new Vector<Vector<Peak>>();
        for (double[] cur : mySpectrum) {
            leftFrequency = 0;
            leftPower = cur[0];
            decline = false;
            Vector<Peak> result_cur = new Vector<Peak>();
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

    public Vector<Vector<Peak>> getPeaks(){
        return myPeaks;
    }

}
