package conversions.notes;

public enum Note {
    C, CSharp, D, DSharp, E, F, FSharp, G, GSharp, A, ASharp, B;

    public int midiCode(int octave) {
        //  MIDI code 0 is for note C-1
        return 12 * (octave + 1) + this.ordinal();
    }
}
