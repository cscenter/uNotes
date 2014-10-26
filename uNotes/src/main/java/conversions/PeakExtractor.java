package conversions;

import java.util.Vector;

public class PeakExtractor {

    private Vector<double[]> raws; //todo rename
    private Vector<Vector<Peak>> peaks;
    private double timeStep;
    private double freqStep;

    public PeakExtractor(double timeStep, double freqStep){
        this.timeStep = timeStep;
        this.freqStep = freqStep;
    }

    public void loadRaws(Vector<double[]> raws){
        this.raws = raws;
    }

    public void extract(){
        int fsize = raws.get(0).length; //todo check NPE
        int tsize = raws.size();
        int decline = 0;
        int declineNew = 0; // todo boolean
        double c_freq = 0;
        double l_freq = 0;
        double c_pow = 0;
        double l_pow = 0;
        peaks = new Vector<Vector<Peak>>();
        for (double[] cur : raws) {
            l_freq = 0;
            l_pow = cur[0];
            decline = 0;
            Vector<Peak> result_cur = new Vector<Peak>();
            peaks.add(result_cur);
            for (int j = 1; j < fsize - 1; ++j) {
                if ((cur[j - 1] <= cur[j]) && (cur[j + 1] <= cur[j])) {
                    declineNew = 1;
                }
                if ((cur[j - 1] >= cur[j]) && (cur[j + 1] >= cur[j])) {
                    declineNew = 0;
                }
                if (decline != declineNew) {
                    if (declineNew == 1) {
                        c_freq = freqStep * j;
                        c_pow = cur[j];
                    } else {
                        double noise = (l_pow + cur[j]) * 0.5;
                        result_cur.add(new Peak(c_pow, noise, c_freq, freqStep * j - l_freq));
                        l_freq = freqStep * j;
                        l_pow = cur[j];
                    }
                    decline = declineNew;
                }
            }
        }
    }

    public Vector<Vector<Peak>> getPeaks(){
        return peaks;
    }

}
