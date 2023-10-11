package com.mytechia.robobo.framework.hri.sound.soundStream.websocket;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.mytechia.commons.framework.exception.InternalErrorException;
import com.mytechia.robobo.framework.RoboboManager;
import com.mytechia.robobo.framework.hri.sound.soundStream.ASoundStreamModule;
import com.mytechia.robobo.framework.remote_control.remotemodule.Command;
import com.mytechia.robobo.framework.remote_control.remotemodule.ICommandExecutor;
import com.mytechia.robobo.framework.remote_control.remotemodule.IRemoteControlModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AndroidMicrophoneSoundStreamModule extends ASoundStreamModule  {
    private static final String TAG = "AndMicSoundStreamModule";
    static final int QUEUE_LENGTH = 60;

    IRemoteControlModule rcmodule;

    private static final int LOW_SAMPLE_RATE = 22050; // Frecuencia de muestreo en Hz
    private static final int DEFAULT_SAMPLE_RATE = 44100; // Frecuencia de muestreo en Hz
    private static final int HIGH_SAMPLE_RATE = 48000; // Frecuencia de muestreo en Hz
    private static final int HIGHEST_SAMPLE_RATE = 96000; // Frecuencia de muestreo en Hz
    private static final int EXTREME_SAMPLE_RATE = 192000; // Frecuencia de muestreo en Hz

    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    public boolean isRecording;
    public AudioRecord audioRecord;

    public UDPServer server;

    private int BUFFER_SIZE = 4096;

    private int sync_id = -1;

    private Thread audioThread;

    private int sampleRate;
    private int channelConfig;
    private int audioFormat;

    @Override
    public void startup(RoboboManager manager) throws InternalErrorException {
        m = manager;

        sampleRate = DEFAULT_SAMPLE_RATE;
        channelConfig = DEFAULT_CHANNEL_CONFIG;
        audioFormat = DEFAULT_AUDIO_FORMAT;

        rcmodule.registerCommand("START-AUDIOSTREAM", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                startRecording();
            }
        });

        rcmodule.registerCommand("STOP-AUDIOSTREAM", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                try{
                    stopRecording();
                } catch (InterruptedException e){
                    Log.e(TAG, e.toString());
                }
            }
        });

        rcmodule.registerCommand("SET-AUDIOSTREAM-SAMPLERATE", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                if (c.getParameters().containsKey("sampleRate")) {
                    try{
                        stopRecording();
                        setSampleRate(Integer.parseInt(c.getParameters().get("sampleRate")));
                        startRecording();
                    } catch (Exception e){
                        Log.e(TAG, e.getMessage());
                    }

                }
            }
        });

        rcmodule.registerCommand("AUDIO-SYNC", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                if (c.getParameters().containsKey("id")) {
                    setSyncId(Integer.parseInt(c.getParameters().get("id")));
                }
            }
        });

        server = new UDPServer(BUFFER_SIZE);
        server.start();
    }

    @Override
    public void shutdown() throws InternalErrorException {

    }

    @Override
    public String getModuleInfo() {
        return null;
    }

    @Override
    public String getModuleVersion() {
        return null;
    }

    protected void startRecording(){

        BUFFER_SIZE = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        if (BUFFER_SIZE == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid parameter for AudioRecord");
            return;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                BUFFER_SIZE
        );
        isRecording = true;
        audioRecord.startRecording();

        audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] audioBuffer = new byte[BUFFER_SIZE];
                while (isRecording) {
                    int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION ||
                            bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "Error reading audio data.");
                    } else {
                        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES*2);
                        buffer.putLong(0, System.currentTimeMillis());
                        buffer.putLong(8,sync_id);
                        sync_id = -1;

                        byte[] metadataBytes = buffer.array();

                        ByteArrayOutputStream output = new ByteArrayOutputStream();

                        try {
                            output.write(audioBuffer);
                            output.write(metadataBytes);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        byte[] out = output.toByteArray();
                        server.addToQueue(out);
                    }
                }
            }
        });

        audioThread.start();
    }

    protected void stopRecording() throws InterruptedException {
        isRecording = false;
        audioRecord.stop();
        audioThread.join();
    }

    @Override
    public void setSampleRate(int sampleRate){
        if (!isRecording){
            switch (sampleRate){
                case LOW_SAMPLE_RATE:
                    sampleRate = LOW_SAMPLE_RATE;
                    break;
                case DEFAULT_SAMPLE_RATE:
                    sampleRate = DEFAULT_SAMPLE_RATE;
                    break;
                case HIGH_SAMPLE_RATE:
                    sampleRate = HIGH_SAMPLE_RATE;
                    break;
                case HIGHEST_SAMPLE_RATE:
                    sampleRate = HIGHEST_SAMPLE_RATE;
                    break;
                case EXTREME_SAMPLE_RATE:
                    sampleRate = EXTREME_SAMPLE_RATE;
                    break;
                default:
                    break;
            }
        }
    }

    public void setSyncId(int id) {
        sync_id = id;
    }
}
