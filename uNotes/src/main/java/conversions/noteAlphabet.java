package conversions;

import java.util.Vector;

public class noteAlphabet {
    private Vector<double[]> frequencies;
    private Vector<Double> frequenciesPlain;


    public noteAlphabet(int lastOctave){
        frequencies = new Vector<double[]>();
        frequenciesPlain = new Vector<Double>();
        double freqC7 = 2093.0;
        double freqThis = freqC7 / 64;
        double step = Math.pow(2, 1.0 / 12);
        for (int i = 0; i < lastOctave; ++i){
            double[] octave = new double[12];
            for (int j = 0; j < 12; ++j){
                octave[j] = freqThis;
                frequenciesPlain.add(freqThis);
                freqThis *= step;
            }
            frequencies.add(octave);
        }
    }

    public Vector<double[]> getFrequencies(){
        return frequencies;
    }
    public Vector<Double> getFrequenciesPlain(){
        return frequenciesPlain;
    }
}

