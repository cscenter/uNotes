package conversions.notes;

public final class MidiHelper {

    /**
     * A4 note frequency, Hz
     */
    private static final double PITCH_A4 = 440;
    public static final int MIN_MIDI_CODE = 0;
    public static final int MAX_MIDI_CODE = 127;
    public static final int MIDI_CODES_COUNT = MAX_MIDI_CODE - MIN_MIDI_CODE + 1;

    /**
     * @param frequency frequency of note
     * @return MIDI code of nearest note
     */
    public static int getMidiCode(double frequency) {
        Double code = 69 + 12 * Math.log(frequency / PITCH_A4) / Math.log(2);
        int result = (int) Math.round(code);
        //  TODO
//        if (result < MIN_MIDI_CODE || result > MAX_MIDI_CODE) {
//            throw new IllegalArgumentException("frequency out of range");
//        }
        return result;
    }

    /**
     * @param midiCode MIDI code
     * @return frequency of note with given MIDI code
     */
    public static double getMidiFrequency(int midiCode) {
        //TODO
//        if (midiCode < MIN_MIDI_CODE || midiCode > MAX_MIDI_CODE) {
//            throw new IllegalArgumentException("MIDI code out of range");
//        }
        return PITCH_A4 * Math.pow(2, (midiCode - 69) / 12.0);
    }
}
