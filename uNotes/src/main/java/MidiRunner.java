import conversions.notes.NoteSequence;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Test for NoteSequence.getMidiSequence()
 *
 * @see conversions.notes.NoteSequence
 */
public class MidiRunner {
    public static void main(String[] args) throws FileNotFoundException {
        File inputDir = new File("test", "music");
        File outputDir = new File("test", "output");
        outputDir.mkdir();

        String inputFileName = "a.mid";
        File in = new File(inputDir, inputFileName);

        System.out.println("uNotes");

        try {
            NoteSequence noteSequence = new NoteSequence(in);
            Sequence midiSequence = noteSequence.getMidiSequence();

            File outMidi = new File(outputDir, inputFileName);
            MidiSystem.write(midiSequence, 0, outMidi);

            ArrayList<double[]> notePower = noteSequence.getNotesSeries(8000);
            PrintStream outPower = new PrintStream(new File(outputDir, inputFileName + ".npw.dat"));

            for (int i = 0; i < notePower.size(); ++i) {
                outPower.print(midiSequence.getMicrosecondLength() * 1e-6 * i / notePower.size() + " ");
                for (int j = 0; j < notePower.get(i).length; j++) {
                    outPower.print(notePower.get(i)[j] + " ");
                }
                outPower.println();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
