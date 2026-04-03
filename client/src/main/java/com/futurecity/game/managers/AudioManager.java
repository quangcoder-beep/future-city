package com.futurecity.game.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Modern Audio Manager for Future City.
 * Handles SFX throttling, safe loading, and volume persistence.
 */
public class AudioManager {
    private static AudioManager instance;
    private final MyAssetManager assetManager;
    private final Preferences prefs;

    private float masterVolume = 1.0f;
    private float sfxVolume = 0.8f;
    private float musicVolume = 0.5f;

    private Music currentMusic;
    private final ObjectMap<String, Long> lastPlayTimes = new ObjectMap<>();
    private long lastGlobalUiPlayTime = 0;
    private static final long SFX_COOLDOWN = 120; // Per-path cooldown
    private static final long GLOBAL_UI_COOLDOWN = 80; // Global UI cooldown

    private AudioManager(MyAssetManager assetManager) {
        this.assetManager = assetManager;
        this.prefs = Gdx.app.getPreferences("FutureCityAudio");
        loadPreferences();
    }

    public static AudioManager getInstance(MyAssetManager assetManager) {
        if (instance == null) {
            instance = new AudioManager(assetManager);
        }
        return instance;
    }

    private void loadPreferences() {
        masterVolume = prefs.getFloat("master", 1.0f);
        sfxVolume = prefs.getFloat("sfx", 0.8f);
        musicVolume = prefs.getFloat("music", 0.5f);
    }

    public void savePreferences() {
        prefs.putFloat("master", masterVolume);
        prefs.putFloat("sfx", sfxVolume);
        prefs.putFloat("music", musicVolume);
        prefs.flush();
    }

    /**
     * Attaches click and hover sounds to any Actor.
     */
    public void attachDefaultTo(com.badlogic.gdx.scenes.scene2d.Actor actor) {
        attachTo(actor, "sounds/ui/click.ogg", "sounds/ui/hover.ogg");
    }

    public void attachTo(com.badlogic.gdx.scenes.scene2d.Actor actor, final String clickSfx, final String hoverSfx) {
        if (clickSfx != null) {
            actor.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    playSfx(clickSfx);
                }
            });
        }
        if (hoverSfx != null) {
            actor.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    if (pointer == -1) playSfx(hoverSfx); // pointer == -1 means mouse hover (not click)
                }
            });
        }
    }

    /**
     * Plays a sound effect with throttling and safety checks.
     */
    public void playSfx(String path) {
        // Default UI play: uses global cooldown, 5% pitch variation, and standard sfx volume
        playSfx(path, sfxVolume, 0.05f, true);
    }

    /**
     * Specialized play method with volume, pitch variation and global throttling control.
     */
    public void playSfx(String path, float volume, float pitchVar, boolean useGlobalCooldown) {
        if (!assetManager.manager.isLoaded(path)) return;

        long now = TimeUtils.millis();
        
        // 1. Global UI Check (Excluded for non-UI like footsteps)
        if (useGlobalCooldown) {
            if (now - lastGlobalUiPlayTime < GLOBAL_UI_COOLDOWN) return;
        }

        // 2. Per-path Check
        long lastPlay = lastPlayTimes.get(path, 0L);
        if (now - lastPlay < SFX_COOLDOWN) return;

        Sound sound = assetManager.manager.get(path, Sound.class);
        if (sound != null) {
            float pitch = 1.0f + ((float) Math.random() * 2 - 1) * pitchVar;
            sound.play(masterVolume * volume, pitch, 0); // pan = 0 (center)
            
            lastPlayTimes.put(path, now);
            if (useGlobalCooldown) lastGlobalUiPlayTime = now;
        }
    }

    /**
     * Plays background music with safety checks.
     */
    public void playMusic(String path, boolean loop) {
        if (!assetManager.manager.isLoaded(path)) return;

        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.stop();
        }

        currentMusic = assetManager.manager.get(path, Music.class);
        if (currentMusic != null) {
            currentMusic.setLooping(loop);
            currentMusic.setVolume(masterVolume * musicVolume);
            currentMusic.play();
        }
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
    }

    public void pauseAll() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    public void resumeAll() {
        if (currentMusic != null) {
            currentMusic.play();
        }
    }

    // Getters and Setters
    public void setMasterVolume(float vol) {
        this.masterVolume = vol;
        if (currentMusic != null) currentMusic.setVolume(masterVolume * musicVolume);
        savePreferences();
    }

    public void setSfxVolume(float vol) {
        this.sfxVolume = vol;
        savePreferences();
    }

    public void setMusicVolume(float vol) {
        this.musicVolume = vol;
        if (currentMusic != null) currentMusic.setVolume(masterVolume * musicVolume);
        savePreferences();
    }

    public float getMasterVolume() { return masterVolume; }
    public float getSfxVolume() { return sfxVolume; }
    public float getMusicVolume() { return musicVolume; }
}
