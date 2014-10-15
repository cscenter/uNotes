package сonversions;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Vector;


public class TimeSeries {
    private AudioInputStream input;
    private AudioFormat format;
    private int bytesPerWavFrame;
    private int frameLen;
    private int channels;
    private int sampleRate;
    private Vector<double[]> tracks = new Vector<double[]>();
    private Vector<Double> track = new Vector<Double>();
    private Vector<Double> sum = new Vector<Double>();

    private static final double rmsTarget = 0.08;
    private static final double rmsAlpha = 0.001;
    private double rms = 1;
    private static final double rmsMin = 0.5 * rmsTarget;

    // The line should be open, but not started yet.
    private int readFrame() {
        byte[] b = new byte[getBytesPerWavFrame()];
        int rs = 0;
        try {
            rs = input.read(b);
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
        tracks.add(wav);
        track.add(avg);
        sum.add(s);
        return rs;
    }

    private void normalize(double rms) {
        for (int i = 0; i < getFrameLen(); ++i) {
            for (int j = 0; j < getChannels(); ++j) {
                double[] temp = tracks.elementAt(i);
                temp[j] *= rmsTarget / rms;
                tracks.set(i, temp);
            }

            track.set(i, track.elementAt(i) * rmsTarget / rms);
        }
    }

    public TimeSeries(AudioInputStream input) {
        this.frameLen = (int) input.getFrameLength();
        format = input.getFormat();
        this.channels = format.getChannels();
        this.input = input;
        bytesPerWavFrame = format.getFrameSize();
        sampleRate = (int) format.getSampleRate();
    }

    public void start() {
        double rmsCur = 0;
        for (int i = 0; i < this.getFrameLen(); ++i) {
            readFrame();
            rmsCur += sum.lastElement();
        }
        rmsCur = Math.sqrt(rmsCur / getFrameLen());
        rms = rmsAlpha * rmsCur + (1 - rmsAlpha) * rms;

        // Keep it from amplifying silence too much
        if (rms < rmsMin) {
            //      System.out.print(".");
            rms = rmsMin;
        }
        normalize(rms);
    }

    public double[] getChannel(int channel) {
        double[] answ = new double[getFrameLen()];
        for (int i = 0; i < getFrameLen(); ++i) {
            answ[i] = tracks.elementAt(i)[channel];
        }
        return answ;
    }

    public double[] getTrack() {
        double[] answ = new double[getFrameLen()];
        for (int i = 0; i < getFrameLen(); ++i) {
            answ[i] = track.elementAt(i);
        }
        return (answ);
    }


    // Convert a byte stream into a stream of doubles.  If it's stereo,
    // the channels will be interleaved with each other in the double
    // stream, as in the byte stream.
    void bytes2doubles(byte[] audioBytes, double[] audioData) {
        if (format.getSampleSizeInBits() == 16) {
            if (format.isBigEndian()) {
                for (int i = 0; i < audioData.length; i++) {
           /* First byte is MSB (high order) */
                    int MSB = (int) audioBytes[2 * i];
           /* Second byte is LSB (low order) */
                    int LSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = ((double) (MSB << 8 | (255 & LSB)))
                            / 32768.0;
                }
            } else {
                for (int i = 0; i < audioData.length; i++) {
           /* First byte is LSB (low order) */
                    int LSB = (int) audioBytes[2 * i];
           /* Second byte is MSB (high order) */
                    int MSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = ((double) (MSB << 8 | (255 & LSB)))
                            / 32768.0;
                }
            }
        } else if (format.getSampleSizeInBits() == 8) {
            if (format.getEncoding().toString().startsWith("PCM_SIGN")) {
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
        return bytesPerWavFrame;
    }

    public int getFrameLen() {
        return frameLen;
    }

    int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
