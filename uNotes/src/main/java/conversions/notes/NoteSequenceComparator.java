package conversions.notes;

import java.util.ArrayList;

public class NoteSequenceComparator {
    /**
     * This method calculates distance between two note sequences (proportion of different notes).
     *
     * At every time step we look at every note
     * @param x first note sequence
     * @param y second first note sequence
     * @param timeSeriesLength number of points
     * @param minMidiCode lowest note
     * @param maxMidiCode highest note
     * @return normalized distance between 2 sequences.
     */
    public static double distance(NoteSequence x, NoteSequence y, int timeSeriesLength, int minMidiCode, int maxMidiCode) {
        double differences = 0;

        ArrayList<double[]> xNotes = x.getNotesSeries(timeSeriesLength);
        ArrayList<double[]> yNotes = y.getNotesSeries(timeSeriesLength);

        for (int i = 0; i < xNotes.size(); i++) {
            for (int j = minMidiCode; j < maxMidiCode; j++) {
                if (xNotes.get(i)[j] != yNotes.get(i)[j]) {
                    differences++;
                }
            }
        }
        return differences / timeSeriesLength / (maxMidiCode - minMidiCode + 1);
    }

    public static double distance(NoteSequence x, NoteSequence y, int timeSeriesLength) {
        return distance(x, y, timeSeriesLength, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE);
    }
}
