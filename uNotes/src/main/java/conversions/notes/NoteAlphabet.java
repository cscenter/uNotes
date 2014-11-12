package conversions.notes;

import java.util.ArrayList;

public class NoteAlphabet { //TODO use MIDI codes
    private ArrayList<double[]> frequencies;
    private ArrayList<Double> frequenciesPlain;


    public NoteAlphabet(int lastOctave) {
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

    public double[] getAllFrequencies() {
        double[] allFrequencies = new double[frequenciesPlain.size()];
        for (int i = 0; i < frequenciesPlain.size(); ++i) {
            allFrequencies[i] = frequenciesPlain.get(i);
        }
        return allFrequencies;
    }

    /**
     * @param frequency frequency of note
     * @return MIDI code of nearest note
     */
    public static int getMIDICode(double frequency) {   //TODO move in separate class
        double pitchA4 = 440;
        Double code = 69 + 12 * Math.log(frequency / pitchA4) / Math.log(2);
        return (int) Math.round(code);
    }

    /**
     * @param midiCode MIDI code
     * @return frequency of note with given MIDI code
     */
    public static double getMIDIFrequency(int midiCode) {   //TODO move in separate class
        double pitchA4 = 440;
        return pitchA4 * Math.pow(2, (midiCode - 69) / 12);
    }
}
