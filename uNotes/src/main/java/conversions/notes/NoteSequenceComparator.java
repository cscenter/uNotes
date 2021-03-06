package conversions.notes;

import java.util.ArrayList;

/**
 * This class provides static methods to compare note sequences.
 */
public class NoteSequenceComparator {
    /**
     * This method calculates error of first note sequence relative to second note sequence.
     * <p/>
     * At every time step we look at every note and check whether it is played/muted in both sequences.
     *
     * @param sequence             given sequence
     * @param expectedSequence     true sequence
     * @param timeSeriesLength     number of points
     * @param falsePositivePenalty penalty for false positives (if the wrong note is played in sequence)
     * @param falseNegativePenalty penalty for false negatives (if the expected note is not played in sequence)
     * @param minMidiCode          lowest note
     * @param maxMidiCode          highest note
     * @return normalized error of sequence relative to expected sequence.
     */
    public static double calculateError(NoteSequence sequence, NoteSequence expectedSequence, int timeSeriesLength,
                                        double falsePositivePenalty, double falseNegativePenalty,
                                        int minMidiCode, int maxMidiCode) {
        double differences = 0;

        ArrayList<double[]> xNotes = sequence.getNotesSeries(timeSeriesLength);
        ArrayList<double[]> yNotes = expectedSequence.getNotesSeries(timeSeriesLength);

        for (int i = 0; i < xNotes.size(); i++) {
            for (int j = minMidiCode; j <= maxMidiCode; j++) {
                if (xNotes.get(i)[j] != 0.0 && yNotes.get(i)[j] == 0.0) {
                    differences += falsePositivePenalty;
                } else if (xNotes.get(i)[j] == 0.0 && yNotes.get(i)[j] != 0.0) {
                    differences += falseNegativePenalty;
                }
            }
        }
        return differences / timeSeriesLength / (maxMidiCode - minMidiCode + 1);
    }

    /**
     * This method calculates error of first note sequence relative to second note sequence.
     * <p/>
     * At every time step we look at every note and check whether it is played/muted in both sequences.
     *
     * @param sequence             given sequence
     * @param expectedSequence     true sequence
     * @param timeSeriesLength     number of points
     * @param falsePositivePenalty penalty for false positives (if the wrong note is played in sequence). = 1.0 by default.
     * @param falseNegativePenalty penalty for false negatives (if the expected note is not played in sequence) = 1.0 by default.
     * @return normalized error of sequence relative to expected sequence.
     */
    public static double calculateError(NoteSequence sequence, NoteSequence expectedSequence, int timeSeriesLength,
                                        double falsePositivePenalty, double falseNegativePenalty) {
        return calculateError(sequence, expectedSequence, timeSeriesLength,
                falsePositivePenalty, falseNegativePenalty,
                MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE
        );
    }

    /**
     * This method calculates error of first note sequence relative to second note sequence.
     * <p/>
     * At every time step we look at every note and check whether it is played/muted in both sequences.
     *
     * @param sequence         given sequence
     * @param expectedSequence true sequence
     * @param timeSeriesLength number of points
     * @return normalized error of sequence relative to expected sequence.
     */
    public static double calculateError(NoteSequence sequence, NoteSequence expectedSequence, int timeSeriesLength) {
        return calculateError(sequence, expectedSequence, timeSeriesLength,
                1.0, 1.0, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE);
    }

    /**
     * This method calculates distance between two note sequences (proportion of different notes).
     * <p/>
     * At every time step we look at every note and check whether it is played/muted in both sequences.
     *
     * @param x                first note sequence
     * @param y                second first note sequence
     * @param timeSeriesLength number of points
     * @param minMidiCode      lowest note
     * @param maxMidiCode      highest note
     * @return normalized distance between 2 sequences.
     */
    public static double distance(NoteSequence x, NoteSequence y, int timeSeriesLength, int minMidiCode, int maxMidiCode) {
        return calculateError(x, y, timeSeriesLength, 1.0, 1.0, minMidiCode, maxMidiCode);
    }

    public static double distance(NoteSequence x, NoteSequence y, int timeSeriesLength) {
        return distance(x, y, timeSeriesLength, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE);
    }
}
