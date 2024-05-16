package com.example.TicTacToc;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.Objects;

public class MusicPlayer {
    private MediaPlayer backgroundPlayer;
    private MediaPlayer soundPlayer;

    public void playMusic(String musicFile) {
        new Thread(() -> {
            Media sound = new Media(Objects.requireNonNull(getClass().getResource(musicFile)).toString());
            backgroundPlayer = new MediaPlayer(sound);
            backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundPlayer.play();
        }).start();
    }

    public void stopMusic() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
        }
    }

    public void playSound(String soundFile) {
        new Thread(()-> {
            stopMusic();
            Media sound = new Media(Objects.requireNonNull(getClass().getResource(soundFile)).toString());
            soundPlayer = new MediaPlayer(sound);
            soundPlayer.play();
            soundPlayer.setOnEndOfMedia(()->backgroundPlayer.play());

        }).start();

    }
}
