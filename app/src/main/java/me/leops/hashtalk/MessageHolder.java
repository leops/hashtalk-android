package me.leops.hashtalk;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;

import fr.tkeunebr.gravatar.Gravatar;

public class MessageHolder extends RecyclerView.ViewHolder {
    private final Context mContext;
    private final TextView mAuthor;
    private final TextView mTime;
    private final ImageView mAvatar;
    private final TextView mContent;
    private final RequestQueue mQueue;
    private final Map<String, String> mCache;

    public MessageHolder(View v, Context c, RequestQueue q, Map<String, String> cache) {
        super(v);

        mCache = cache;
        mQueue = q;
        mContext = c;
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
                    ((MainActivity)mContext).mSearchView.setQuery(h, false);
                }
            }, hm.start(), hm.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Matcher mm = Message.RX_MENTION.matcher(nContent);
        while (mm.find()) {
            final String m = mm.group(1);
            str.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    ((MainActivity)mContext).mSearchView.setQuery(m, false);
                }
            }, mm.start(), mm.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        mContent.setText(str);
    }

    private void setAvatar(String author, String uri) {
        if(mAuthor.getText().equals(author)) {
            Glide.with(mContext)
                .load(uri)
                .fitCenter()
                .placeholder(R.drawable.placeholder_icon)
                .dontAnimate()
                .into(mAvatar);
        }
    }

    public void setAuthor(final String nAuthor) {
        mAuthor.setText(nAuthor);

        if(mCache.containsKey(nAuthor)) {
            setAvatar(nAuthor, mCache.get(nAuthor));
        } else {
            final String url = Gravatar.init()
                .with(nAuthor)
                .size(75)
                .defaultImage(Gravatar.DefaultImage.IDENTICON)
                .size(Gravatar.MAX_IMAGE_SIZE_PIXEL)
                .build();
            String profile = "http://en.gravatar.com/" + nAuthor + ".json";

            mAvatar.setImageResource(R.drawable.placeholder_icon);

            StringRequest request = new StringRequest(Request.Method.GET, profile, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    String addr = url;
                    try {
                        addr = new JSONObject(response)
                                .getJSONArray("entry")
                                .getJSONObject(0)
                                .getJSONArray("photos")
                                .getJSONObject(0)
                                .getString("value");
                        mCache.put(nAuthor, addr);
                    } catch (JSONException e) {
                        //Log.e(TAG, e.toString());
                    } finally {
                        setAvatar(nAuthor, addr);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //Log.e(TAG, error.toString());
                    mCache.put(nAuthor, url);
                    setAvatar(nAuthor, url);
                }
            });

            mQueue.add(request);
        }
    }

    public void setTime(long time) {
        Timestamp stamp = new Timestamp(time);
        Date date = new Date(stamp.getTime());
        String dStr = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT).format(date);
        mTime.setText(dStr);
    }
}
