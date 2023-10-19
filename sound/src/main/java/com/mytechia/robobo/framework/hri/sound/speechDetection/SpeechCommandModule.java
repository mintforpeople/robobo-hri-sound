package com.mytechia.robobo.framework.hri.sound.speechDetection;

import com.mytechia.commons.framework.exception.InternalErrorException;
import com.mytechia.robobo.framework.IModule;
import com.mytechia.robobo.framework.RoboboManager;
import com.mytechia.robobo.framework.exception.ModuleNotFoundException;
import com.mytechia.robobo.framework.remote_control.remotemodule.Command;
import com.mytechia.robobo.framework.remote_control.remotemodule.IRemoteControlModule;

import java.util.HashMap;

public class SpeechCommandModule implements ISpeechListener , IModule {
    protected RoboboManager m;
    protected IRemoteControlModule rcmodule = null;
    protected ISpeechDetectionModule speechModule = null;
    private HashMap<String, Command> cmdMap = null;

    public SpeechCommandModule() {
        cmdMap = new HashMap<String, Command>();
    }

    @Override
    public void onResult(String s) {
        Command c = cmdMap.get(s);
        if(c != null)
            rcmodule.queueCommand(c);
    }

    public void setCmd(String s, Command cmd){
        this.cmdMap.put(s, cmd);
    }

    public void removeCmd(String s){
        this.cmdMap.remove(s);
    }

    @Override
    public void startup(RoboboManager manager) throws InternalErrorException {
        m = manager;
        try {
            rcmodule = manager.getModuleInstance(IRemoteControlModule.class);
            speechModule = manager.getModuleInstance(ISpeechDetectionModule.class);

            speechModule.suscribeAny(this);
        } catch (ModuleNotFoundException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void shutdown() throws InternalErrorException {

    }

    @Override
    public String getModuleInfo() {
        return "Speech command request module";
    }

    @Override
    public String getModuleVersion() {
        return "v0.1";
    }
}
