package conversions.notes;

import com.sun.istack.internal.NotNull;
import conversions.QuasiNotes;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class NoteSequence {
    private int myTempoInBPM;
    private Sequence mySequence;
    private Track myTrack;
    /**
     * MIDI command for tempo change
     */
    private static final int TEMPO = 0x51;
    /**
     * MIDI max. velocity for NOTE_ON and NOTE_OFF events
     */
    private static final int MAX_VELOCITY = 127;

    private long myTicksLength;  //Length, ticks
    double myDuration;  // Duration, seconds

    /**
     * Builds NoteSequence from MIDI file. Move all notes in single track
     * Catches only NOTE_ON and NOTE_OFF events.
     * Also catches one TEMPO event at the first MIDI tick.
     * NB: Does not support tempo changes.
     *
     * @param midiInputFile input File
     * @throws InvalidMidiDataException if:
     *                                  1) cannot get length of file,
     *                                  2) MIDI Sequence type is not PPQ (=> cannot calculate tempo in BPM),
     *                                  3) Tempo changes during playback
     * @throws IOException              if cannot open file
     */
    public NoteSequence(@NotNull File midiInputFile) throws InvalidMidiDataException, IOException {
        mySequence = MidiSystem.getSequence(midiInputFile);
        myTicksLength = mySequence.getTickLength();
        if (mySequence.getMicrosecondLength() == MidiFileFormat.UNKNOWN_LENGTH) {
            throw new InvalidMidiDataException("Unknown length");
        }
        myDuration = mySequence.getMicrosecondLength() / 1.0e6;
        Track[] tracks = mySequence.getTracks();

        // Move all notes in single track
        // Catch only NOTE_ON and NOTE_OFF events
        //myTrack = tracks[0];
        myTrack = mySequence.createTrack();
        for (Track track : tracks) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                long tick = event.getTick();
                myTrack.add(event);
                MidiMessage midiMessage = event.getMessage();
                if (midiMessage instanceof MetaMessage) {
                    MetaMessage message = (MetaMessage) midiMessage;
                    if (message.getType() == TEMPO) {
                        if (tick > 0) {
                            throw new InvalidMidiDataException(
                                    "Tempo has been changed in the middle of MIDI file");
                        }

                        // Calculate BPM
                        myTempoInBPM = calculateBPM(message);
                        myTrack.add(new MidiEvent(message, tick));
                    }
                } else if (midiMessage instanceof ShortMessage) {
                    ShortMessage message = (ShortMessage) midiMessage;
                    int command = message.getCommand();
                    int key = message.getData1();
                    int velocity = message.getData2();

                    if (command == ShortMessage.NOTE_ON && velocity == 0) { //Equivalent to NOTE_OFF
                        myTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, key, MAX_VELOCITY), tick));
                    } else if (command == ShortMessage.NOTE_ON || command == ShortMessage.NOTE_OFF) {
                        myTrack.add(event);
                    }
                }
            }
            // delete this track from the sequence
            mySequence.deleteTrack(track);
        }
    }

    private int calculateBPM(MetaMessage message) throws InvalidMidiDataException {
        if (message.getType() != TEMPO) {
            throw new InvalidMidiDataException("TEMPO message expected");
        }
        if (mySequence.getDivisionType() != Sequence.PPQ) {
            throw new InvalidMidiDataException(
                    "Cannot get tempoInBPM, input MIDI Sequence must have divisionType = Sequence.PPQ");
        }
        byte[] data = message.getData();
        long tempoInMPQ = (((long) data[0] & 0xff) << 16) |
                (((long) data[1] & 0xff) << 8) |
                (((long) data[2] & 0xff));

        return (int) Math.round(60.0 * 1e6 / tempoInMPQ);
    }

    /**
     * Build note sequence, assuming that one timeStep  = one MIDI tick = one quarter note.
     * Use another constructor if you want to specify tempo(BPM) and time resolution (e.g. up to 16th notes)
     *
     * @param quasiNotes QuasiNotes sequence
     * @throws InvalidMidiDataException
     */
    public NoteSequence(@NotNull QuasiNotes quasiNotes) throws InvalidMidiDataException {
        this(quasiNotes, (int) (60.0 / quasiNotes.getTimeStep()), 1);   // Let's say that one tick = one quarter note...
    }

    /**
     * @param quasiNotes          QuasiNotes sequence
     * @param tempoInBPM          tempo of melody in beats per minute.
     *                            Duration of quarter note is (60 / tempoInBPM ) seconds
     * @param ticksPerQuarterNote number of ticks per quarter note.
     *                            This parameter specifies duration of shortest note we want to use,
     *                            e.g ticksPerQuarterNote = 4 for 16th notes, ticksPerQuarterNote = 8 for 32nd notes.
     * @throws InvalidMidiDataException
     */
    // TODO add note power threshold (=0 by default)
    // TODO ignore short notes?
    public NoteSequence(@NotNull QuasiNotes quasiNotes, int tempoInBPM, int ticksPerQuarterNote) throws InvalidMidiDataException {
        myTempoInBPM = tempoInBPM;

        int minMidiCode = quasiNotes.getMinMidiCode();
        double timeStep = quasiNotes.getTimeStep();
        ArrayList<double[]> noteSeries = quasiNotes.getNotePowerSeries();

        // time step in seconds = length of shortest note
        double outputTimeStep = 60.0 / tempoInBPM / ticksPerQuarterNote;

        mySequence = new Sequence(Sequence.PPQ, ticksPerQuarterNote);
        myTrack = mySequence.createTrack();
        MetaMessage tempoMessage = buildTempoMessage(tempoInBPM);

        myTrack.add(new MidiEvent(tempoMessage, 0));

        HashSet<Integer> currentNotes = new HashSet<Integer>();
        for (int timePoint = 0; timePoint < noteSeries.size(); timePoint++) {
            double[] power = noteSeries.get(timePoint);
            int tick = (int) Math.round(timePoint * timeStep / outputTimeStep);
            for (int noteIndex = 0; noteIndex < power.length; noteIndex++) {
                int midiCode = noteIndex + minMidiCode;
                if (power[noteIndex] > 0 && !currentNotes.contains(midiCode)) {
                    myTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, midiCode, MAX_VELOCITY), tick));
                    currentNotes.add(midiCode);
                } else if (power[noteIndex] <= 0 && currentNotes.contains(midiCode)) {
                    myTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, midiCode, 0), tick));
                    currentNotes.remove(midiCode);
                }
            }
        }
        myTicksLength = (int) Math.round((noteSeries.size() - 1) * timeStep / outputTimeStep);
    }

    private static MetaMessage buildTempoMessage(int tempoInBPM) throws InvalidMidiDataException {
        //Set tempo
        int tempoInMPQ = tempoMPQToBPM(tempoInBPM);
        byte[] data = new byte[3];
        data[0] = (byte) ((tempoInMPQ >> 16) & 0xFF);
        data[1] = (byte) ((tempoInMPQ >> 8) & 0xFF);
        data[2] = (byte) (tempoInMPQ & 0xFF);
        return new MetaMessage(TEMPO, data, data.length);
    }

    private static int tempoMPQToBPM(int tempoInBPM) {
        return (int) Math.round(60.0 / tempoInBPM * 1e6);
    }

    public Sequence getMidiSequence() {
        return mySequence;
    }

    public ArrayList<double[]> getNotesSeries(int outputArrayLength) {
        ArrayList<double[]> result = new ArrayList<double[]>();

        double step = 1.0 * outputArrayLength / myTrack.ticks();
        HashSet<Integer> currentNotes = new HashSet<Integer>();


        int prevIndex = 0;
        for (int i = 0; i < myTrack.size(); i++) {
            MidiEvent event = myTrack.get(i);
            int currIndex = new Double(event.getTick() * step).intValue();
            for (int j = prevIndex; j < currIndex; j++) {
                double[] notesArray = new double[MidiHelper.MIDI_CODES_COUNT];
                for (Integer note : currentNotes) {
                    notesArray[note] = 1;    //Mark note as playing
                }
                result.add(notesArray);
            }
            prevIndex = currIndex;

            if (event.getMessage() instanceof ShortMessage) {
                ShortMessage message = (ShortMessage) event.getMessage();
                if (message.getCommand() == ShortMessage.NOTE_ON) {
                    currentNotes.add(message.getData1());
                } else if (message.getCommand() == ShortMessage.NOTE_OFF) {
                    currentNotes.remove(message.getData1());
                }
            }
        }

        return result;
    }

    /**
     * @return tempo in BPM.
     */
    public int getTempoInBPM() {
        return myTempoInBPM;
    }

    /**
     * @return length of sequence in MIDI ticks
     */
    public long getTicksLength() {
        return myTicksLength;
    }
}
