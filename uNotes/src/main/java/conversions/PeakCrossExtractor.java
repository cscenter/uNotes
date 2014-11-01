package conversions;

import java.util.Vector;

public class PeakCrossExtractor {

    private Vector<double[]> spectrum; //todo rename
    private Vector<Vector<Peak>> peaks;
    private double timeStep;
    private double freqStep;
    private double sensitivity;

    public PeakCrossExtractor(double timeStep, double freqStep, double sensitivity){
        this.timeStep = timeStep;
        this.freqStep = freqStep;
        this.sensitivity = sensitivity;
        peaks = new Vector<Vector<Peak>>();
    }

    public void loadRaws(Vector<double[]> raws){
        this.spectrum = raws;
    }

    public void extract(int slice){
        int fsize = spectrum.size(); //todo check NPE
        double[] series = new double[fsize];
        int decline = 0;
        int declineNew = 0; // todo boolean
        double c_freq = 0;
        double l_freq = 0;
        double c_pow = 0;
        double l_pow = 0;
        double d_pow = -1000;
        double u_pow = 1000;

        for (int i = 0; i < fsize; i++) {
            series[i] = spectrum.elementAt(i)[slice];
        }


        l_freq = 0;
        l_pow = series[0];
        decline = 0;
        Vector<Peak> result_cur = new Vector<Peak>();
        peaks.add(result_cur);
        for (int j = 1; j < fsize - 1; ++j) {
            if ((series[j - 1] <= series[j]) && (series[j + 1] <= series[j])) {
                if (decline == 1){
                    c_freq = timeStep * j;
                    c_pow = series[j];
                }
                if (series[j] - d_pow > sensitivity) {
                    declineNew = 1;
                    u_pow = series[j];
                }
            }
            if ((series[j - 1] >= series[j]) && (series[j + 1] >= series[j])) {
                if (u_pow - series[j] > sensitivity) {
                    declineNew = 0;
                    d_pow = series[j];
                }
            }
            if (decline != declineNew) {
                if (declineNew == 1) {
                    c_freq = timeStep * j;
                    c_pow = series[j];
                } else {
                    double noise = (l_pow + series[j]) * 0.5;
                    result_cur.add(new Peak(c_pow, noise, c_freq, timeStep * j - l_freq));
                    l_freq = timeStep * j;
                    l_pow = series[j];
                }
                decline = declineNew;
            }
        }

    }

    public Vector<Vector<Peak>> getPeaks(){
        return peaks;
    }

}
