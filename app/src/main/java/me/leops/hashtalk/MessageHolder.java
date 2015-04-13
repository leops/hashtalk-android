package me.leops.hashtalk;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.tkeunebr.gravatar.Gravatar;

public class MessageHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "MessageHolder";
    public static final Pattern hashtag = Pattern.compile("\\#(\\w+)");
    public static final Pattern mention = Pattern.compile("\\@(\\w+)");

    private final Context mContext;
    private final TextView mAuthor;
    private final TextView mTime;
    private final SimpleDraweeView mAvatar;
    private final TextView mContent;
    private final RequestQueue mQueue;
    private final Map<String, Uri> mCache;

    public MessageHolder(View v, Context c, RequestQueue q, Map<String, Uri> cache) {
        super(v);

        mCache = cache;
        mQueue = q;
        mContext = c;
        mAuthor = (TextView) v.findViewById(R.id.author);
        mTime = (TextView) v.findViewById(R.id.time);
        mAvatar = (SimpleDraweeView) v.findViewById(R.id.avatar);
        mContent = (TextView) v.findViewById(R.id.content);

        mContent.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setContent(String nContent) {
        SpannableString str = new SpannableString(nContent);

        Matcher hm = hashtag.matcher(nContent);
        while (hm.find()) {
            final String h = hm.group(1);
            str.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    ((MainActivity)mContext).mSearchView.setQuery(h, false);
                }
            }, hm.start(), hm.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Matcher mm = mention.matcher(nContent);
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

    public void loadImg(final String author, String url, boolean cache) {
        if(mCache.containsKey(author)) {
            mAvatar.setImageURI(mCache.get(author));
        } else {
            Uri uri = Uri.parse(url);
            mAvatar.setImageURI(uri);
            if(cache) mCache.put(author, uri);
        }
    }

    public void loadImg(final String author, String url) {
        loadImg(author, url, true);
    }

    public void setAuthor(final String nAuthor) {
        mAuthor.setText(nAuthor);

        if(mCache.containsKey(nAuthor)) {
            loadImg(nAuthor, "");
        } else {
            final String url = Gravatar.init().with(nAuthor).size(75).defaultImage(Gravatar.DefaultImage.IDENTICON).size(Gravatar.MAX_IMAGE_SIZE_PIXEL).build();
            String profile = "http://en.gravatar.com/" + nAuthor + ".json";

            StringRequest stringRequest = new StringRequest(Request.Method.GET, profile, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    String addr = url;
                    try {
                        addr = new JSONObject(response).getJSONArray("entry").getJSONObject(0).getJSONArray("photos").getJSONObject(0).getString("value");
                        loadImg(nAuthor, addr);
                    } catch (JSONException e) {
                        loadImg(nAuthor, addr, false);
                        //Log.e(TAG, e.toString());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    loadImg(nAuthor, url);
                    //Log.e(TAG, error.toString());
                }
            });

            mQueue.add(stringRequest);
        }
    }

    public void setTime(long time) {
        Timestamp stamp = new Timestamp(time);
        Date date = new Date(stamp.getTime());
        String dStr = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT).format(date);
        mTime.setText(dStr);
    }
}
