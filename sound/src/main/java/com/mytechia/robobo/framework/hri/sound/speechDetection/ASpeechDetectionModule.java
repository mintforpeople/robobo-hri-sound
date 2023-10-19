package com.mytechia.robobo.framework.hri.sound.speechDetection;


import android.text.TextUtils;

import com.mytechia.robobo.framework.RoboboManager;
import com.mytechia.robobo.framework.remote_control.remotemodule.Command;
import com.mytechia.robobo.framework.remote_control.remotemodule.ICommandExecutor;
import com.mytechia.robobo.framework.remote_control.remotemodule.IRemoteControlModule;
import com.mytechia.robobo.framework.remote_control.remotemodule.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class ASpeechDetectionModule implements ISpeechDetectionModule {

    public HashSet<ISpeechListener> anyListeners;
    public HashMap<String, ISpeechListener> phraselisteners;
    public List<String> remotePhrases;
    protected RoboboManager m;
    protected IRemoteControlModule remoteModule = null;
    protected boolean doDetection = true;

    public ASpeechDetectionModule(){
        anyListeners = new HashSet<ISpeechListener>();
        phraselisteners = new HashMap<String, ISpeechListener>();
        remotePhrases = new ArrayList<String>();
    }

    //public void

    @Override
    public void suscribePhrase(ISpeechListener listener, String phrase) {
        phraselisteners.put(phrase, listener);
    }

    @Override
    public void unsuscribePhrase(ISpeechListener listener, String phrase) {
        phraselisteners.remove(phrase, listener);
    }

    @Override
    public void suscribeAny(ISpeechListener listener) {
        anyListeners.add(listener);
    }

    @Override
    public void unsuscribeAll(final ISpeechListener listener) {
        anyListeners.remove(listener);
        if(phraselisteners.containsValue(listener)) {
            phraselisteners.values().removeAll(Collections.singleton(listener)); // remove(listener) would only remove the first one
        }
    }

    protected void registerCommands(){
        remoteModule.registerCommand("ADD-PHRASE", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                String phrase = c.getParameters().get("phrase");
                remotePhrases.add(phrase);

                Status status = new Status("REGISTERED-PHRASES");
                status.putContents("phrases", TextUtils.join(",", remotePhrases));
                remoteModule.postStatus(status);

            }
        });
        remoteModule.registerCommand("REMOVE-PHRASE", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                String phrase = c.getParameters().get("phrase");
                remotePhrases.remove(phrase);

                Status status = new Status("REGISTERED-PHRASES");
                status.putContents("phrases", TextUtils.join(",", remotePhrases));
                remoteModule.postStatus(status);
            }
        });
        remoteModule.registerCommand("START-SPEECH-DETECTION", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                doDetection = true;
            }
        });
        remoteModule.registerCommand("STOP-SPEECH-DETECTION", new ICommandExecutor() {
            @Override
            public void executeCommand(Command c, IRemoteControlModule rcmodule) {
                doDetection = false;
            }
        });
    }


    //public void suscribe(ISpeechListener listener){ listeners.add(listener); }
    //public void unsuscribe(ISpeechListener listener){ listeners.remove(listener); }

}
