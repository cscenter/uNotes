import сonversions.Spectrum;
import сonversions.TimeSeries;
import сonversions.fourier.BlackmanWindow;
import сonversions.fourier.STFT;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.PrintStream;
import java.util.Vector;

class Runner {
    public static void main(String[] args) {
        int nhop = 100;
        int nfft = 512;

        File dir = new File("test", "music");
        String inputFileName = "a.wav";
        File in = new File(dir, inputFileName);

        System.out.println("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(nfft, nhop, new BlackmanWindow());
            Spectrum result = stft.transform(series);

            Vector<double[]> power = result.getPowerSpectrum();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File(inputFileName + ".power.dat"));

            for (int i = 0; i < power.size(); ++i) {
                for (int j = 0; j < power.elementAt(i).length; j++) {
                    out.println(i * dt + "   " + j * dnu + "  " + power.elementAt(i)[j]);
                }
            }


        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
    }
}
