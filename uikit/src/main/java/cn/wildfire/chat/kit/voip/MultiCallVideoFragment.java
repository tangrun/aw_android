/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.voip.adapter.VoipCallAdapter;
import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MultiCallVideoFragment extends Fragment implements AVEngineKit.CallSessionCallback {
    @BindView(R2.id.rootView)
    ConstraintLayout rootLinearLayout;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;
    @BindView(R2.id.fullScreenDurationTextView)
    TextView fullScreenDurationTextView;
    @BindView(R2.id.rv_peer)
    RecyclerView rvPeer;
    @BindView(R2.id.focusVideoContainerFrameLayout)
    FrameLayout focusVideoContainerFrameLayout;
    @BindView(R2.id.muteImageView)
    ImageView muteImageView;
    @BindView(R2.id.videoImageView)
    ImageView videoImageView;
    @BindView(R2.id.topActions)
    View topActionContainer;
    @BindView(R2.id.callControlActions)
    View callControlActionContainer;

    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;

    private VoipCallItem focusVoipCallItem;

    VoipCallAdapter adapter;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

    public static final String TAG = "MultiCallVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_video_outgoing_connected, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private AVEngineKit getEngineKit() {
        return AVEngineKit.Instance();
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }

        initParticipantsView(session);

        if (session.getState() == AVEngineKit.CallState.Outgoing) {
            session.startPreview();
        } else if (session.getState() == AVEngineKit.CallState.Connected) {
        }

        handler.post(updateCallDurationRunnable);
        updateParticipantStatus(session);

        muteImageView.setSelected(session.isAudioMuted());
        videoImageView.setSelected(session.videoMuted);

        topActionContainer.setVisibility(View.VISIBLE);
        callControlActionContainer.setVisibility(View.VISIBLE);
        fullScreenDurationTextView.setVisibility(View.GONE);
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {
        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        participants = session.getParticipantIds();


        adapter = new VoipCallAdapter();
        adapter.setFocusContainer(focusVideoContainerFrameLayout);
        List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);
        participantUserInfos.add(me);
        adapter.setUserInfoList(participantUserInfos);
        adapter.setMe(me);
        rvPeer.setAdapter(adapter);
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
//        int count = participantGridView.getChildCount();
//        String meUid = userViewModel.getUserId();
//        for (int i = 0; i < count; i++) {
//            View view = participantGridView.getChildAt(i);
//            String userId = (String) view.getTag();
//            if (meUid.equals(userId)) {
//                ((VoipCallItem) view).getStatusTextView().setVisibility(View.GONE);
//            } else {
//                PeerConnectionClient client = session.getClient(userId);
//                if (client.state == AVEngineKit.CallState.Connected) {
//                    ((VoipCallItem) view).getStatusTextView().setVisibility(View.GONE);
//                } else if (client.videoMuted) {
//                    ((VoipCallItem) view).getStatusTextView().setText("关闭摄像头");
//                    ((VoipCallItem) view).getStatusTextView().setVisibility(View.VISIBLE);
//                }
//            }
//        }
    }

    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.finish();
        }
    }

    @OnClick(R2.id.addParticipantImageView)
    void addParticipant() {
        ((MultiCallActivity) getActivity()).addParticipant(AVEngineKit.MAX_VIDEO_PARTICIPANT_COUNT - participants.size() - 1);
    }

    @OnClick(R2.id.muteImageView)
    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            boolean toMute = !session.isAudioMuted();
            session.muteAudio(toMute);
            muteImageView.setSelected(toMute);
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    void switchCamera() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && !session.isScreenSharing() && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
        }
    }

    @OnClick(R2.id.videoImageView)
    void video() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected && !session.isScreenSharing()) {
            boolean toMute = !session.videoMuted;
            session.muteVideo(toMute);
            videoImageView.setSelected(toMute);
        }
    }

    @OnClick(R2.id.hangupImageView)
    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            session.endCall();
        }
    }


    @OnClick(R2.id.shareScreenImageView)
    void shareScreen() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() != AVEngineKit.CallState.Connected) {
            return;
        }
        if (session.videoMuted) {
            // TODO 目前关闭摄像头之后，不支持屏幕共享
            return;
        }
        if (!AVEngineKit.isSupportConference()) {
            Toast.makeText(getActivity(), "当前版本不支持屏幕共享", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!session.isScreenSharing()) {
            ((VoipBaseActivity) getActivity()).startScreenShare();
        } else {
            ((VoipBaseActivity) getActivity()).stopScreenShare();
        }
    }

    // hangup 触发
    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        Log.d(TAG, "didCallEndWithReason() called with: callEndReason = [" + callEndReason + "]");
        // do nothing
        getActivity().finish();
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        Log.d(TAG, "didChangeState() called with: callState = [" + callState + "]");
        AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
        if (callState == AVEngineKit.CallState.Connected) {
            updateParticipantStatus(callSession);
        } else if (callState == AVEngineKit.CallState.Idle) {
            if (getActivity() == null) {
                return;
            }
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    /**
     * 人进入
     * @param userId
     * @param screenSharing
     */
    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        Log.d(TAG, "didParticipantJoined() called with: userId = [" + userId + "], screenSharing = [" + screenSharing + "]");
        if (adapter.containsPeer(userId)) {
            return;
        }

        UserInfo userInfo = userViewModel.getUserInfo(userId, false);
        adapter.peerJoined(userInfo);
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {
        Log.d(TAG, "didParticipantConnected() called with: userId = [" + userId + "], screenSharing = [" + screenSharing + "]");
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason, boolean screenSharing) {
        Log.d(TAG, "didParticipantLeft() called with: userId = [" + userId + "], callEndReason = [" + callEndReason + "], screenSharing = [" + screenSharing + "]");
        adapter.peerLeft(userId);

        Toast.makeText(getActivity(), ChatManager.Instance().getUserDisplayName(userId) + "离开了通话", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didChangeMode(boolean audioOnly) {
        Log.d(TAG, "didChangeMode() called with: audioOnly = [" + audioOnly + "]");
    }

    @Override
    public void didCreateLocalVideoTrack() {
        Log.d(TAG, "didCreateLocalVideoTrack() called");
        // 本地 视频 开启了
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        Log.d(TAG, "didReceiveRemoteVideoTrack() called with: userId = [" + userId + "], screenSharing = [" + screenSharing + "]");
        adapter.peerNotify(userId);
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        Log.d(TAG, "didRemoveRemoteVideoTrack() called with: userId = [" + userId + "]");
        adapter.peerNotify(userId);
        // 对方关闭视频
//        VoipCallItem item = rootLinearLayout.findViewWithTag(userId);
//        if (item != null) {
//            item.getStatusTextView().setText("关闭摄像头");
//            item.getStatusTextView().setVisibility(View.VISIBLE);
//        }
    }

    @Override
    public void didError(String reason) {
        Log.d(TAG, "didError() called with: reason = [" + reason + "]");
        Toast.makeText(getActivity(), "发生错误" + reason, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {
        Log.d(TAG, "didGetStats() called with: statsReports = [" + statsReports + "]");
    }

    @Override
    public void didVideoMuted(String userId, boolean videoMuted) {
        Log.d(TAG, "didVideoMuted() called with: userId = [" + userId + "], videoMuted = [" + videoMuted + "]");
        adapter.peerNotify(userId);
//        if (videoMuted) {
//            didRemoveRemoteVideoTrack(userId);
//        }
    }


    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, "didReportAudioVolume() called with: userId = [" + userId + "], volume = [" + volume + "]");
//        VoipCallItem voipCallItem = getUserVoipCallItem(userId);
//        if (voipCallItem != null) {
//            if (volume > 1000) {
//                voipCallItem.getStatusTextView().setVisibility(View.VISIBLE);
//                voipCallItem.getStatusTextView().setText("正在说话");
//            } else {
//                voipCallItem.getStatusTextView().setVisibility(View.GONE);
//                voipCallItem.getStatusTextView().setText("");
//            }
//        }
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {
        Log.d(TAG, "didAudioDeviceChanged() called with: device = [" + device + "]");
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(updateCallDurationRunnable);
    }

//    private View.OnClickListener clickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            String userId = (String) v.getTag();
//            AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
//            if (session == null
//                    || session.getState() != AVEngineKit.CallState.Connected
//                    || (userId.equals(ChatManager.Instance().getUserId()) && session.isScreenSharing())) {
//                return;
//            }
//
//            if (focusVoipCallItem != null) {
//                // 放大时 点击恢复宫格显示
//                focusVideoContainerFrameLayout.removeView(focusVoipCallItem);
//                focusVideoContainerFrameLayout.endViewTransition(focusVoipCallItem);
//                DisplayMetrics dm = getResources().getDisplayMetrics();
//                int with = dm.widthPixels;
//                participantGridView.addView(focusVoipCallItem, clickedIndex, new FrameLayout.LayoutParams(with / 3, with / 3));
//            }else{
//                // 正常时 点击放大显示
//            }
//
//
//            if (!userId.equals(((VoipBaseActivity) getActivity()).getFocusVideoUserId())) {
//                // 正常时 点击放大显示
//                VoipCallItem clickedVoipCallItem = (VoipCallItem) v;
//                int clickedIndex = participantGridView.indexOfChild(v);
//                participantGridView.removeView(clickedVoipCallItem);
//                participantGridView.endViewTransition(clickedVoipCallItem);
//
//
//                focusVideoContainerFrameLayout.addView(clickedVoipCallItem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//                focusVoipCallItem = clickedVoipCallItem;
//                ((VoipBaseActivity) getActivity()).setFocusVideoUserId(userId);
//            }else {
//                // 放大时 点击恢复宫格显示
//                if (focusVoipCallItem != null) {
//                    focusVideoContainerFrameLayout.removeView(focusVoipCallItem);
//                    focusVideoContainerFrameLayout.endViewTransition(focusVoipCallItem);
//                    DisplayMetrics dm = getResources().getDisplayMetrics();
//                    int with = dm.widthPixels;
//                    participantGridView.addView(focusVoipCallItem, clickedIndex, new FrameLayout.LayoutParams(with / 3, with / 3));
//                }
//            }
//        }
//    };

    private final Handler handler = new Handler();
    private final Runnable updateCallDurationRunnable = new Runnable() {
        @Override
        public void run() {
            AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
            if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
                long s = System.currentTimeMillis() - session.getConnectedTime();
                s = s / 1000;
                String text;
                if (s > 3600) {
                    text = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
                } else {
                    text = String.format("%02d:%02d", s / 60, (s % 60));
                }
                durationTextView.setText(text);
                fullScreenDurationTextView.setText(text);
            }
            handler.postDelayed(updateCallDurationRunnable, 1000);
        }
    };

}
