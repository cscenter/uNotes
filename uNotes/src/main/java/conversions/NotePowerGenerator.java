package conversions;

import conversions.notes.NoteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakExtractor;

import java.util.ArrayList;

public class NotePowerGenerator {   //TODO rename
    private int minMidiCode;
    private int maxMidiCode;
    private double timeStep;
    private double timeZeroPoint;
    private ArrayList<double[]> notePowerSeries = new ArrayList<double[]>();

    public static final int ALIGNMENT_LOCALIZATION_FACTOR = 20;

    public NotePowerGenerator(Spectrum spectrum) {
        this(spectrum, 0, 127);   //all MIDI notes (from C-1 to G9)
    }

    public NotePowerGenerator(Spectrum spectrum, int minMidiCode, int maxMidiCode) {
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
            notePowerSeries.add(notePowerSlice);
        }
    }

    public int getMaxMidiCode() {
        return maxMidiCode;
    }

    public int getMinMidiCode() {
        return minMidiCode;
    }

    public double getTimeStep() {
        return timeStep;
    }

    public double getTimeZeroPoint() {
        return timeZeroPoint;
    }

    public ArrayList<double[]> getNotePowerSeries() {
        return notePowerSeries;
    }
}
