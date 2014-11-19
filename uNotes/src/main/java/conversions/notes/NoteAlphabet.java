package conversions.notes;

import java.util.ArrayList;

public final class NoteAlphabet { //TODO use MIDI codes
    private ArrayList<Double> myFrequencies;    //TODO double[] ?
    private int myMinMidiCode;
    private int myMaxMidiCode;

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
        if (minMidiCode > maxMidiCode) {
            throw new IllegalArgumentException("minMidiCode is greater than maxMidiCode");
        }

        myMinMidiCode = minMidiCode;
        myMaxMidiCode = maxMidiCode;

        myFrequencies = new ArrayList<Double>(maxMidiCode - minMidiCode + 1);
        for (int i = minMidiCode; i <= maxMidiCode; i++) {
            myFrequencies.add(MidiHelper.getMidiFrequency(i));
        }
    }


    // TODO: delete getFrequencies() or getAllFrequencies()
    public ArrayList<Double> getFrequencies() {
        return new ArrayList<Double>(myFrequencies);
    }

    public double[] getAllFrequencies() {
        double[] allFrequencies = new double[myFrequencies.size()];
        for (int i = 0; i < myFrequencies.size(); ++i) {
            allFrequencies[i] = myFrequencies.get(i);
        }
        return allFrequencies;
    }

    public int getMinMidiCode() {
        return myMinMidiCode;
    }

    public int getMaxMidiCode() {
        return myMaxMidiCode;
    }
}
