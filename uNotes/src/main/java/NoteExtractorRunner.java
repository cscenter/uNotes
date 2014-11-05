import conversions.*;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.Vector;

public class NoteExtractorRunner {
    public static void main(String[] args) {
        int timeStepLength = 256;
        int windowLength = 4096;

        File dir = new File("test", "music");
        String inputFileName = "gvp2.wav";
        File in = new File(dir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
            Spectrum result = stft.transform(series);

            Vector<double[]> power = result.getPowerSpectrum();

            double t0 = result.getTimeZeroPoint();
            double nu0 = result.getFrequencyZeroPoint();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File(inputFileName + ".power.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.elementAt(i).length; j++) {
                    out.println((i * dt + t0) + "   " + (j * dnu + nu0) + "  " + power.elementAt(i)[j]);
                }
            }

            WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(result);

            Spectrum subSpectrum = noteGetter.spectrumTransform(result);

            Vector<double[]> wPower = subSpectrum.getPowerSpectrum();

            double t0W = subSpectrum.getTimeZeroPoint();
            double nu0W = subSpectrum.getFrequencyZeroPoint();

            double dtW = subSpectrum.getTimeStep();
            double dnuW = subSpectrum.getFrequencyStep();

            PrintStream outNotes = new PrintStream(new File(inputFileName + ".npw.dat"));
            PeakCrossExtractor pke = new PeakCrossExtractor(dt, dnu, 10);

            pke.loadRaws(power);

            pke.extract(63);

            Vector<Vector<Peak>> timePeaks  = pke.getPeaks();

            noteAlphabet sevenOctaves = new noteAlphabet(7);
            Vector<Double> notes = sevenOctaves.getFrequenciesPlain();

            PeakExtractor pex = new PeakExtractor(dt, dnu);
            pex.loadRaws(power);
            pex.extract();
            Vector<Vector<Peak>> peaks = pex.getPeaks();

            Vector<double[]> notePower = new Vector<double[]>();

            for (int i = 0; i < peaks.size(); ++i) {
                double[] notePowerSlice = new double[notes.size()];
                for (int j = 0; j < peaks.elementAt(i).size(); j++) {
                    Peak cur = peaks.elementAt(i).elementAt(j);
                    if (cur.powerRel > 10 & cur.power > 10 & wPower.elementAt(i)[Math.min((int)(cur.center / dnuW), wPower.size() - 1)] > 10){
                        double diff = 10000;
                        int noteIndex = 0;
                        for (int l = 0; l < notes.size(); ++l){
                            if (Math.abs(notes.elementAt(l) - cur.center) < diff){
                                noteIndex = l;
                                diff = Math.abs(notes.elementAt(l) - cur.center);
                            }
                        }
                        if (notePowerSlice[noteIndex] == 0 | notePowerSlice[noteIndex] < cur.power){
                            notePowerSlice[noteIndex] = cur.power;
                        }
                    }
                }
                notePower.add(notePowerSlice);
            }

            for (int i = 0; i < notePower.size(); ++i) {
                outNotes.print(i * dt + " ");
                for (int j = 0; j < notePower.elementAt(i).length; j++) {
                    outNotes.print(notePower.elementAt(i)[j] + " ");
                }
                outNotes.println();
            }

            for (int i = 0; i < notes.size(); ++i) {
                System.out.println((i + 1) + " " + notes.elementAt(i));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
