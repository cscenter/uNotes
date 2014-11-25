package conversions.notes;

import com.sun.istack.internal.NotNull;
import conversions.QuasiNotes;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class NoteSequence {
    private Sequence mySequence;
    private Track myTrack;
    /**
     * MIDI command for tempo change
     */
    private final int TEMPO = 0x51;

    long myLength;  //Length, ticks
    double myDuration;  // Duration, seconds

    public NoteSequence(@NotNull File midiInputFile) throws InvalidMidiDataException, IOException {
        mySequence = MidiSystem.getSequence(midiInputFile);
        myLength = mySequence.getTickLength();
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
                myTrack.add(event);
                MidiMessage midiMessage = event.getMessage();
                if (midiMessage instanceof MetaMessage) {
                    MetaMessage message = (MetaMessage) midiMessage;
                    if (message.getType() == TEMPO) {
                        myTrack.add(new MidiEvent(new MetaMessage(TEMPO, message.getData(), message.getData().length), event.getTick()));
                    }
                }
                if (midiMessage instanceof ShortMessage) {
                    ShortMessage message = (ShortMessage) midiMessage;
                    int command = message.getCommand();
                    int key = message.getData1();
                    int velocity = message.getData2();

                    if (command == ShortMessage.NOTE_ON && velocity == 0) { //Equivalent to NOTE_OFF
                        myTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, key, 127), event.getTick()));
                    } else if (command == ShortMessage.NOTE_ON || command == ShortMessage.NOTE_OFF) {
                        myTrack.add(event);
                    }
                }
            }
            // delete this track from the sequence
            mySequence.deleteTrack(track);
        }
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
    public NoteSequence(@NotNull QuasiNotes quasiNotes, int tempoInBPM, int ticksPerQuarterNote) throws InvalidMidiDataException {
        int minMidiCode = quasiNotes.getMinMidiCode();
        double timeStep = quasiNotes.getTimeStep();
        ArrayList<double[]> noteSeries = quasiNotes.getNotePowerSeries();

        double outputTimeStep = 60.0 / tempoInBPM / ticksPerQuarterNote;

        mySequence = new Sequence(Sequence.PPQ, ticksPerQuarterNote);
        myTrack = mySequence.createTrack();
        //Set tempo
        int tempoInMPQ = (int) Math.round(60.0 / tempoInBPM * 1e6);  //  tempo in microseconds per quarter
        byte[] data = new byte[3];
        data[0] = (byte) ((tempoInMPQ >> 16) & 0xFF);
        data[1] = (byte) ((tempoInMPQ >> 8) & 0xFF);
        data[2] = (byte) (tempoInMPQ & 0xFF);
        MetaMessage tempoMessage = new MetaMessage(TEMPO, data, data.length);
        myTrack.add(new MidiEvent(tempoMessage, 0));

        HashSet<Integer> currentNotes = new HashSet<Integer>();
        for (int timePoint = 0; timePoint < noteSeries.size(); timePoint++) {
            double[] power = noteSeries.get(timePoint);
            int tick = (int) Math.round(timePoint * timeStep / outputTimeStep);
            for (int noteIndex = 0; noteIndex < power.length; noteIndex++) {
                int midiCode = noteIndex + minMidiCode;
                if (power[noteIndex] > 0 && !currentNotes.contains(midiCode)) {
                    myTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, midiCode, 127), tick));
                    currentNotes.add(midiCode);
                } else if (power[noteIndex] <= 0 && currentNotes.contains(midiCode)) {
                    myTrack.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, midiCode, 0), tick));
                    currentNotes.remove(midiCode);
                }
            }
        }
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
            int currIndex = (new Double(event.getTick() * step)).intValue();
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
}
