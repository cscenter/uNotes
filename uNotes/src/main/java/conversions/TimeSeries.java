package conversions;

import com.sun.istack.internal.NotNull;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Vector;


public class TimeSeries {
    private AudioInputStream myInput;
    private AudioFormat myFormat;
    private int myBytesPerWavFrame;
    private int myFrameLen;
    private int myChannels;
    private int mySampleRate;
    private Vector<double[]> myTracks = new Vector<double[]>();
    private Vector<Double> myTrack = new Vector<Double>();
    private Vector<Double> mySum = new Vector<Double>();

    private static final double ourRmsTarget = 0.08;
    private static final double ourRmsAlpha = 0.001;
    private double myRms = 1;
    private static final double ourRmsMin = 0.5 * ourRmsTarget;

    // The line should be open, but not started yet.
    private int readFrame() {// TODO: why do we need to return int?
        byte[] b = new byte[getBytesPerWavFrame()];
        int rs = 0;
        try {
            rs = myInput.read(b);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return (rs);
        }
        double[] wav = new double[getChannels()];
        bytes2doubles(b, wav);
        double avg = 0;
        double s = 0;
        for (int i = 0; i < getChannels(); ++i) {
            avg += wav[i];
            s += wav[i] * wav[i];
        }
        avg /= getChannels();
        s /= getChannels();
        myTracks.add(wav);
        myTrack.add(avg);
        mySum.add(s);
        return rs;
    }

    private void normalize(double rms) {// TODO: use myRms here instead of parameter rms?
        for (int i = 0; i < getFrameLen(); ++i) {
            for (int j = 0; j < getChannels(); ++j) {
                double[] temp = myTracks.elementAt(i);
                temp[j] *= ourRmsTarget / rms;
                myTracks.set(i, temp);
            }

            myTrack.set(i, myTrack.elementAt(i) * ourRmsTarget / rms);
        }
    }

    public TimeSeries(@NotNull AudioInputStream input) {
        myInput = input;
        myFrameLen = (int) myInput.getFrameLength();
        myFormat = myInput.getFormat();
        myChannels = myFormat.getChannels();
        myBytesPerWavFrame = myFormat.getFrameSize();
        mySampleRate = (int) myFormat.getSampleRate();
    }

    public void start() {
        double rmsCur = 0;
        for (int i = 0; i < this.getFrameLen(); ++i) {
            readFrame();
            rmsCur += mySum.lastElement();
        }
        rmsCur = Math.sqrt(rmsCur / getFrameLen());
        myRms = ourRmsAlpha * rmsCur + (1 - ourRmsAlpha) * myRms;

        // Keep it from amplifying silence too much
        if (myRms < ourRmsMin) {
            //      System.out.print(".");
            myRms = ourRmsMin;
        }
        normalize(myRms);
    }

    public double[] getChannel(int channel) {   // TODO: do we need this method?
        double[] answ = new double[getFrameLen()];
        for (int i = 0; i < getFrameLen(); ++i) {
            answ[i] = myTracks.elementAt(i)[channel];
        }
        return answ;
    }

    public double[] getTrack() {
        double[] track = new double[getFrameLen()];
        for (int i = 0; i < getFrameLen(); ++i) {
            track[i] = myTrack.elementAt(i);
        }
        return track;
    }


    // Convert a byte stream into a stream of doubles.  If it's stereo,
    // the channels will be interleaved with each other in the double
    // stream, as in the byte stream.
    void bytes2doubles(byte[] audioBytes, double[] audioData) {
        if (myFormat.getSampleSizeInBits() == 16) {
            if (myFormat.isBigEndian()) {
                for (int i = 0; i < audioData.length; i++) {
                    // First byte is MSB (high order)
                    int MSB = (int) audioBytes[2 * i];
                    // Second byte is LSB (low order)
                    int LSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = ((double) (MSB << 8 | (255 & LSB)))
                            / 32768.0;
                }
            } else {
                for (int i = 0; i < audioData.length; i++) {
                    // First byte is LSB (low order)
                    int LSB = (int) audioBytes[2 * i];
                    // Second byte is MSB (high order)
                    int MSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = ((double) (MSB << 8 | (255 & LSB)))
                            / 32768.0;
                }
            }
        } else if (myFormat.getSampleSizeInBits() == 8) {
            if (myFormat.getEncoding().toString().startsWith("PCM_SIGN")) {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i] / 128.0;
                }
            } else {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = (audioBytes[i] - 128) / 128.0;
                }
            }
        }
    }

    int getBytesPerWavFrame() {
        return myBytesPerWavFrame;
    }

    public int getFrameLen() {
        return myFrameLen;
    }

    int getChannels() {
        return myChannels;
    }

    public int getSampleRate() {
        return mySampleRate;
    }

    public ArrayList<Double> getAlignedAmplitude(int startCount, int countsInRange) {
        ArrayList<Double> alignedAmplitude = new ArrayList<Double>();
        int i = startCount;
        while (i < myFrameLen) {
            double amplitude = 0;
            int j = i - (countsInRange / 2);
            int rightSide = Math.min(i + (countsInRange / 2), myFrameLen);
            while (j < rightSide) {
                amplitude += Math.abs(myTrack.elementAt(j));
                ++j;
            }
            amplitude /= countsInRange;
            alignedAmplitude.add(amplitude);
            i += countsInRange;
        }
        return alignedAmplitude;
    }
}
