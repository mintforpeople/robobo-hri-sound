package com.mytechia.robobo.framework.hri.sound.soundStream;

import com.mytechia.robobo.framework.RoboboManager;
import com.mytechia.robobo.framework.remote_control.remotemodule.IRemoteControlModule;

public abstract class ASoundStreamModule implements ISoundStreamModule{
    protected ISoundStreamModule soundStreamModule;
    protected IRemoteControlModule remoteModule = null;
    protected RoboboManager m;
}
