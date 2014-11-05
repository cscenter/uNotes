import conversions.*;
import conversions.peaks.*;
import conversions.notes.*;
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
        String inputFileName = "gvpm2.wav";
        File in = new File(dir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            System.out.println("Calculating power spectrum...");
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

            System.out.println("Calculating wavelet power spectrum...");

            Spectrum subSpectrum = noteGetter.spectrumTransform(result);
            Vector<double[]> wPower = subSpectrum.getPowerSpectrum();

            double t0W = subSpectrum.getTimeZeroPoint();
            double nu0W = subSpectrum.getFrequencyZeroPoint();

            double dtW = subSpectrum.getTimeStep();
            double dnuW = subSpectrum.getFrequencyStep();

            PrintStream outWavelet = new PrintStream(new File(inputFileName + ".pw.dat"));
            for (int i = 0; i < wPower.size(); i++) {
                for (int j = 0; j < wPower.elementAt(i).length; j++) {
                    outWavelet.println((t0W + i * dtW) + " " + (nu0W + j * dnuW) + " " + wPower.elementAt(i)[j]);
                }
            }


            System.out.println("Calculating notes...");
            PrintStream outNotes = new PrintStream(new File(inputFileName + ".npw.dat"));

            NoteAlphabet sevenOctaves = new NoteAlphabet(7);
            Vector<Double> notes = sevenOctaves.getFrequenciesPlain();

            PeakExtractor pex = new PeakExtractor(dt, dnu);
            pex.loadSpectrum(power);
            pex.extract();
            Vector<Vector<Peak>> peaks = pex.getPeaks();

            Vector<double[]> notePower = new Vector<double[]>();

            double powerLevel = 10;  //TODO rename
            double waveletPowerLevel = 8;  //TODO rename
            int timeSensitivity = 3;

            //TODO: move this algo to separate class
            for (int i = 0; i < peaks.size(); ++i) {
                double[] notePowerSlice = new double[notes.size()];

                final Vector<Peak> peakSlice = peaks.elementAt(i);
                for (int j = 0; j < peakSlice.size(); j++) {
                    Peak currentPeak = peakSlice.elementAt(j);
                    if (currentPeak.powerRel > powerLevel
                            && currentPeak.power > powerLevel
                            && wPower.elementAt(i)[Math.min((int) (currentPeak.center / dnuW), wPower.size() - 1)] > waveletPowerLevel ) {
                        //Find note by frequency
                        //TODO move elsewhere
                        double diff = 10000;
                        int noteIndex = 0;
                        for (int l = 0; l < notes.size(); ++l) {
                            if (Math.abs(notes.elementAt(l) - currentPeak.center) < diff) {
                                noteIndex = l;
                                diff = Math.abs(notes.elementAt(l) - currentPeak.center);
                            }
                        }
                        if (notePowerSlice[noteIndex] == 0 || notePowerSlice[noteIndex] < currentPeak.power) {
                            notePowerSlice[noteIndex] = currentPeak.power;
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


            PeakCrossExtractor pke = new PeakCrossExtractor(dt, powerLevel, timeSensitivity);

            pke.loadSpectrum(notePower);

            for (int i = 0; i < notePower.elementAt(0).length; i++) {
                pke.extract(i);
            }

            Vector<Vector<Peak>> timePeaks = pke.getPeaks();
            PrintStream outTNotes = new PrintStream(new File(inputFileName + ".nt.dat"));
            for (int i = 0; i < timePeaks.size(); i++) {
                outTNotes.println(notes.elementAt(i));
                for (int j = 0; j < timePeaks.elementAt(i).size(); j++) {
                    Peak cur = timePeaks.elementAt(i).elementAt(j);
                    outTNotes.println(cur.leftBorder + " " + cur.rightBorder + " " + cur.power);
                }

            }

            for (int i = 0; i < notes.size(); ++i) {
                System.out.println((i + 1) + " " + notes.elementAt(i));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
