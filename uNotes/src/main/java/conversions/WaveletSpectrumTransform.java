package conversions;

import com.sun.istack.internal.NotNull;
import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;

/**
 * Created by User on 22.10.2014.
 */
public class WaveletSpectrumTransform extends SpectrumTransform {

    public static final double ALPHA = Math.sqrt(2.0);
    private static final double B = 2.0;

    private int myWindowLength;

    private ArrayList<double[]> waveletsArguments;
    private double[] normalizingFactor;
    private ArrayList<Complex[]> wavelet;


    public WaveletSpectrumTransform(@NotNull Spectrum input){
        this.input = input;

        myWindowLength = input.getPowerSpectrum().elementAt(0).length; //TODO

        int scaleLength = myWindowLength - 2;

        waveletsArguments = new ArrayList<double[]>();
        int timeShift = myWindowLength / 2;

        for (int i = 0; i < scaleLength; ++i) {
            double[] arguments = new double[myWindowLength];
            for (int k = 0; k < myWindowLength; ++k) {
                arguments[k] = (k - timeShift) * ALPHA / (i + 2);
                //System.out.println(i + "  " + k + "  " +  arguments[k]);
            }
            waveletsArguments.add(arguments);
        }

        normalizingFactor = new double[scaleLength];

        for (int i = 0; i < normalizingFactor.length; ++i) {
            double[] arguments = waveletsArguments.get(i);
            for (int k = 0; k < myWindowLength; ++k) {
                //System.out.println(i + "  " + k + "  " +  arguments[k]);
                normalizingFactor[i] += Math.exp(-1.0 * Math.pow(arguments[k], 2.0) / B);
            }
            //System.out.println(i + "  " + normalizingFactor[i]);
        }

        wavelet = new ArrayList<Complex[]>();
        Complex[] section;

        for (int i = 0; i < scaleLength; ++i) {
            double[] arguments = waveletsArguments.get(i);
            section = new Complex[myWindowLength];
            for (int k = 0; k < myWindowLength; ++k) {
                double arg = 2.0 * Math.PI * arguments[k];
                double rate = Math.exp(-1.0 * Math.pow(arguments[k] / B, 2.0));
                section[k] = new Complex(rate * Math.cos(arg), -rate * Math.sin(arg));
            }
            wavelet.add(section);
        }
    }

    @Override
    public double[] transform(int slice) {

        double[] wav = input.getPowerSpectrum().elementAt(slice).clone();//TODO arraycopy

        int scaleLength = myWindowLength - 2;
        double[] scalogramsSection = new double[scaleLength];

        Complex waveletAmplitude;
        Complex[] section;

        for (int i = 0; i < scaleLength; ++i) {
            section = wavelet.get(i);
            waveletAmplitude = new Complex(0.0, 0.0);
            for (int k = 0; k < myWindowLength; ++k) {
                Complex arg = new Complex(section[k].getReal(), section[k].getImaginary());
                arg = arg.multiply(wav[k]);
                //System.out.println(i + "  " + k + "  " + arg);
                waveletAmplitude = waveletAmplitude.add(arg);
                //System.out.println(i + "  " + k + "  " + waveletAmplitude);
            }
            waveletAmplitude = waveletAmplitude.divide(normalizingFactor[i]);
            //System.out.println(currFrame + "  " + i + "  " +  waveletAmplitude);

            scalogramsSection[i] = Math.pow(waveletAmplitude.abs(), 2.0);
            //System.out.println(currFrame + "  " + i + "  " +  scalogramsSection[i]);
        }

        return scalogramsSection;
    }
}
