package conversions;

import conversions.fourier.BlackmanWindow;
import conversions.fourier.STFT;
import conversions.notes.NoteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakExtractor;

import javax.sound.sampled.AudioInputStream;
import java.util.ArrayList;

public class NotePowerGenerator {

    public static ArrayList<double[]> getNotePower(AudioInputStream inputStream, int windowLength, int timeStepLength) {
        return getNotePower(inputStream, windowLength, timeStepLength, 0, 127);   //all MIDI notes (from C-1 to G9)
    }

    public static ArrayList<double[]> getNotePower(Spectrum spectrum) {
        return getNotePower(spectrum, 0, 127);   //all MIDI notes (from C-1 to G9)
    }

    public static ArrayList<double[]> getNotePower(AudioInputStream inputStream, int windowLength, int timeStepLength, int minMidiCode, int maxMidiCode) {

        TimeSeries series = new TimeSeries(inputStream);
        series.start();

        STFT stft = new STFT(windowLength, timeStepLength, new BlackmanWindow());
        Spectrum spectrum = stft.transform(series);

        // At first we must align the fourier spectrum
        spectrum.alignment(20); //TODO magic constant

        return getNotePower(spectrum, minMidiCode, maxMidiCode);
    }

    public static ArrayList<double[]> getNotePower(Spectrum spectrum, int minMidiCode, int maxMidiCode) {
        ArrayList<double[]> power = spectrum.getPowerSpectrum();

        double dt = spectrum.getTimeStep();
        double dnu = spectrum.getFrequencyStep();

        NoteAlphabet noteAlphabet = new NoteAlphabet(minMidiCode, maxMidiCode);
        ArrayList<Double> notes = noteAlphabet.getFrequencies();

        //  TODO We assume that spectrum is aligned
        // Then we can make secondary wavelet spectrum in notes frequencies, corresponding to noteAlphabet
        WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(spectrum, noteAlphabet.getAllFrequencies());
        ArrayList<double[]> wPower = noteGetter.spectrumTransformWithCounts(spectrum);

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
                    if (noteMidiCode < noteAlphabet.getMinMidiCode() || noteMidiCode > noteAlphabet.getMaxMidiCode()) {
                        continue;
                    }
                    int noteIndex = noteMidiCode - noteAlphabet.getMinMidiCode();

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
