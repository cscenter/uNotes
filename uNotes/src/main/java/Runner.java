import сonversions.*;
import сonversions.fourier.BlackmanWindow;
import сonversions.fourier.STFT;


import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.Vector;

/**
 * Created by Денис on 04.10.2014.
 */
public class Runner {
    public static void main(String[] args){
        int nhop = 100;
        int nfft = 512;
        File in = new File("a.wav");


        System.out.print("uNotes");

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(in);

            TimeSeries series = new TimeSeries(stream);
            series.start();

            STFT stft = new STFT(nfft, nhop, new BlackmanWindow());
            Spectrum result = stft.makeTransformation(series);

            Vector<double[]> power = result.getPower();

            double dt = result.getTimeStep();
            double dnu = result.getFrequencyStep();
            PrintStream out = new PrintStream(new File("power.dat"));

            for (int i = 0; i < power.size(); ++i){
                for (int j = 0; j < power.elementAt(i).length; j++) {
                    out.println(i * dt + "   " + j * dnu + "  " + power.elementAt(i)[j]);
                }
            }


        }catch(Exception ex)
        {
            System.out.print(ex.getMessage());
        }
    }
}
