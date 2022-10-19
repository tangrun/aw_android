package cn.wildfire.chat.kit.voip.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;
import org.webrtc.MediaStreamTrack;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;

import java.util.ArrayList;
import java.util.List;

public class VoipCallAdapter extends RecyclerView.Adapter<VoipCallItemViewHolder> {
    private static final String TAG = "VoipCallAdapter";

    public final List<UserInfo> userInfoList = new ArrayList<>();

    private UserInfo focusUser, me;

    public final RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;

    private FrameLayout focusContainer;

    public VoipCallAdapter setMe(UserInfo me) {
        this.me = me;
        return this;
    }

    public void setUserInfoList(List<UserInfo> list) {
        userInfoList.clear();
        userInfoList.addAll(list);
    }

    public void setFocusContainer(FrameLayout focusContainer) {
        this.focusContainer = focusContainer;
        if (focusContainer != null) {
            focusContainer.setOnClickListener(v -> {
                if (focusUser != null) {
                    focusContainer.removeAllViews();
                    focusUser = null;
                }
            });
        }
    }

    @NonNull
    @Override
    public VoipCallItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VoipCallItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.av_multi_call_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VoipCallItemViewHolder holder, int position) {
        holder.userInfo = userInfoList.get(position);
        UserInfo userInfo = holder.userInfo;

        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null) {
            FrameLayout container;
            if (userEquals(focusUser, userInfo)) {
                container = focusContainer;
            } else {
                container = holder.videoContainer;
            }

            if (userEquals(me, userInfo)) {
                session.setupLocalVideoView(container, scalingType);
            } else {
                session.setupRemoteVideoView(userInfo.uid, container, scalingType);
            }
        }

        String statusText = userInfo.displayName;
        if (session != null) {
            PeerConnectionClient client = session.getClient(userInfo.uid);
            if (client != null && client.tri() != null) {
//
//                if (client.tri().getReceivers() != null)
//                    for (RtpReceiver receiver : client.tri().getReceivers()) {
//                        MediaStreamTrack track = receiver.track();
//                        Log.d(TAG, "RtpReceiver: " + userInfo.displayName + " " + track.kind() + " " + track.enabled() + " " + track.state());
//                    }
//                if (client.tri().getSenders() != null)
//                    for (RtpSender sender : client.tri().getSenders()) {
//                        MediaStreamTrack track = sender.track();
//                        Log.d(TAG, "RtpSender: " + userInfo.displayName + " " + track.kind() + " " + track.enabled() + " " + track.state());
//                    }
            }
        }
        holder.tvStatus.setText(statusText);


        GlideApp.with(holder.ivPortrait).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(holder.ivPortrait);
        holder.itemView.setOnClickListener(v -> {

        });


    }

    private boolean userEquals(UserInfo u1, UserInfo u2) {
        if (u1 == u2) return true;
        if (u1 != null && u2 != null) {
            if (u1.uid != null)
                return u1.uid.equals(u2.uid);
        }
        return false;
    }

    private boolean stringEquals(String s1, String s2) {
        if (s1 != null) return s1.equals(s2);
        else return s2 == null;
    }

    @Override
    public int getItemCount() {
        return userInfoList.size();
    }

    public boolean containsPeer(String peerId) {
        for (UserInfo userInfo : userInfoList) {
            if (userInfo.uid != null) {
                if (userInfo.uid.equals(peerId)) return true;
            }
        }
        return false;
    }

    public void peerJoined(UserInfo userInfo) {
        if (containsPeer(userInfo.uid)) {
            return;
        }
        userInfoList.add(userInfo);
        notifyItemInserted(userInfoList.size() - 1);
    }

    public void peerLeft(String userId) {
        for (int i = 0; i < userInfoList.size(); i++) {
            UserInfo userInfo = userInfoList.get(i);
            if (stringEquals(userInfo.uid, userId)) {
                userInfoList.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void peerNotify(String userId) {
        for (int i = 0; i < userInfoList.size(); i++) {
            UserInfo userInfo = userInfoList.get(i);
            if (stringEquals(userInfo.uid, userId)) {
                notifyItemChanged(i);
                return;
            }
        }
    }
}
