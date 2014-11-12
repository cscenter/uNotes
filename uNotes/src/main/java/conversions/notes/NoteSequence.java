package conversions.notes;

import com.sun.istack.internal.NotNull;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class NoteSequence {
    private Sequence mySequence;
    private Track myTrack;

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
                double[] notesArray = new double[128];
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
