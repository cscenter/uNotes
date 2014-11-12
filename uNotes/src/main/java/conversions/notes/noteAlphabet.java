package conversions.notes;

import java.util.ArrayList;

public class noteAlphabet {
    private ArrayList<double[]> frequencies;
    private ArrayList<Double> frequenciesPlain;


    public noteAlphabet(int lastOctave) {
        frequencies = new ArrayList<double[]>();
        frequenciesPlain = new ArrayList<Double>();
        double freqC7 = 2093.0;
        double freqThis = freqC7 / 64;
        double step = Math.pow(2, 1.0 / 12);
        for (int i = 0; i < lastOctave; ++i) {
            double[] octave = new double[12];
            for (int j = 0; j < 12; ++j) {
                octave[j] = freqThis;
                frequenciesPlain.add(freqThis);
                freqThis *= step;
            }
            frequencies.add(octave);
        }
    }

    public ArrayList<double[]> getFrequencies() {
        return frequencies;
    }

    public ArrayList<Double> getFrequenciesPlain() {
        return frequenciesPlain;
    }

    public double[] getAllFrequecies() {
        double[] allFrequencies = new double[frequenciesPlain.size()];
        for (int i = 0; i < frequenciesPlain.size(); ++i) {
            allFrequencies[i] = frequenciesPlain.get(i);
        }
        return allFrequencies;
    }
}

