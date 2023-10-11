package com.mytechia.robobo.framework.sound;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.mytechia.robobo.framework.RoboboManager;
import com.mytechia.robobo.framework.exception.ModuleNotFoundException;
import com.mytechia.robobo.framework.hri.sound.emotionSound.IEmotionSoundModule;
import com.mytechia.robobo.framework.hri.sound.noiseMetering.INoiseMeterModule;
import com.mytechia.robobo.framework.hri.sound.noteDetection.INoteDetectionModule;
import com.mytechia.robobo.framework.hri.sound.pitchDetection.IPitchDetectionModule;
import com.mytechia.robobo.framework.hri.sound.soundDispatcherModule.ISoundDispatcherModule;
import com.mytechia.robobo.framework.service.RoboboServiceHelper;

public class TestActivity extends AppCompatActivity {

    private IEmotionSoundModule soundModule;
    private ISoundDispatcherModule dispatcherModule;
    private IPitchDetectionModule pitchDetectionModule;
    private INoteDetectionModule noteDetectionModule;
    private INoiseMeterModule noiseMeterModule;
    private RoboboManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        RoboboServiceHelper serviceHelper = new RoboboServiceHelper(this, new RoboboServiceHelper.Listener() {
            @Override
            public void onRoboboManagerStarted(RoboboManager roboboManager) {
                manager = roboboManager;
                startapp();


            }

            @Override
            public void onError(Throwable ex) {

            }
        });
        Bundle options = new Bundle();
        serviceHelper.bindRoboboService(options);

    }


    public void startapp(){
        try {

                dispatcherModule = manager.getModuleInstance(ISoundDispatcherModule.class);
                pitchDetectionModule = manager.getModuleInstance(IPitchDetectionModule.class);
//                noteDetectionModule = manager.getModuleInstance(INoteDetectionModule.class);
            noiseMeterModule = manager.getModuleInstance(INoiseMeterModule.class);
                dispatcherModule.runDispatcher();
            Log.d("TEST","TEST");



        } catch (ModuleNotFoundException e) {
            e.printStackTrace();
        }


    }
}
