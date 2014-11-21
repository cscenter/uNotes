package conversions;

import conversions.notes.MidiHelper;
import conversions.notes.NoteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakExtractor;

import java.util.ArrayList;

public class QuasiNotes {   //TODO rename
    private int myMinMidiCode;
    private int myMaxMidiCode;
    private double myTimeStep;
    private ArrayList<double[]> myNotePowerSeries = new ArrayList<double[]>();

    public QuasiNotes(Spectrum spectrum) {
        this(spectrum, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE);   //all MIDI notes (from C-1 to G9)
    }

    public QuasiNotes(Spectrum spectrum, int minMidiCode, int maxMidiCode) {
        myMinMidiCode = minMidiCode;
        myMaxMidiCode = maxMidiCode;
        myTimeStep = spectrum.getTimeStep();

        ArrayList<double[]> power = spectrum.getPowerSpectrum();

        double dt = spectrum.getTimeStep();
        double dnu = spectrum.getFrequencyStep();

        NoteAlphabet noteAlphabet = new NoteAlphabet(minMidiCode, maxMidiCode);
        ArrayList<Double> notes = noteAlphabet.getFrequencies();

        WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(spectrum, noteAlphabet.getAllFrequencies());
        ArrayList<double[]> wPower = noteGetter.spectrumTransformWithCounts(spectrum);

        PeakExtractor pex = new PeakExtractor(dt, dnu);
        pex.loadSpectrum(power);
        pex.extract();
        ArrayList<ArrayList<Peak>> peaks = pex.getPeaks();


        for (int i = 0; i < (int)(spectrum.getTimeZeroPoint() / myTimeStep + 1.0e-7); ++i) {
            myNotePowerSeries.add(new double[notes.size()]);
        }

        for (int i = 0; i < peaks.size(); ++i) {
            double[] notePowerSlice = new double[notes.size()];
            double criticalNoise = getCriticalNoise(wPower.get(i), 0.1);
            for (int j = 0; j < peaks.get(i).size(); j++) {
                Peak cur = peaks.get(i).get(j);
                if (cur.powerRel > 10 & cur.power > 10) {
                    int noteMidiCode = MidiHelper.getMidiCode(cur.center);
                    //  If peak frequency if too high or too low:
                    if (noteMidiCode < noteAlphabet.getMinMidiCode() || noteMidiCode > noteAlphabet.getMaxMidiCode()) {
                        continue;
                    }
                    int noteIndex = noteMidiCode - noteAlphabet.getMinMidiCode();

                    if (wPower.get(i)[noteIndex] < criticalNoise) {
                        continue;
                    }

                    if (notePowerSlice[noteIndex] == 0 || notePowerSlice[noteIndex] < cur.power) {
                        notePowerSlice[noteIndex] = cur.power;
                    }
                }
            }
            myNotePowerSeries.add(notePowerSlice);
        }
    }

    public int getMaxMidiCode() {
        return myMaxMidiCode;
    }

    public int getMinMidiCode() {
        return myMinMidiCode;
    }

    public double getTimeStep() {
        return myTimeStep;
    }

    public ArrayList<double[]> getNotePowerSeries() {
        return myNotePowerSeries;
    }

    private double getCriticalNoise(double[] selection, double statisticalSignificance) {
        if ((statisticalSignificance > 1.0) || (statisticalSignificance < 0.0)) {
            throw new IllegalArgumentException("statisticalSignificance must belongs to the interval [0, 1]");
        }
        double variance = 0;
        for (int i = 0; i < selection.length; ++i) {
            variance += (selection[i] * selection[i]);
        }
        variance = variance / (double) (selection.length - 1);

        return (-variance * variance * Math.log(statisticalSignificance) / (double)selection.length);
    }
}
