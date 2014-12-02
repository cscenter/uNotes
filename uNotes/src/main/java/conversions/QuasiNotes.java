package conversions;

import conversions.notes.MidiHelper;
import conversions.notes.NoteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakExtractor;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class QuasiNotes {
    public double myRelativePowerThreshold;
    public double myPowerThreshold;
    public double myStatisticalSignificance;
    private int myMinMidiCode;
    private int myMaxMidiCode;
    private double myTimeStep;
    private ArrayList<double[]> myNotePowerSeries = new ArrayList<double[]>();

    @Deprecated
    public QuasiNotes(Spectrum spectrum) throws FileNotFoundException {
        this(spectrum, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE);   //all MIDI notes (from C-1 to G9)
    }

    public QuasiNotes(Spectrum spectrum, double relativePowerThreshold, double powerThreshold, double statisticalSignificance) {
        this(spectrum, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE, relativePowerThreshold, powerThreshold, statisticalSignificance);
    }

    @Deprecated
    public QuasiNotes(Spectrum spectrum, int minMidiCode, int maxMidiCode) {
        this(spectrum, minMidiCode, maxMidiCode, 20.0, 0.0, 0.003);
    }

    public QuasiNotes(Spectrum spectrum, int minMidiCode, int maxMidiCode, double relativePowerThreshold, double powerThreshold, double statisticalSignificance) {
        myRelativePowerThreshold = relativePowerThreshold;
        myPowerThreshold = powerThreshold;
        myStatisticalSignificance = statisticalSignificance;

        myMinMidiCode = minMidiCode;
        myMaxMidiCode = maxMidiCode;
        myTimeStep = spectrum.getTimeStep();

        ArrayList<double[]> power = spectrum.getPowerSpectrum();
        ArrayList<double[]> alignedPower = spectrum.getAlignedPowerSpectrum();

        double dt = spectrum.getTimeStep();
        double dnu = spectrum.getFrequencyStep();

        NoteAlphabet noteAlphabet = new NoteAlphabet(minMidiCode, maxMidiCode);
        double[] notes = noteAlphabet.getFrequencies();

        WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(spectrum, noteAlphabet.getFrequencies());
        ArrayList<double[]> wPower = noteGetter.spectrumTransformWithCounts(alignedPower);

        PeakExtractor pex = new PeakExtractor(dt, dnu);
        pex.loadSpectrum(power);
        pex.extract();
        ArrayList<ArrayList<Peak>> peaks = pex.getPeaks();


        for (int i = 0; i < (int) (spectrum.getTimeZeroPoint() / myTimeStep + 1.0e-7); ++i) {
            myNotePowerSeries.add(new double[notes.length]);
        }

        //
        //PrintStream outNotes = new PrintStream(new File(new File("test", "output"), myStatisticalSignificance + "logger.dat"));
        //
        double criticalNoise = 0;
        for (int i = 0; i < peaks.size(); ++i) {
            criticalNoise += getCriticalNoise(alignedPower.get(i), myStatisticalSignificance);
        }
        criticalNoise /= peaks.size();

        for (int i = 0; i < peaks.size(); ++i) {
            double[] notePowerSlice = new double[notes.length];
            //double criticalNoise = getCriticalNoise(alignedPower.get(i), myStatisticalSignificance);

            /*
            double mean = 0;
            for (int j = 0; j < power.get(i).length; ++j) {
                mean += power.get(i)[j];
            }
            mean /= power.get(i).length;
            double var = 0;
            for (int j = 0; j < power.get(i).length; ++j) {
                var += Math.pow(power.get(i)[j] - mean, 2.0);
            }
            var /= (power.get(i).length - 1);
            outNotes.println(myNotePowerSeries.size() * dt + "   " + criticalNoise + "  " + mean + "  " + Math.sqrt(var));
            */

            for (int j = 0; j < peaks.get(i).size(); j++) {
                Peak cur = peaks.get(i).get(j);
                if (cur.powerRel > myRelativePowerThreshold & cur.power > myPowerThreshold) {
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

    public static double getCriticalNoise(double[] selection, double statisticalSignificance) {
        if ((statisticalSignificance > 1.0) || (statisticalSignificance < 0.0)) {
            throw new IllegalArgumentException("statisticalSignificance must belong to the interval [0, 1]");
        }

        double variance = 0;
        for (double value : selection) {
            variance += (value * value);
        }
        variance = variance / (double) (selection.length - 1);

        return (-variance * Math.log(statisticalSignificance) / (double) selection.length);
    }
}
