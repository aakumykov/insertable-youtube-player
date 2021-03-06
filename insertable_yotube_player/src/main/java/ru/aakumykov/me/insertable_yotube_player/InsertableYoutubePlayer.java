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

// TODO: Android Lifecycle

public class InsertableYoutubePlayer implements
        View.OnClickListener
{
    public enum PlayerType {
        VIDEO_PLAYER, AUDIO_PLAYER
    }


    public interface iPlayListener {
        void onPlay();
    }

    public interface iProgressListener {
        void onProgress(Float position, int percentage);
    }

    public interface iPauseListener {
        void onPause();
    }

    public interface iSeekListener {
        void onSeek(float position);
    }

    public interface iEndListener {
        void onEnd();
    }

    public interface iAppearListener {
        void onAppear();
    }

    public interface iReadyListener {
        void onReady(Float duration);
    }

    public interface iErrorListener {
        void onError(String errorMsg);
    }

    private iAppearListener mAppearListener;
    private iReadyListener mReadyListener;
    private iErrorListener mErrorListener;
    private iPlayListener mPlayListener;
    private iProgressListener mProgressListener;
    private iPauseListener mPauseListener;
    private iSeekListener mSeekListener;
    private iEndListener mEndListener;

    public InsertableYoutubePlayer addOnAppearListener(iAppearListener appearListener) {
        mAppearListener = appearListener;
        return this;
    }

    public InsertableYoutubePlayer addOnReadyListener(iReadyListener readyListener ) {
        mReadyListener = readyListener;
        return this;
    }

    public InsertableYoutubePlayer addOnErrorListener(iErrorListener errorListener) {
        mErrorListener = errorListener;
        return this;
    }

    public InsertableYoutubePlayer addOnPlayListener(iPlayListener playListener) {
        mPlayListener = playListener;
        return this;
    }

    public InsertableYoutubePlayer addOnProgressListener(iProgressListener progressListener) {
        mProgressListener = progressListener;
        return this;
    }

    public InsertableYoutubePlayer addOnPauseListener(iPauseListener pauseListener) {
        mPauseListener = pauseListener;
        return this;
    }

    public InsertableYoutubePlayer addOnSeekListener(iSeekListener seekListener) {
        mSeekListener = seekListener;
        return this;
    }

    public InsertableYoutubePlayer addOnEndListener(iEndListener endListener) {
        mEndListener = endListener;
        return this;
    }



    private final static String TAG = "InsertableYoutubePlayer";

    private Context context;
    private ViewGroup targetContainer;
    private PlayerType playerType;
    private Integer waitingMessageId;
    private int playIconId;
    private int pauseIconId;
    private int waitIconId;

    private TextView playerMsg;
    private ConstraintLayout audioPlayer;
    private ImageView playerControlButton;
    private SeekBar playerSeekBar;
    private TextView playerStatusBar;

    private PlayerConstants.PlayerState mMediaPlayerState;
    private boolean mSeekRequested = false;
    private boolean mReady = false;
    private float mVideoDuration = 0.0f;
    private float mVideoPosition = 0.0f;

    private String mVideoId;
    private Float mTimecode;

    private YouTubePlayerView mYouTubePlayerView;
    private YouTubePlayer mYouTubePlayer;
    private YouTubePlayerListener mYoutubePlayerListener = new YouTubePlayerListener() {

        @Override
        public void onReady(@NonNull YouTubePlayer youTubePlayer) {
            InsertableYoutubePlayer.this.mYouTubePlayer = youTubePlayer;

            hidePlayerMsg();

            if (null != mVideoId) {
                show(mVideoId, mTimecode, playerType);

                if (null != mAppearListener)
                    mAppearListener.onAppear();
            }
        }

        @Override
        public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState playerState) {
            mMediaPlayerState = playerState;
            if (isAudioPlayer())
                changePlayerControls(mMediaPlayerState);

            switch (playerState) {

                case PLAYING:
                    if (null != mPlayListener) mPlayListener.onPlay();
                    break;

                case PAUSED:
                    if (null != mPauseListener) mPauseListener.onPause();
                    break;

                case ENDED:
                    if (null != mEndListener) mEndListener.onEnd();
                    break;
            }
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
            if (null != mErrorListener)
                mErrorListener.onError(playerError.toString());
        }

        @Override
        public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float v) {
            mVideoPosition = v;
            moveSeekBar(v);
            if (null != mProgressListener)
                mProgressListener.onProgress(v, Math.round(100*(v/mVideoDuration)));
        }

        @Override
        public void onVideoDuration(@NonNull YouTubePlayer youTubePlayer, float v) {
            mVideoDuration = v;

            if (mSeekRequested) {
                mSeekRequested = false;
                if (null != mSeekListener)
                    mSeekListener.onSeek(v);
            }

            if (!mReady) {
                mReady = true;
                if (null != mReadyListener)
                    mReadyListener.onReady(v);
            }
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


    };


    // Конструкторы
    public InsertableYoutubePlayer(
            @NonNull Context context,
            @NonNull ViewGroup targetContainer,
            @Nullable Integer waitingMessageId,
            @Nullable Integer playIconId,
            @Nullable Integer pauseIconId,
            @Nullable Integer waitIconId
    )
    {
        Log.d(TAG, "MyYoutubePlayer");

        this.context = context;
        this.targetContainer = targetContainer;

        this.waitingMessageId =
                (null == waitingMessageId) ?
                R.string.waiting_message_id :
                waitingMessageId;

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

    public InsertableYoutubePlayer(
            @NonNull Context context,
            @NonNull ViewGroup targetContainer
    ) {
        this(
                context,
                targetContainer,
                null,
                null,
                null,
                null
        );
    }


    // View.OnClick
    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (R.id.audioPlayerControlButton == viewId) {
            playPauseMedia();
        }
    }


    // Внешние методы
    public void show(String videoId, @Nullable Float timecode, PlayerType playerType) {
        show(videoId, timecode, playerType, null);
    }

    public void show(String videoId, @Nullable Float timecode, PlayerType playerType, @Nullable Integer waitingMessageId) {
        this.mVideoId = videoId;
        this.mTimecode = (null == timecode) ? 0.0f : timecode;
        this.playerType = playerType;

        if (null != waitingMessageId)
            showPlayerMsg(waitingMessageId, true);

        if (null != mYouTubePlayer) {

            mYouTubePlayer.cueVideo(videoId, this.mTimecode);

            switch (playerType) {

                case AUDIO_PLAYER:
                    Utils.hide(mYouTubePlayerView);
                    Utils.show(audioPlayer);
                    break;

                case VIDEO_PLAYER:
                    Utils.hide(audioPlayer);
                    Utils.show(mYouTubePlayerView);
                    break;
            }
        }
    }

    public void remove() {
        mVideoId = null;

        if (null != mYouTubePlayer)
            mYouTubePlayer.pause();

        hidePlayerMsg();
        Utils.hide(mYouTubePlayerView);
        Utils.hide(audioPlayer);
    }

    public void pause() {
        if (null != mYouTubePlayer)
            mYouTubePlayer.pause();
    }

    public void play() {
        if (null != mYouTubePlayer)
            mYouTubePlayer.play();
    }

    public void play(float fromPosition) {
        if (null != mYouTubePlayer) {
            mYouTubePlayer.seekTo(fromPosition);
            mYouTubePlayer.play();
        }
    }

    public void release() {
        if (null != mYouTubePlayerView) {
            Utils.hide(mYouTubePlayerView);
            Utils.hide(audioPlayer);
            mYouTubePlayerView.release();
        }
    }

    public void convert2video() {
        playerType = PlayerType.VIDEO_PLAYER;
        Utils.show(mYouTubePlayerView);
        Utils.hide(audioPlayer);
    }

    public void convert2audio() {
        playerType = PlayerType.AUDIO_PLAYER;
        Utils.hide(mYouTubePlayerView);
        Utils.show(audioPlayer);
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public boolean hasMedia() {
        return !TextUtils.isEmpty(mVideoId);
    }

    public boolean wasPlay() {
        return PlayerConstants.PlayerState.PLAYING.equals(mMediaPlayerState);
    }

    public boolean isAudioPlayer() {
        return PlayerType.AUDIO_PLAYER.equals(playerType);
    }

    public boolean isVideoPlayer() {
        return PlayerType.VIDEO_PLAYER.equals(playerType);
    }

    public void seekTo(int positionPercentDecimal) {
        mSeekRequested = true;
        if (null != mYouTubePlayer)
            mYouTubePlayer.seekTo(mVideoDuration * positionPercentDecimal / 100);
    }

    public void seekTo(Float position) {
        if (null != mYouTubePlayer)
            mYouTubePlayer.seekTo(position);
    }

    public float getDuration() {
        return this.mVideoDuration;
    }

    public float getPosition() {
        return this.mVideoPosition;
    }


    // Внутренние методы
    private void preparePlayerLayout() {
        LayoutInflater layoutInflater = LayoutInflater.from(targetContainer.getContext());

        LinearLayout player_layout = (LinearLayout) layoutInflater.inflate(R.layout.insertable_youtube_player, null);
        playerMsg = player_layout.findViewById(R.id.playerMsg);

        mYouTubePlayerView = player_layout.findViewById(R.id.youTubePlayerView);

        audioPlayer = player_layout.findViewById(R.id.audioPlayer);
        playerControlButton = player_layout.findViewById(R.id.audioPlayerControlButton);
        playerSeekBar = player_layout.findViewById(R.id.audioPlayerSeekBar);
        playerStatusBar = player_layout.findViewById(R.id.audioPlayerStatusBar);

        player_layout.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        );

        if (0 != targetContainer.getChildCount())
            targetContainer.removeViewAt(0);

        targetContainer.addView(player_layout);

        Utils.show(targetContainer);
    }

    private void preparePlayer() {

        Utils.show(targetContainer);

        showPlayerMsg(waitingMessageId, true);

        if (null == mYouTubePlayer) {

            mYouTubePlayerView.initialize(mYoutubePlayerListener);

            mYouTubePlayerView.enableBackgroundPlayback(true);
        }

        playerControlButton.setOnClickListener(this);

        configureSeekBar();
    }

    private void configureSeekBar() {
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float newPosition = mVideoDuration * progress / 100;
                    if (null != mYouTubePlayer) {
                        mYouTubePlayer.seekTo(newPosition);
                        mYouTubePlayer.play();
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
        int progress = Math.round((currentPosition / mVideoDuration) * 100);
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
        if (PlayerConstants.PlayerState.PLAYING.equals(this.mMediaPlayerState)) {
            mYouTubePlayer.pause();
        } else {
            mYouTubePlayer.play();
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