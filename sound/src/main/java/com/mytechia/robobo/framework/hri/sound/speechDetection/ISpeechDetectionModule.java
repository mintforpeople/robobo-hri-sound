package com.mytechia.robobo.framework.hri.sound.speechDetection;

import com.mytechia.robobo.framework.IModule;

public interface ISpeechDetectionModule extends IModule {

    /**
     * Suscribes a listener to the notifications feed, given a phrase
     * @param listener
     * @param phrase
     */
    public void suscribePhrase(ISpeechListener listener, String phrase);

    /**
     * Unsuscribes a listener of the notifications feed of a particular phrase
     * @param listener
     * @param phrase
     */
    public void unsuscribePhrase(ISpeechListener listener, String phrase);

    /**
     * Suscribes a listener to the notifications feed, for any result
     * @param listener
     */
    public void suscribeAny(ISpeechListener listener);

    /**
     * Unsuscribes a listener of the notifications feed for all phrases and from the Any feed
     * @param listener
     */
    public void unsuscribeAll(ISpeechListener listener);
}
