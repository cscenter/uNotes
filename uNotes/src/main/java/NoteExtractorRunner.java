import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.WaveletSpectrumTransform;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.notes.noteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakCrossExtractor;
import conversions.peaks.PeakExtractor;

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
        String inputFileName = "gvp2.wav";
        File in = new File(inputDir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
            Spectrum result = stft.transform(series);

            ArrayList<double[]> power = result.getPowerSpectrum();

            double t0 = result.getTimeZeroPoint();
            double nu0 = result.getFrequencyZeroPoint();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File(outputDir, inputFileName + ".power.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.get(i).length; j++) {
                    out.println((i * dt + t0) + "   " + (j * dnu + nu0) + "  " + power.get(i)[j]);
                }
            }

            PrintStream outNotes = new PrintStream(new File(outputDir, inputFileName + ".npw.dat"));
            ///////TODO: what is pke?
            PeakCrossExtractor pke = new PeakCrossExtractor(dt, dnu, 10);

            pke.loadSpectrum(power);

            pke.extract(63);

            ArrayList<ArrayList<Peak>> timePeaks = pke.getPeaks();
            //////
            noteAlphabet sevenOctaves = new noteAlphabet(7);
            ArrayList<Double> notes = sevenOctaves.getFrequenciesPlain();
            ////// In the first place we must to alignment the fourier spectrum
            result.alignment(20);
            ////// Then we can make secondary wavelet spectrum in notes frequency, corresponding to sevenOcatves
            WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(result, sevenOctaves.getAllFrequecies());
            ArrayList<double[]> wPower = noteGetter.spectrumTransformWithCounts(result);
            //////
            PeakExtractor pex = new PeakExtractor(dt, dnu);
            pex.loadSpectrum(power);
            pex.extract();
            ArrayList<ArrayList<Peak>> peaks = pex.getPeaks();

            ArrayList<double[]> notePower = new ArrayList<double[]>();

            for (int i = 0; i < peaks.size(); ++i) {
                double[] notePowerSlice = new double[notes.size()];
                for (int j = 0; j < peaks.get(i).size(); j++) {
                    Peak cur = peaks.get(i).get(j);
                    //if (cur.powerRel > 10 & cur.power > 10 & wPower.get(i)[Math.min((int)(cur.center / dnuW), wPower.size() - 1)] > 10){
                    if (cur.powerRel > 10 & cur.power > 10) {
                        double diff = 10000;
                        int noteIndex = 0;
                        for (int l = 0; l < notes.size(); ++l) {
                            if (Math.abs(notes.get(l) - cur.center) < diff) {
                                noteIndex = l;
                                diff = Math.abs(notes.get(l) - cur.center);
                            }
                        }

                        if (wPower.get(i)[noteIndex] < 10) {
                            continue;
                        }

                        if (notePowerSlice[noteIndex] == 0 || notePowerSlice[noteIndex] < cur.power) {
                            notePowerSlice[noteIndex] = cur.power;
                        }
                    }
                }
                notePower.add(notePowerSlice);
            }

            for (int i = 0; i < notePower.size(); ++i) {
                outNotes.print(i * dt + " ");
                for (int j = 0; j < notePower.get(i).length; j++) {
                    outNotes.print(notePower.get(i)[j] + " ");
                }
                outNotes.println();
            }

            for (int i = 0; i < notes.size(); ++i) {
                System.out.println((i + 1) + " " + notes.get(i));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
