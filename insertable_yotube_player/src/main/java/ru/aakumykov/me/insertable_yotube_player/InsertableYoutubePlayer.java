package ru.aakumykov.me.insertable_yotube_player;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.utils.TimeUtilities;

public class InsertableYoutubePlayer implements
        View.OnClickListener
{
    public interface iMyYoutubePlayerCallbacks {
        void onMediaAdded();
    }

    public enum PlayerType {
        VIDEO_PLAYER, AUDIO_PLAYER
    }


    private final static String TAG = "MyYoutubePlayer";
    private PlayerType playerType;
    private int waitingMessageId;
    private int playIconId;
    private int pauseIconId;
    private int waitIconId;
    private Context context;
    private ViewGroup targetContainer;

    private TextView playerMsg;
    private ConstraintLayout audioPlayer;
    private ImageView playerControlButton;
    private SeekBar playerSeekBar;
    private TextView playerStatusBar;

    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;

    private float videoDuration = 0f;
    private String videoId;
    private Float timecode;
    private PlayerConstants.PlayerState mediaPlayerState;


    public InsertableYoutubePlayer(
            @NonNull Context context,
            @NonNull ViewGroup targetContainer,
            @NonNull Integer waitingMessageId,
            @Nullable Integer playIconId,
            @Nullable Integer pauseIconId,
            @Nullable Integer waitIconId
    )
    {
        Log.d(TAG, "MyYoutubePlayer");

        this.context = context;
        this.targetContainer = targetContainer;
        this.waitingMessageId = waitingMessageId;

        this.playIconId = (null == playIconId) ? R.drawable.ic_player_play : playIconId;
        this.pauseIconId = (null == pauseIconId) ? R.drawable.ic_player_pause : pauseIconId;
        this.waitIconId = (null == waitIconId) ? R.drawable.ic_player_wait : waitIconId;

        preparePlayerLayout();

        preparePlayer();
    }


    public InsertableYoutubePlayer(
            @NonNull Context context,
            @NonNull ViewGroup targetContainer,
            int waitingMessageId
    )
    {
        this(
            context,
            targetContainer,
            waitingMessageId,
            null,
            null,
            null
        );
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (R.id.playerControlButton == viewId) {
            playPauseMedia();
        }
    }


    // Внешние методы
    public void show(String videoId, PlayerType playerType) {
        show(videoId, null, playerType);
    }

    public void show(String videoId, @Nullable Float timecode, PlayerType playerType) {
        this.videoId = videoId;
        this.timecode = (null == timecode) ? 0.0f : timecode;
        this.playerType = playerType;

        if (null != youTubePlayer) {

            youTubePlayer.cueVideo(videoId, this.timecode);

            switch (playerType) {

                case AUDIO_PLAYER:
                    Utils.hide(youTubePlayerView);
                    Utils.show(audioPlayer);
                    break;

                case VIDEO_PLAYER:
                    Utils.hide(audioPlayer);
                    Utils.show(youTubePlayerView);
                    break;
            }
        }
    }

    public void remove() {
        videoId = null;

        if (null != youTubePlayer)
            youTubePlayer.pause();

        hidePlayerMsg();
        Utils.hide(youTubePlayerView);
        Utils.hide(audioPlayer);
    }

    public void pause() {
        if (null != youTubePlayer)
            youTubePlayer.pause();
    }

    public void play() {
        if (null != youTubePlayer)
            youTubePlayer.play();
    }

    public void release() {
        if (null != youTubePlayerView) {
            Utils.hide(youTubePlayerView);
            Utils.hide(audioPlayer);
            youTubePlayerView.release();
        }
    }

    public void convert2video() {
        playerType = PlayerType.VIDEO_PLAYER;
        Utils.show(youTubePlayerView);
        Utils.hide(audioPlayer);
    }

    public void convert2audio() {
        playerType = PlayerType.AUDIO_PLAYER;
        Utils.hide(youTubePlayerView);
        Utils.show(audioPlayer);
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public boolean hasMedia() {
        return !TextUtils.isEmpty(videoId);
    }

    public boolean wasPlay() {
        return PlayerConstants.PlayerState.PLAYING.equals(mediaPlayerState);
    }

    public boolean isAudioPlayer() {
        return PlayerType.AUDIO_PLAYER.equals(playerType);
    }

    public boolean isVideoPlayer() {
        return PlayerType.VIDEO_PLAYER.equals(playerType);
    }


    // Внутренние методы
    private void preparePlayerLayout() {
        LayoutInflater layoutInflater = LayoutInflater.from(targetContainer.getContext());

        LinearLayout player_layout = (LinearLayout) layoutInflater.inflate(R.layout.insertable_youtube_player, null);
        playerMsg = player_layout.findViewById(R.id.playerMsg);

        youTubePlayerView = player_layout.findViewById(R.id.youTubePlayerView);

        audioPlayer = player_layout.findViewById(R.id.audioPlayer);
        playerControlButton = player_layout.findViewById(R.id.playerControlButton);
        playerSeekBar = player_layout.findViewById(R.id.playerSeekBar);
        playerStatusBar = player_layout.findViewById(R.id.playerStatusBar);

        if (0 != targetContainer.getChildCount())
            targetContainer.removeViewAt(0);

        targetContainer.addView(player_layout);

        Utils.show(targetContainer);
    }

    private void preparePlayer() {

        Utils.show(targetContainer);

        showPlayerMsg(waitingMessageId, true);

        if (null == youTubePlayer) {

            youTubePlayerView.initialize(new YouTubePlayerListener() {

                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    InsertableYoutubePlayer.this.youTubePlayer = youTubePlayer;

                    hidePlayerMsg();

                    if (null != videoId)
                        show(videoId, playerType);
                }

                @Override
                public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState playerState) {
                    mediaPlayerState = playerState;
                    //showPlayerMsg(state);
                    if (isAudioPlayer())
                        changePlayerControls(mediaPlayerState);
                }

                @Override
                public void onPlaybackQualityChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlaybackQuality playbackQuality) {

                }

                @Override
                public void onPlaybackRateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlaybackRate playbackRate) {

                }

                @Override
                public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError playerError) {
                    showPlayerMsg(String.valueOf(playerError), false);
                }

                @Override
                public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float v) {
                    moveSeekBar(v);
                }

                @Override
                public void onVideoDuration(@NonNull YouTubePlayer youTubePlayer, float v) {
                    videoDuration = v;
                }

                @Override
                public void onVideoLoadedFraction(@NonNull YouTubePlayer youTubePlayer, float v) {

                }

                @Override
                public void onVideoId(@NonNull YouTubePlayer youTubePlayer, @NonNull String s) {

                }

                @Override
                public void onApiChange(@NonNull YouTubePlayer youTubePlayer) {

                }
            });

            youTubePlayerView.enableBackgroundPlayback(true);
        }

        playerControlButton.setOnClickListener(this);

        configureSeekBar();
    }

    private void configureSeekBar() {
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float newPosition = videoDuration * progress / 100;
                    if (null != youTubePlayer) {
                        youTubePlayer.seekTo(newPosition);
                        youTubePlayer.play();
//                        youTubePlayer.loadVideo(videoId, newPosition);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void moveSeekBar(float currentPosition) {
        int progress = Math.round((currentPosition / videoDuration) * 100);
        playerSeekBar.setProgress(progress);
        playerStatusBar.setText(TimeUtilities.formatTime(currentPosition));
    }

    private <T> void showPlayerMsg(T arg, boolean withAnimation) {

        if (withAnimation) {
            int duration = 1000;

            ObjectAnimator backgroundAnimator = ObjectAnimator.ofObject(
                    playerMsg,
                    "backgroundColor",
                    new ArgbEvaluator(),
                    ContextCompat.getColor(context, R.color.my_youtube_player_msg_animation_start),
                    ContextCompat.getColor(context, R.color.my_youtube_player_msg_animation_end)
            );

            backgroundAnimator.setRepeatCount(ValueAnimator.INFINITE);
            backgroundAnimator.setRepeatMode(ValueAnimator.REVERSE);
            backgroundAnimator.setDuration(duration);

            ObjectAnimator textAnimator = ObjectAnimator.ofObject(
                    playerMsg,
                    "textColor",
                    new ArgbEvaluator(),
                    ContextCompat.getColor(context, R.color.my_youtube_player_msg_animation_end),
                    ContextCompat.getColor(context, R.color.my_youtube_player_msg_animation_start)
            );

            textAnimator.setRepeatCount(ValueAnimator.INFINITE);
            textAnimator.setRepeatMode(ValueAnimator.REVERSE);
            textAnimator.setDuration(duration);

            backgroundAnimator.start();
            textAnimator.start();
        }

        String msg = "";

        if (arg instanceof Integer) {
            int msgId = (Integer)arg;
            msg = context.getResources().getString(msgId);
        } else {
            msg = String.valueOf(arg);
        }

        playerMsg.setText(msg);
        Utils.show(playerMsg);
    }

    private void hidePlayerMsg() {
        playerMsg.clearAnimation();
        Utils.hide(playerMsg);
    }

    private void playPauseMedia() {
        if (PlayerConstants.PlayerState.PLAYING.equals(this.mediaPlayerState)) {
            youTubePlayer.pause();
        } else {
            youTubePlayer.play();
        }
    }

    private void changePlayerControls(PlayerConstants.PlayerState state) {
        switch (state) {

            case VIDEO_CUED:
                showPlayButton();
                break;

            case PLAYING:
                showPauseButton();
                break;

            case PAUSED:
                showPlayButton();
                break;

            case BUFFERING:
                showWatingButton();
                break;

            default:
                break;
        }
    }

    private void showPlayButton() {
        Drawable icon = context.getResources().getDrawable(playIconId);
        playerControlButton.setImageDrawable(icon);
    }

    private void showPauseButton() {
        Drawable icon = context.getResources().getDrawable(pauseIconId);
        playerControlButton.setImageDrawable(icon);
    }

    private void showWatingButton() {
        Drawable icon = context.getResources().getDrawable(waitIconId);
        playerControlButton.setImageDrawable(icon);
    }

}