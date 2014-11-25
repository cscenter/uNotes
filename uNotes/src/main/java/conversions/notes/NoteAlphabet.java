package conversions.notes;

import java.util.Arrays;

public final class NoteAlphabet {
    private double[] myFrequencies;
    private int myMinMidiCode;
    private int myMaxMidiCode;

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

        int midiCodesCount = maxMidiCode - minMidiCode + 1;
        myFrequencies = new double[midiCodesCount];
        for (int i = 0; i < midiCodesCount; i++) {
            myFrequencies[i] = MidiHelper.getMidiFrequency(i + minMidiCode);
        }
    }


    public double[] getFrequencies() {
        return Arrays.copyOf(myFrequencies, myFrequencies.length);
    }

    public int getMinMidiCode() {
        return myMinMidiCode;
    }

    public int getMaxMidiCode() {
        return myMaxMidiCode;
    }
}
