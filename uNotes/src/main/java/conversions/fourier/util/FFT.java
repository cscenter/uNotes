package conversions.fourier.util;

public final class FFT {
    private int n;
    private int m;

    private double[] cos;
    private double[] sin;

    public FFT(int n) {
        // Make sure n is a power of 2
        if (n <= 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Array length for FFT must be power of 2");
        }

        this.n = n;
        //m = log2(n)
        this.m = 31 - Integer.numberOfLeadingZeros(n);

        // precompute tables
        cos = new double[n / 2];
        sin = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            cos[i] = Math.cos(-2 * Math.PI * i / n);
            sin[i] = Math.sin(-2 * Math.PI * i / n);
        }
    }

    /**
     * ************************************************************
     * transform.c
     * Douglas L. Jones
     * University of Illinois at Urbana-Champaign
     * January 19, 1992
     * http://cnx.rice.edu/content/m12016/latest/
     * <p/>
     * transform: in-place radix-2 DIT DFT of a complex input
     * <p/>
     * input:
     * n: length of FFT: must be a power of two
     * m: n = 2**m
     * input/output
     * 00101   * x: double array of length n with real part of data
     * 00102   * y: double array of length n with imag part of data
     * 00103   *
     * 00104   *   Permission to copy and use this program is granted
     * 00105   *   as long as this header is included.
     * 00106   ***************************************************************
     */
    public void transform(double[] x, double[] y) {
        int i, j, k, n1, n2, a;
        double c, s, t1, t2;
        // Bit-reverse
        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j = j - n1;
                n1 = n1 / 2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }
        // FFT
        //n1 = 0;
        n2 = 1;
        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 *= 2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < n; k = k + n2) {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }
}
