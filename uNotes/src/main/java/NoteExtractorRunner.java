import conversions.NotePowerGenerator;
import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

public class NoteExtractorRunner {
    public static void main(String[] args) {
        int timeStepLength = 256;
        int windowLength = 4096;

        File inputDir = new File("test", "music");
        File outputDir = new File("test", "output");
        outputDir.mkdir();

        String inputFileName = "a.wav";
        File in = new File(inputDir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
            Spectrum spectrum = stft.transform(series);

            ArrayList<double[]> power = spectrum.getPowerSpectrum();

            double t0 = spectrum.getTimeZeroPoint();
            double nu0 = spectrum.getFrequencyZeroPoint();

            double dt = spectrum.getTimeStep();
            double dnu = spectrum.getFrequencyStep();
            PrintStream out = new PrintStream(new File(outputDir, inputFileName + ".power.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.get(i).length; j++) {
                    out.println((i * dt + t0) + "   " + (j * dnu + nu0) + "  " + power.get(i)[j]);
                }
            }

            PrintStream outNotes = new PrintStream(new File(outputDir, inputFileName + ".npw.dat"));
            // TODO: what is pke?
//            PeakCrossExtractor pke = new PeakCrossExtractor(dt, dnu, 10);
//            pke.loadSpectrum(power);
//            pke.extract(63);
//            ArrayList<ArrayList<Peak>> timePeaks = pke.getPeaks();


            //  Search notes from C0 to B6
            NotePowerGenerator notePowerGenerator = new NotePowerGenerator(spectrum, 2 * 12, 9 * 12 - 1);
            ArrayList<double[]> notePower = notePowerGenerator.getNotePowerSeries();
            for (int i = 0; i < notePower.size(); ++i) {
                outNotes.print(i * dt + " ");
                for (int j = 0; j < notePower.get(i).length; j++) {
                    outNotes.print(notePower.get(i)[j] + " ");
                }
                outNotes.println();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
