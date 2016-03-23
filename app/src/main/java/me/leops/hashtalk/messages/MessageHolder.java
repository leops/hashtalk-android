package me.leops.hashtalk.messages;

import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;

import me.leops.hashtalk.R;
import me.leops.hashtalk.activity.MainActivity;

public class MessageHolder extends RecyclerView.ViewHolder {
    private MainActivity mActivity;
    private final TextView mAuthor;
    private final TextView mTime;
    private final ImageView mAvatar;
    private final TextView mContent;

    public MessageHolder(View v, MainActivity activity) {
        super(v);
        mActivity = activity;

        mAuthor = (TextView) v.findViewById(R.id.author);
        mTime = (TextView) v.findViewById(R.id.time);
        mAvatar = (ImageView) v.findViewById(R.id.avatar);
        mContent = (TextView) v.findViewById(R.id.content);

        mContent.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setContent(String nContent) {
        SpannableString str = new SpannableString(nContent);

        Matcher hm = Message.RX_HASHTAG.matcher(nContent);
        while (hm.find()) {
            final String h = hm.group(1);
            str.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    mActivity.setQuery(h);
                }
            }, hm.start(), hm.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Matcher mm = Message.RX_MENTION.matcher(nContent);
        while (mm.find()) {
            final String m = mm.group(1);
            str.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    mActivity.addMention(m);
                }
            }, mm.start(), mm.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        mContent.setText(str);
    }

    public void setAuthor(final String nAuthor) {
        final int size = Math.max(mAvatar.getHeight(), mAvatar.getWidth());
        mActivity.getFirebase("users").child(nAuthor).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String name = dataSnapshot.child("displayName").getValue(String.class);
                String avatar = dataSnapshot.child("avatar").getValue(String.class);

                View.OnClickListener mention = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mActivity.addMention(name);
                    }
                };

                mAvatar.setOnClickListener(mention);
                mAuthor.setOnClickListener(mention);
                mAuthor.setText(name);

                Glide.with(mActivity)
                    .load(avatar + "?d=identicon&s=" + size)
                    .fitCenter()
                    .placeholder(R.drawable.placeholder_icon)
                    .dontAnimate()
                    .into(mAvatar);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                //
            }
        });
    }

    public void setTime(long time) {
        Timestamp stamp = new Timestamp(time);
        Date date = new Date(stamp.getTime());
        String dStr = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT).format(date);
        mTime.setText(dStr);
    }

    public void setMessage(Message message) {
        setContent(message.getContent());
        setAuthor(message.getAuthor());
        setTime(message.getTime());
    }
}
