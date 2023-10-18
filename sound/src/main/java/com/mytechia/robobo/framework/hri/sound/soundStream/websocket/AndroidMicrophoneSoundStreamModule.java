package com.mytechia.robobo.framework.hri.sound.soundStream.websocket;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.mytechia.commons.framework.exception.InternalErrorException;
import com.mytechia.robobo.framework.LogLvl;
import com.mytechia.robobo.framework.RoboboManager;
import com.mytechia.robobo.framework.exception.ModuleNotFoundException;
import com.mytechia.robobo.framework.hri.sound.soundStream.ASoundStreamModule;
import com.mytechia.robobo.framework.remote_control.remotemodule.Command;
import com.mytechia.robobo.framework.remote_control.remotemodule.ICommandExecutor;
import com.mytechia.robobo.framework.remote_control.remotemodule.IRemoteControlModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

public class AndroidMicrophoneSoundStreamModule extends ASoundStreamModule  {
    private static final String TAG = "AndMicSoundStreamModule";
    static final int QUEUE_LENGTH = 60;

    private static final int LOW_SAMPLE_RATE = 11025; // Frecuencia de muestreo en Hz
    private static final int DEFAULT_SAMPLE_RATE = 44100; // Frecuencia de muestreo en Hz
    private static final int HIGH_SAMPLE_RATE = 48000; // Frecuencia de muestreo en Hz
    private static final int HIGHEST_SAMPLE_RATE = 96000; // Frecuencia de muestreo en Hz
    private static final int EXTREME_SAMPLE_RATE = 192000; // Frecuencia de muestreo en Hz

    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    public boolean isRecording;
    public AudioRecord audioRecord;

    public UDPServer server;

    private int buffersize = 4096;

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

        // Get instances pf teh remote and sound dispatcher modules
        try {
            remoteModule = manager.getModuleInstance(IRemoteControlModule.class);

        } catch (ModuleNotFoundException e) {
            e.printStackTrace();
        }
        // Load properties from file
        Properties properties = new Properties();
        AssetManager assetManager = manager.getApplicationContext().getAssets();

        try {
            InputStream inputStream = assetManager.open("sound.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sampleRate = Integer.parseInt(properties.getProperty("samplerate"));
        buffersize = Integer.parseInt(properties.getProperty("buffersize"));

        remoteModule.registerCommand("START-AUDIOSTREAM", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                startRecording();
            }
        });

        remoteModule.registerCommand("STOP-AUDIOSTREAM", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                try{
                    stopRecording();
                } catch (InterruptedException e){
                    Log.e(TAG, e.toString());
                }
            }
        });

        remoteModule.registerCommand("SET-AUDIOSTREAM-SAMPLERATE", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                if (c.getParameters().containsKey("sampleRate")) {
                    try{
                        boolean wasRecording = isRecording;
                        if (wasRecording){
                            stopRecording();
                        }
                        int newSampleRate = Integer.parseInt(c.getParameters().get("sampleRate"));
                        setSampleRate(newSampleRate);
                        if (wasRecording){
                            startRecording();
                        }
                    } catch (Exception e){
                        Log.e(TAG, e.getMessage());
                    }

                }
            }
        });

        remoteModule.registerCommand("AUDIOSTREAM-SYNC", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                if (c.getParameters().containsKey("id")) {
                    setSyncId(Integer.parseInt(c.getParameters().get("id")));
                }
            }
        });

        if (server == null){
            server = new UDPServer(buffersize, TAG, m);
            server.start();
        }
    }

    @Override
    public void shutdown() {
        try{
            if (isRecording){
                stopRecording();
            }
            if (server != null){
                UDPServer moribund = server;
                server = null;
                moribund.stopServerRunning();
                moribund.interrupt();
            }
        } catch (InterruptedException e){
            m.logError(TAG, e.getMessage(), e);
        }
    }

    @Override
    public String getModuleInfo() {
        return "Sound Stream Module";
    }

    @Override
    public String getModuleVersion() {
        return "v0.1";
    }

    @SuppressLint("MissingPermission")
    protected void startRecording(){
        if(!isRecording && audioThread == null){
            m.log(LogLvl.DEBUG, TAG, "Started recording mic audio for streaming");
            buffersize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            if (buffersize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid parameter for AudioRecord");
                return;
            }

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    buffersize
            );
            isRecording = true;
            audioRecord.startRecording();

            audioThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] audioBuffer = new byte[buffersize];
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
                                m.logError(TAG, e.getMessage(), e);
                            }

                            byte[] out = output.toByteArray();
                            server.addToQueue(out);
                        }
                    }
                }
            });

            audioThread.start();
        }
    }

    protected void stopRecording() throws InterruptedException {
        if(isRecording && audioThread != null){
            m.log(LogLvl.DEBUG, TAG,"Stopped recording mic audio for streaming");
            isRecording = false;
            audioRecord.stop();
            Thread moribund = audioThread;
            audioThread = null;
            moribund.interrupt();
        }
    }

    @Override
    public void setSampleRate(int sampleRate){
        m.log(LogLvl.DEBUG, TAG,String.format("Changing sampleRate from %d to %d", this.sampleRate, sampleRate));
        if (!isRecording){
            switch (sampleRate){
                case LOW_SAMPLE_RATE:
                    this.sampleRate = LOW_SAMPLE_RATE;
                    break;
                case DEFAULT_SAMPLE_RATE:
                    this.sampleRate = DEFAULT_SAMPLE_RATE;
                    break;
                case HIGH_SAMPLE_RATE:
                    this.sampleRate = HIGH_SAMPLE_RATE;
                    break;
                case HIGHEST_SAMPLE_RATE:
                    this.sampleRate = HIGHEST_SAMPLE_RATE;
                    break;
                case EXTREME_SAMPLE_RATE:
                    this.sampleRate = EXTREME_SAMPLE_RATE;
                    break;
                default:
                    break;
            }
        }
    }

    public void setSyncId(int id) {
        m.log(LogLvl.DEBUG, TAG,"Syncing audio stream");
        sync_id = id;
    }
}
