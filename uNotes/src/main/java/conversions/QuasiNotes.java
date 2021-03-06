package conversions;

import conversions.notes.MidiHelper;
import conversions.notes.NoteAlphabet;
import conversions.peaks.Peak;
import conversions.peaks.PeakExtractor;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

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

    public double myAbsolutePowerThreshold;

    private double myTempo;

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

        double criticalNoise = 0;
        for (int i = 0; i < peaks.size(); ++i) {
            criticalNoise += getCriticalNoise(alignedPower.get(i), myStatisticalSignificance);
        }
        criticalNoise /= peaks.size();

        for (int i = 0; i < peaks.size(); ++i) {
            double[] notePowerSlice = new double[notes.length];

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
                      double absolutePowerThreshold, double firstOctaveFactor
            , double minDuration, double gravy, double divider, double dividerPower) throws FileNotFoundException {
        this(series, startCount, countsInRange, spectrum, MidiHelper.MIN_MIDI_CODE, MidiHelper.MAX_MIDI_CODE,
                relativePowerThreshold, powerThreshold, statisticalSignificance, absolutePowerThreshold,
                minDuration, gravy, divider, dividerPower);   //all MIDI notes (from C-1 to G9)
    }

    public QuasiNotes(TimeSeries series, int startCount, int countsInRange, Spectrum spectrum,
                      int minMidiCode, int maxMidiCode,
                      double relativePowerThreshold, double powerThreshold, double statisticalSignificance,
                      double absolutePowerThreshold, double minDuration, double gravy, double divider, double dividerPower) {
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

        myTempo = series.getTempo(startCount, countsInRange);

        WaveletSpectrumTransform noteGetter = new WaveletSpectrumTransform(spectrum, myNoteAlphabet.getFrequencies());

        myWaveletPower = noteGetter.spectrumTransformWithCounts(alignedPower);

        basicValidation();

        timeValidation();

        amplitudeValidation();

        relativeValidation();

        secondaryValidation(alignedPower);

        smooth(minDuration, gravy, divider, dividerPower);

        //smooth(60.0 / 140.0 / 4, 0.06, 2.5, 0.75);

        ArrayList<double[]> myNewNotePowerSeries = new ArrayList<double[]>();
        int i = 0;
        while (i * myTimeStep + 1e-06 < spectrum.getTimeZeroPoint()) {
            myNewNotePowerSeries.add(new double[myNoteAlphabet.getSize()]);
            ++i;
        }
        for (i = 0; i < myNotePowerSeries.size(); ++i) {
            myNewNotePowerSeries.add(myNotePowerSeries.get(i));
        }

        myNotePowerSeries = myNewNotePowerSeries;
    }

    /**
     * This method extract Peaks which match the myNoteAlphabet to myPeaks
     */
    private void extractMyPeaks(ArrayList<ArrayList<Peak>> allPeaks) {
        myPeaks = new ArrayList<Peak[]>();
        for (int i = 0; i < allPeaks.size(); ++i) {
            Peak[] peakSlice = new Peak[myNoteAlphabet.getSize()];

            Arrays.fill(peakSlice, null);

            for (int j = 0; j < allPeaks.get(i).size(); j++) {
                Peak cur = allPeaks.get(i).get(j);
                int noteMidiCode = MidiHelper.getMidiCode(cur.center);
                //  If peak frequency if too high or too low:
                if (noteMidiCode >= myNoteAlphabet.getMinMidiCode() && noteMidiCode <= myNoteAlphabet.getMaxMidiCode()) {
                    int noteIndex = noteMidiCode - myNoteAlphabet.getMinMidiCode();

                    if ((peakSlice[noteIndex] == null) || (peakSlice[noteIndex].power < cur.power)) {
                        peakSlice[noteIndex] = cur;
                    }
                }
            }
            myPeaks.add(peakSlice);
        }
    }

    private void basicValidation() {
        for (Peak[] peakSlice : myPeaks) {
            double[] notePowerSlice = new double[myNoteAlphabet.getSize()];

            for (int j = 0; j < peakSlice.length; j++) {
                if ((peakSlice[j] != null) && (peakSlice[j].power > myAbsolutePowerThreshold)) {
                    notePowerSlice[j] = 1.0;
                }
            }
            myNotePowerSeries.add(notePowerSlice);
        }
    }

    private void timeValidation() {
        for (int i = 0; i < myNotePowerSeries.size(); ++i) {
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
            for (Peak aPeak : peakSlice) {
                if ((aPeak != null) && (aPeak.power > maxAmplitude)) {
                    maxAmplitude = aPeak.power;
                }
            }
            double[] notePowerSlice = myNotePowerSeries.get(i);
            for (int j = 0; j < peakSlice.length; ++j) {
                if (peakSlice[j] != null) {
                    notePowerSlice[j] *= Math.exp((peakSlice[j].power - maxAmplitude) / 20.0);
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
                    notePowerSlice[j] *= Math.exp(-myRelativePowerThreshold / peakSlice[j].powerRel);
                }
            }
        }
    }

    private void secondaryValidation(ArrayList<double[]> alignedPower) {
        Variance varianceEvaluator = new Variance();
        for (int i = 0; i < myWaveletPower.size(); ++i) {
            double[] waveletSlice = myWaveletPower.get(i);
            double[] notePowerSlice = myNotePowerSeries.get(i);
            double[] alignedSlice = alignedPower.get(i);
            for (int j = 0; j < waveletSlice.length; ++j) {
                double exponent = Math.exp(-alignedSlice.length * waveletSlice[j] / varianceEvaluator.evaluate(alignedSlice));
                notePowerSlice[j] *= Math.exp(-exponent / myStatisticalSignificance);
            }
        }
    }

    private void smooth(double minDuration, double gravy, double divider, double dividerPower) {

        trimming(gravy);

        timeSmooth(minDuration, gravy);

        frequencySmooth(divider, dividerPower);

        trimming(gravy);

        timeSmooth(minDuration, gravy);

    }

    /**
     * This method trim our probability for some level
     */
    private void trimming(double probabilityLevel) {
        for (double[] notePowerSlice : myNotePowerSeries) {
            for (int j = 0; j < notePowerSlice.length; ++j) {
                if (notePowerSlice[j] < probabilityLevel) {
                    notePowerSlice[j] = 0.0;
                }
            }
        }
    }

    //
    private void frequencySmooth(double divider, double dividerPower) {
        for (int i = 0; i < myNotePowerSeries.size(); ++i) {
            double[] notePowerSlice = myNotePowerSeries.get(i);
            for (int j = 0; j < notePowerSlice.length; ++j) {
                if (notePowerSlice[j] > 0.0) {
                    int k = 1;
                    double currentDivider = divider;
                    while (k * 12 + j - 1 < notePowerSlice.length) {
                        divideProbability(notePowerSlice, currentDivider, j + k * 12);
                        divideProbability(notePowerSlice, Math.pow(currentDivider, dividerPower), j + k * 12 + 1);
                        divideProbability(notePowerSlice, Math.pow(currentDivider, dividerPower), j + k * 12 - 1);
                        currentDivider = Math.pow(currentDivider, dividerPower);
                        ++k;
                    }
                }
            }
        }
    }

    private void divideProbability(double[] notePowerSlice, double divider, int position) {
        if (position < notePowerSlice.length) {
            notePowerSlice[position] /= divider;
        }
    }

    private void timeSmooth(double minDuration, double gravy) {
        int durationInCounts = (int) (minDuration / myTimeStep);
        for (int noteIndex = 0; noteIndex < myNoteAlphabet.getSize(); ++noteIndex) {
            int position = 0;
            boolean isPlayed = false;
            while (position < myNotePowerSeries.size()) {
                int noteBegin = takeNoteBegin(noteIndex, position);
                if ((isPlayed) && ((noteBegin - position) < durationInCounts)) {
                    fillNote(gravy, noteIndex, position, noteBegin);
                }
                position = takeNoteEnd(noteIndex, noteBegin);
                isPlayed = true;
            }
            position = 0;
            while (position < myNotePowerSeries.size()) {
                int noteBegin = takeNoteBegin(noteIndex, position);
                int noteEnd = takeNoteEnd(noteIndex, noteBegin);
                position = noteEnd;
                if (noteEnd - noteBegin < durationInCounts) {
                    clearNote(noteIndex, noteBegin, noteEnd);
                }
            }
        }
    }

    private int takeNoteBegin(int noteCode, int position) {
        while ((position < myNotePowerSeries.size()) && (myNotePowerSeries.get(position)[noteCode] < 1.0e-9)) {
            ++position;
        }
        return position;
    }

    private int takeNoteEnd(int noteCode, int position) {
        while ((position < myNotePowerSeries.size()) && (myNotePowerSeries.get(position)[noteCode] > 1.0e-9)) {
            ++position;
        }
        return position;
    }

    private void clearNote(int noteCode, int beginPos, int endPos) {
        if (beginPos > endPos) {
            throw new IllegalArgumentException("beginPos must be less then endPos " + beginPos + " " + endPos);
        }
        if ((beginPos < 0) || (endPos < 0)) {
            throw new IllegalArgumentException("Positions must be positive beginPos = " + beginPos + " endPos = " + endPos);
        }
        for (int i = beginPos; i < endPos; ++i) {
            myNotePowerSeries.get(i)[noteCode] = 0;
        }
    }

    private void fillNote(double gravy, int noteCode, int beginPos, int endPos) {
        if (beginPos > endPos) {
            throw new IllegalArgumentException("beginPos must be less then endPos " + beginPos + " " + endPos);
        }
        if ((beginPos < 0) || (endPos < 0)) {
            throw new IllegalArgumentException("Positions must be positive beginPos = " + beginPos + " endPos = " + endPos);
        }
        for (int i = beginPos; i < endPos; ++i) {
            myNotePowerSeries.get(i)[noteCode] = gravy * 1.1;
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

    public double getTempo() {
        return myTempo;
    }

    public ArrayList<double[]> getNotePowerSeries() {
        return myNotePowerSeries;
    }

    public static double getCriticalNoise(double[] selection, double statisticalSignificance) {
        Variance varianceEvaluator = new Variance();
        if ((statisticalSignificance > 1.0) || (statisticalSignificance < 0.0)) {
            throw new IllegalArgumentException("statisticalSignificance must belong to the interval [0, 1]");
        }

        double variance = varianceEvaluator.evaluate(selection);

        return (-variance * Math.log(statisticalSignificance) / (double) selection.length);
    }
}