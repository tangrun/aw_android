package cn.wildfire.chat.kit.voip.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.voip.ZoomableFrameLayout;
import cn.wildfirechat.model.UserInfo;

public class VoipCallItemViewHolder extends RecyclerView.ViewHolder {

    public UserInfo userInfo;

    public ImageView ivPortrait;
    public ZoomableFrameLayout videoContainer;
    public TextView tvStatus;

    public VoipCallItemViewHolder(@NonNull View itemView) {
        super(itemView);
        ivPortrait = itemView.findViewById(R.id.portraitImageView);
        videoContainer = itemView.findViewById(R.id.videoContainer);
        tvStatus = itemView.findViewById(R.id.statusTextView);
    }

    public void onBind(UserInfo userInfo) {
        if (this.userInfo ==null){
            this.userInfo = userInfo;
        }
    }
}
