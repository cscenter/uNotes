package conversions.notes;

import java.util.ArrayList;

public class NoteAlphabet { //TODO use MIDI codes
    private ArrayList<Double> myFrequencies;

    @Deprecated
    public NoteAlphabet(int lastOctave) {
        //notes from C0 to B(lastOctave)
        this(2 * 12, (lastOctave + 2) * 12 - 1);
    }

    /**
     * builds frequencies from minMidiCode to maxMidiCode
     *
     * @param minMidiCode MIDI code of the lowest note (C-1 note has code 0)
     * @param maxMidiCode MIDI code of the highest note (C-1 note has code 0)
     */
    public NoteAlphabet(int minMidiCode, int maxMidiCode) {
        if (minMidiCode < 0) {
            throw new IllegalArgumentException("minMidiCode must be >=0");
        }
        if (maxMidiCode > 127) {
            throw new IllegalArgumentException("maxMidiCode must be <=127");
        }
        if (minMidiCode > maxMidiCode) {
            throw new IllegalArgumentException("minMidiCode is greater than maxMidiCode");
        }

        myFrequencies = new ArrayList<Double>(maxMidiCode - minMidiCode + 1);
        for (int i = minMidiCode; i <= maxMidiCode; i++) {
            myFrequencies.add(getMIDIFrequency(i));
        }
    }


    public ArrayList<Double> getFrequencies() {
        return myFrequencies;
    }

    public double[] getAllFrequencies() {
        double[] allFrequencies = new double[myFrequencies.size()];
        for (int i = 0; i < myFrequencies.size(); ++i) {
            allFrequencies[i] = myFrequencies.get(i);
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
        return pitchA4 * Math.pow(2, (midiCode - 69) / 12.0);
    }
}
