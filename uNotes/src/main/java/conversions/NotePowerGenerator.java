package conversions;

import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.notes.NoteAlphabet;
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

        double dt = result.getTimeStep();
        double dnu = result.getFrequencyStep();

        NoteAlphabet sevenOctaves = new NoteAlphabet(2 * 12, 9 * 12 - 1);   //From C0 to B6
        ArrayList<Double> notes = sevenOctaves.getFrequencies();
        // At first we must align the fourier spectrum
        result.alignment(20);
        // Then we can make secondary wavelet spectrum in notes frequencies, corresponding to sevenOctaves
        WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(result, sevenOctaves.getAllFrequencies());
        ArrayList<double[]> wPower = noteGetter.spectrumTransformWithCounts(result);

        PeakExtractor pex = new PeakExtractor(dt, dnu);
        pex.loadSpectrum(power);
        pex.extract();
        ArrayList<ArrayList<Peak>> peaks = pex.getPeaks();

        ArrayList<double[]> notePower = new ArrayList<double[]>();

        for (int i = 0; i < peaks.size(); ++i) {
            double[] notePowerSlice = new double[notes.size()];
            for (int j = 0; j < peaks.get(i).size(); j++) {
                Peak cur = peaks.get(i).get(j);
                if (cur.powerRel > 10 & cur.power > 10) {
                    int noteMidiCode = NoteAlphabet.getMIDICode(cur.center);
                    //  If peak frequency if too high or too low:
                    if (noteMidiCode < sevenOctaves.getMinMidiCode() || noteMidiCode > sevenOctaves.getMaxMidiCode()) {
                        continue;
                    }
                    int noteIndex = noteMidiCode - sevenOctaves.getMinMidiCode();

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

        return notePower;
    }
}
