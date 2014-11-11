package conversions;

import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.notes.noteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakExtractor;

import javax.sound.sampled.AudioInputStream;
import java.util.ArrayList;

public class NotePowerGenerator {
    public ArrayList<double[]> getNotePower(AudioInputStream inputStream, int windowLength, int timeStepLength) {

        TimeSeries series = new TimeSeries(inputStream);
        series.start();

        STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
        Spectrum result = stft.transform(series);

        ArrayList<double[]> power = result.getPowerSpectrum();

        double t0 = result.getTimeZeroPoint();
        double nu0 = result.getFrequencyZeroPoint();

        double dt = result.getTimeStep();
        double dnu = result.getFrequencyStep();

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

                    if (notePowerSlice[noteIndex] == 0 | notePowerSlice[noteIndex] < cur.power) {
                        notePowerSlice[noteIndex] = cur.power;
                    }
                }
            }
            notePower.add(notePowerSlice);
        }

        return notePower;
    }
}
