import conversions.*;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.Vector;

public class WaveletSpectrumTransformRunner {
    public static void main(String[] args) {
        int timeStepLength = 256;
        int windowLength = 2048;

        File dir = new File("test", "music");
        String inputFileName = "a.wav";
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

            //
            //result.alignment(20);
            //

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.elementAt(i).length; j++) {
                    out.println((i * dt + t0) + "   " + (j * dnu + nu0) + "  " + power.elementAt(i)[j]);
                }
            }

            //
            result.alignment(20);
            //

            WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(result);

            Spectrum subSpectrum = noteGetter.spectrumTransform(result);

            power = subSpectrum.getPowerSpectrum();

            t0 = subSpectrum.getTimeZeroPoint();
            nu0 = subSpectrum.getFrequencyZeroPoint();

            dt = subSpectrum.getTimeStep();
            dnu = subSpectrum.getFrequencyStep();
            PrintStream outNotes = new PrintStream(new File(inputFileName + ".wt2.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 1; j < power.elementAt(i).length; j++) {
                    outNotes.println((i * dt + t0) + "   " + (j * dnu + nu0) + "   " + power.elementAt(i)[j]);
                }
            }

            /*
            PrintStream outPeaks = new PrintStream(new File(inputFileName + ".pkt.dat"));
            PeakCrossExtractor pke = new PeakCrossExtractor(dt, dnu, 10);

            pke.loadRaws(power);

            pke.extract(63);

            Vector<Vector<Peak>> timePeaks  = pke.getPeaks();

            for (int i = 0; i < timePeaks.size(); ++i) {
                for (int j = 0; j < timePeaks.elementAt(i).size(); j++) {
                    Peak temp = timePeaks.elementAt(i).elementAt(j);
                    outPeaks.println(temp.center + " " + temp.power + " " + temp.powerRel + " " + temp.width);
                }
            }
            */

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
