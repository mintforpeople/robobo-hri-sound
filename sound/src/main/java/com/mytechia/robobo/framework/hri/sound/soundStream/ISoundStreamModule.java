package com.mytechia.robobo.framework.hri.sound.soundStream;

import com.mytechia.robobo.framework.IModule;

public interface ISoundStreamModule extends IModule {
    public void startRecording();
    public void stopRecording() throws InterruptedException;
}
