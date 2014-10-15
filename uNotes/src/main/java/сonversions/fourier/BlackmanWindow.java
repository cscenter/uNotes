package сonversions.fourier;

/**
 * Make a blackman window:
 * w(n) = 0.42 - 0.5cos{(2 * PI * n) / (N - 1)} + 0.08cos{(4 * PI * n) / (N - 1)};
 */
public class BlackmanWindow implements TimeWindow {
    @Override
    public double[] makeWindow(int size) {
        double[] newWindow = new double[size];
        for (int i = 0; i < size; ++i) {
            newWindow[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (size - 1))
                    + 0.08 * Math.cos(4 * Math.PI * i / (size - 1));
        }

        return newWindow;
    }
}
