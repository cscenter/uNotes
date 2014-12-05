package conversions;

import conversions.notes.MidiHelper;
import conversions.notes.NoteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakExtractor;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

public class QuasiNotes {
    public double myRelativePowerThreshold;
    public double myPowerThreshold;
    public double myStatisticalSignificance;
    private int myMinMidiCode;
    private int myMaxMidiCode;
    private double myTimeStep;
    private ArrayList<double[]> myNotePowerSeries = new ArrayList<double[]>();//Notes probability in second case

    //for second QuasiNotes constructor

    private NoteAlphabet myNoteAlphabet;
    private ArrayList<Peak[]> myPeaks;
    private ArrayList<Double> myAlignedAmplitude;
    private ArrayList<double[]> myWaveletPower;

    public double myAbsolutePowerThreshold = -20;

    //private ArrayList<double[]> myNotesSeries = new ArrayList<double[]>();

    @Deprecated
    public QuasiNotes(Spectrum spectrum) throws FileNotFoundException {
        this(spectrum, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE);   //all MIDI notes (from C-1 to G9)
    }

    public QuasiNotes(Spectrum spectrum, double relativePowerThreshold, double powerThreshold, double statisticalSignificance) {
        this(spectrum, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE, relativePowerThreshold,
                powerThreshold, statisticalSignificance);
    }

    @Deprecated
    public QuasiNotes(Spectrum spectrum, int minMidiCode, int maxMidiCode) {
        this(spectrum, minMidiCode, maxMidiCode, 20.0, 0.0, 0.003);
    }

    public QuasiNotes(Spectrum spectrum, int minMidiCode, int maxMidiCode, double relativePowerThreshold,
                      double powerThreshold, double statisticalSignificance) {
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

    public QuasiNotes(TimeSeries series, int startCount, int countsInRange, Spectrum spectrum,
                      double relativePowerThreshold, double powerThreshold, double statisticalSignificance,
                      double absolutePowerThreshold) throws FileNotFoundException {
        this(series, startCount, countsInRange, spectrum, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE,
                relativePowerThreshold, powerThreshold, statisticalSignificance, absolutePowerThreshold);   //all MIDI notes (from C-1 to G9)
    }

    public QuasiNotes(TimeSeries series, int startCount, int countsInRange, Spectrum spectrum,
                      int minMidiCode, int maxMidiCode,
                      double relativePowerThreshold, double powerThreshold, double statisticalSignificance,
                      double absolutePowerThreshold) {
        myMinMidiCode = minMidiCode;
        myMaxMidiCode = maxMidiCode;
        myTimeStep = spectrum.getTimeStep();

        myRelativePowerThreshold = relativePowerThreshold;
        myPowerThreshold = powerThreshold;
        myStatisticalSignificance = statisticalSignificance;
        myAbsolutePowerThreshold = absolutePowerThreshold;

        myNoteAlphabet = new NoteAlphabet(myMinMidiCode, myMaxMidiCode);

        ArrayList<double[]> power = spectrum.getPowerSpectrum();
        ArrayList<double[]> alignedPower = spectrum.getAlignedPowerSpectrum();

        double dt = spectrum.getTimeStep();
        double dnu = spectrum.getFrequencyStep();

        PeakExtractor pex = new PeakExtractor(dt, dnu);
        pex.loadSpectrum(power);
        pex.extract();
        ArrayList<ArrayList<Peak>> allPeaks = pex.getPeaks();
        extractMyPeaks(allPeaks);

        myAlignedAmplitude = series.getAlignedAmplitude(startCount, countsInRange);
        int zeroElementsNumber = myPeaks.size() - myAlignedAmplitude.size();
        for (int i = 0; i < zeroElementsNumber; ++i) {
            myAlignedAmplitude.add(0.0);
        }

        //System.out.print(myPeaks.size() + "   " + myAlignedAmplitude.size());

        //ArrayList<Double> notes = myNoteAlphabet.getFrequencies();

        WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(spectrum, myNoteAlphabet.getFrequencies());
        myWaveletPower = noteGetter.spectrumTransformWithCounts(alignedPower);

        basicValidation();

        timeValidation();

        amplitudeValidation();

        relativeValidation();

        secondaryValidation(alignedPower);
    }

    /**
     * This method extract Peaks which match the muNoteAlphabet to myPeaks
     */
    private void extractMyPeaks(ArrayList<ArrayList<Peak>> allPeaks) {
        myPeaks = new ArrayList<Peak[]>();
        for (int i = 0; i < allPeaks.size(); ++i) {
            Peak[] peakSlice = new Peak[myNoteAlphabet.getSize()];

            Arrays.fill(peakSlice, null);

            //double[] noteAmplitudeSlice = new double[myNoteAlphabet.getSize()];

            //Arrays.fill(noteAmplitudeSlice, -1000.0);

            for (int j = 0; j < allPeaks.get(i).size(); j++) {
                Peak cur = allPeaks.get(i).get(j);
                int noteMidiCode = MidiHelper.getMidiCode(cur.center);
                //  If peak frequency if too high or too low:
                if (noteMidiCode >= myNoteAlphabet.getMinMidiCode() && noteMidiCode <= myNoteAlphabet.getMaxMidiCode()) {
                    int noteIndex = noteMidiCode - myNoteAlphabet.getMinMidiCode();
                    /*
                    if (noteAmplitudeSlice[noteIndex] < cur.power) {
                        noteAmplitudeSlice[noteIndex] = cur.power;
                    }
                    */
                    if ((peakSlice[noteIndex] == null) || (peakSlice[noteIndex].power < cur.power)) {
                        peakSlice[noteIndex] = cur;
                    }
                }
            }
            myPeaks.add(peakSlice);
        }
    }

    private void basicValidation() {
        for (int i = 0; i < myPeaks.size(); ++i) {
            double[] notePowerSlice = new double[myNoteAlphabet.getSize()];
            Peak[] peakSlice = myPeaks.get(i);

            for (int j = 0; j < peakSlice.length; j++) {
                if ((peakSlice[j] != null) && (peakSlice[j].power > myAbsolutePowerThreshold)) {
                    notePowerSlice[j] = 1.0;
                }
            }

            myNotePowerSeries.add(notePowerSlice);
        }
    }

    private void timeValidation() {
        for (int i = 0; i < myAlignedAmplitude.size(); ++i) {
            double[] notePowerSlice = myNotePowerSeries.get(i);
            for (int j = 0; j < notePowerSlice.length; ++j) {
                notePowerSlice[j] *= Math.exp(-0.005 / myAlignedAmplitude.get(i));
            }
        }
    }

    private void amplitudeValidation() {
        for (int i = 0; i < myPeaks.size(); ++i) {
            Peak[] peakSlice = myPeaks.get(i);
            double maxAmplitude = -1000;
            for (int j = 0; j < peakSlice.length; ++j) {
                if ((peakSlice[j] != null) && (peakSlice[j].power > maxAmplitude)) {
                    maxAmplitude = peakSlice[j].power;
                }
            }
            double[] notePowerSlice = myNotePowerSeries.get(i);
            for (int j = 0; j < peakSlice.length; ++j) {
                if (peakSlice[j] != null) {
                    notePowerSlice[j] *= Math.exp((peakSlice[j].power - maxAmplitude) / 10.0);
                }
            }
        }
    }

    private void relativeValidation() {
        for (int i = 0; i < myPeaks.size(); ++i) {
            Peak[] peakSlice = myPeaks.get(i);
            double[] notePowerSlice = myNotePowerSeries.get(i);
            for (int j = 0; j < peakSlice.length; ++j) {
                if (peakSlice[j] != null) {
                    notePowerSlice[j] *= Math.exp(-myRelativePowerThreshold / 10.0 / peakSlice[j].powerRel);
                }
            }
        }
    }

    private void secondaryValidation(ArrayList<double[]> alignedPower) {
        for (int i = 0; i < myWaveletPower.size(); ++i) {
            double[] waveletSlice = myWaveletPower.get(i);
            double[] notePowerSlice = myNotePowerSeries.get(i);
            double[] alignedSlice = alignedPower.get(i);
            for (int j = 0; j < waveletSlice.length; ++j) {
                double exponent = Math.exp(-alignedSlice.length * waveletSlice[j] / getVariance(alignedSlice));
                notePowerSlice[j] *= Math.exp(-exponent / myStatisticalSignificance);
            }
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

    public static double getVariance(double[] selection) {
        double variance = 0;
        for (double value : selection) {
            variance += (value * value);
        }
        variance = variance / (double) (selection.length - 1);
        return variance;
    }

    public static double getCriticalNoise(double[] selection, double statisticalSignificance) {
        if ((statisticalSignificance > 1.0) || (statisticalSignificance < 0.0)) {
            throw new IllegalArgumentException("statisticalSignificance must belong to the interval [0, 1]");
        }

        double variance = getVariance(selection);

        return (-variance * Math.log(statisticalSignificance) / (double) selection.length);
    }
}
