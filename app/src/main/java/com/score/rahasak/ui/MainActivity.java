package com.score.rahasak.ui;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.util.Log;

import com.score.rahasak.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {
    private AudioRecord recorder;
    private AudioTrack player;

    private MediaCodec encoder;
    private MediaCodec decoder;

    private short audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private short channelConfig = AudioFormat.CHANNEL_IN_MONO;

    private int bufferSize;
    private boolean isRecording;
    private boolean isPlaying;

    private Thread IOrecorder;

    private Thread IOudpPlayer;


    private DatagramSocket ds;
    private final int localPort = 39000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);

        IOrecorder = new Thread(new Runnable() {
            public void run() {
                int read;
                byte[] buffer1 = new byte[bufferSize];

                ByteBuffer[] inputBuffers;
                ByteBuffer[] outputBuffers;

                ByteBuffer inputBuffer;
                ByteBuffer outputBuffer;

                MediaCodec.BufferInfo bufferInfo;
                int inputBufferIndex;
                int outputBufferIndex;

                byte[] outData;

                DatagramPacket packet;
                try {
                    encoder.start();
                    recorder.startRecording();
                    isRecording = true;
                    while (isRecording) {
                        read = recorder.read(buffer1, 0, bufferSize);

                        inputBuffers = encoder.getInputBuffers();
                        outputBuffers = encoder.getOutputBuffers();
                        inputBufferIndex = encoder.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();

                            inputBuffer.put(buffer1);

                            encoder.queueInputBuffer(inputBufferIndex, 0, buffer1.length, 0, 0);
                        }

                        bufferInfo = new MediaCodec.BufferInfo();
                        outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);


                        while (outputBufferIndex >= 0) {
                            outputBuffer = outputBuffers[outputBufferIndex];

                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                            outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);


                            // Log.d("AudioEncoder", outData.length + " bytes encoded");
                            //-------------
                            packet = new DatagramPacket(outData, outData.length,
                                    InetAddress.getByName("127.0.0.1"), localPort);
                            ds.send(packet);
                            //------------

                            encoder.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);

                        }
                        // ----------------------;

                    }
                    encoder.stop();
                    recorder.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        IOudpPlayer = new Thread(new Runnable() {
            public void run() {
                SocketAddress sockAddress;
                String address;

                int len = 1024;
                byte[] buffer2 = new byte[len];
                DatagramPacket packet;

                byte[] data;

                ByteBuffer[] inputBuffers;
                ByteBuffer[] outputBuffers;

                ByteBuffer inputBuffer;
                ByteBuffer outputBuffer;

                MediaCodec.BufferInfo bufferInfo;
                int inputBufferIndex;
                int outputBufferIndex;
                byte[] outData;
                try {
                    player.play();
                    decoder.start();
                    isPlaying = true;
                    while (isPlaying) {
                        try {
                            packet = new DatagramPacket(buffer2, len);
                            ds.receive(packet);

                            sockAddress = packet.getSocketAddress();
                            address = sockAddress.toString();

                            //   Log.d("UDP Receiver"," received !!! from " + address);

                            data = new byte[packet.getLength()];
                            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                            // Log.d("UDP Receiver",  data.length + " bytes received");

                            //===========
                            inputBuffers = decoder.getInputBuffers();
                            outputBuffers = decoder.getOutputBuffers();
                            inputBufferIndex = decoder.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                inputBuffer = inputBuffers[inputBufferIndex];
                                inputBuffer.clear();

                                inputBuffer.put(data);

                                decoder.queueInputBuffer(inputBufferIndex, 0, data.length, 0, 0);
                            }

                            bufferInfo = new MediaCodec.BufferInfo();
                            outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);

                            while (outputBufferIndex >= 0) {
                                outputBuffer = outputBuffers[outputBufferIndex];

                                outputBuffer.position(bufferInfo.offset);
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                                outData = new byte[bufferInfo.size];
                                outputBuffer.get(outData);

                                //  Log.d("AudioDecoder", outData.length + " bytes decoded");

                                player.write(outData, 0, outData.length);

                                decoder.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);

                            }

                            //===========

                        } catch (IOException e) {
                        }
                    }

                    decoder.stop();
                    player.stop();

                } catch (Exception e) {
                }
            }
        });

//===========================================================
        int rate = findAudioRecord();
        if (rate != -1) {
            Log.v("=========media ", "ready: " + rate);
            Log.v("=========media channel ", "ready: " + channelConfig);

            boolean encoderReady = setEncoder(rate);
            Log.v("=========encoder ", "ready: " + encoderReady);
            if (encoderReady) {
                boolean decoderReady = setDecoder(rate);
                Log.v("=========decoder ", "ready: " + decoderReady);
                if (decoderReady) {
                    Log.d("TAG", bufferSize + "");
                    try {
                        setPlayer(rate);

                        ds = new DatagramSocket(localPort);
                        IOudpPlayer.start();

                        IOrecorder.start();

                    } catch (SocketException e) {
                        e.printStackTrace();
                    }


                }

            }
        }
    }


    protected void onDestroy() {

        recorder.release();
        player.release();
        encoder.release();
        decoder.release();

    }
/*
    protected void onResume()
    {

        // isRecording = true;
    }

    protected void onPause()
    {

        isRecording = false;
    }
*/

    private int findAudioRecord() {
        for (int rate : new int[]{44100}) {
            try {
                Log.v("TAG", rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
                bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                    // check if we can instantiate and have a success
                    recorder = new AudioRecord(AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);

                    if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                        Log.v("TAG", rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);

                        return rate;
                    }
                }
            } catch (Exception e) {
                Log.v("error", "" + rate);
            }

        }
        return -1;
    }

    private boolean setEncoder(int rate) {
        //encoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, rate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1024);//AAC-HE 64kbps
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return true;
    }

    private boolean setDecoder(int rate) {
        //decoder = MediaCodec.createDecoderByType("audio/mp4a-latm");
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, rate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1024);//AAC-HE 64kbps
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);

        decoder.configure(format, null, null, 0);

        return true;
    }

    private boolean setPlayer(int rate) {
        int bufferSizePlayer = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, audioFormat);
        Log.d("====buffer Size player ", String.valueOf(bufferSizePlayer));

        player = new AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, audioFormat, bufferSizePlayer, AudioTrack.MODE_STREAM);


        if (player.getState() == AudioTrack.STATE_INITIALIZED) {

            return true;
        } else {
            return false;
        }

    }


}