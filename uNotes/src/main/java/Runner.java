import conversions.Spectrum;
import conversions.TimeSeries;
import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.fourier.SpectrumTransformer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.Vector;
import java.lang.Math;

class Runner {
    public static void main(String[] args) {
        int timeStepLength = 100;
        int windowLength = 512;

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

            SpectrumTransformer noteGetter = new SpectrumTransformer(result);


            Vector<double[]> power = result.getPowerSpectrum();

            double t0 = result.getTimeZeroPoint();
            double nu0 = result.getFrequencyZeroPoint();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File(inputFileName + ".power.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.elementAt(i).length; j++) {
                    out.println(i * dt + t0 + "   " + j * dnu + nu0 + "  " + power.elementAt(i)[j]);
                }
            }

            PrintStream outNotes = new PrintStream(new File(inputFileName + ".fft2.dat"));

            for (int i = 0; i < power.size(); ++i) {
                double[] slice = noteGetter.transform(i);
                for (int j = 1; j < slice.length / 2; j++) {
                        outNotes.println(dt * i + t0 + "   " + slice.length * dnu / j + "  " + slice[j]);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
