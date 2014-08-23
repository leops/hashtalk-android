package com.leops.hashtalk;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.firebase.client.Query;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.tkeunebr.gravatar.Gravatar;

/**
 * Created by user on 16/08/2014.
 */
public class MessageListAdapter extends FirebaseListAdapter<Message> implements Filterable {
    private MessageFilter msgFilter;
    private LinkClickListener listener;

    public void setMsgClickListener(LinkClickListener listener) {
        this.listener = listener;
    }

    public MessageListAdapter(Query ref, LayoutInflater inflater, int layout) {
        super(ref, Message.class, layout, inflater);
    }

    @Override
    protected void populateView(View view, Message msg) {
        // Author
        final String author = msg.getAuthor();
        TextView authorText = (TextView)view.findViewById(R.id.author);
        authorText.setText(author);
        authorText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null)
                    listener.onTextLinkClick(v, "@" + author);
            }
        });

        // Image
        //new GetJSONTask(view).execute(author);
        ImageView img = (ImageView) view.findViewById(R.id.profilePicture);
        String avatarUri = Gravatar.init().with(author).size(100).defaultImage(Gravatar.DefaultImage.IDENTICON).build();
        Picasso.with(view.getContext()).load(avatarUri).into(img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null)
                    listener.onTextLinkClick(v, "@" + author);
            }
        });

        //Hashtag
        String h = msg.getHashtag().get(0).toString();
        String content = msg.getContent();
        if(h != "") {
            content += " #" + h;
        }

        //Content
        LinkifyTextView msgView = (LinkifyTextView)view.findViewById(R.id.message);
        msgView.setOnLinkClickListener(new LinkClickListener() {
            @Override
            public void onTextLinkClick(View textView, String clickedString) {
                if(listener != null)
                    listener.onTextLinkClick(textView, clickedString);
            }
        });
        msgView.setLinkText(content);

        // Time
        Date time = new Date(msg.getTime());
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.SHORT);
        ((TextView) view.findViewById(R.id.date)).setText(formatter.format(time));
    }

    @Override
    public Filter getFilter() {
        if (msgFilter == null)
            msgFilter = new MessageFilter();

        return msgFilter;
    }

    private class MessageFilter extends Filter {
        private Boolean similar(String a, String b) {
            Boolean r = a.matches("^.*" + b + ".*$");
            return r;
        }

        private Boolean msgFilter(Message item, String search) {
            Boolean noSearch = (search == "" && item.getHashtag().get(0) == ""),
                    hashMatch = false,
                    authMatch = false;

            if (search != "") {
                //Author
                Pattern p = Pattern.compile("@(\\S+)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(search);
                while (m.find()) {
                    String name = m.group(1);
                    if(similar(item.getAuthor(), name))
                        authMatch = true;
                }

                // Hashtag
                p = Pattern.compile("#(\\S+)", Pattern.CASE_INSENSITIVE);
                m = p.matcher(search);
                while (m.find()) {
                    String name = m.group(1);
                    for (Object hash : item.getHashtag()) {
                        if (similar(hash.toString(), name))
                            hashMatch = true;
                    }
                }
            }

            return noSearch || (hashMatch || authMatch);
        };

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                results.values = models;
                results.count = models.size();
            } else {
                List<Message> nMsgList = new ArrayList<Message>();

                for (Message m : models) {
                    if (msgFilter(m, constraint.toString()))
                        nMsgList.add(m);
                }

                results.values = nMsgList;
                results.count = nMsgList.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,FilterResults results) {
            if (results.count == 0)
                notifyDataSetInvalidated();
            else {
                filterModels = (List<Message>) results.values;
                notifyDataSetChanged();
            }
        }
    }

    private class GetJSONTask extends AsyncTask<String, Integer, String> {
        private View view;
        private String author;

        public GetJSONTask(View view) {
            super();
            this.view = view;
        }

        protected String doInBackground(String... author) {
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();

            try {
                this.author = URLEncoder.encode(author[0], "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
                this.author = author[0];
            }

            HttpGet httpGet = new HttpGet("http://en.gravatar.com/" + this.author + ".json");

            try{
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if(statusCode == 200){
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while((line = reader.readLine()) != null){
                        builder.append(line);
                    }
                } else {
                    Log.e("getJSON", "Code: " + statusCode + ", Author: " + author[0]);
                }
            }catch(ClientProtocolException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } catch(SecurityException e) {
                e.printStackTrace();
            }
            return builder.toString();
        }

        protected void onPostExecute(String result) {
            String uri = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                uri = jsonObject.getJSONArray("entry").getJSONObject(0).getJSONArray("photos").getJSONObject(0).getString("value");
            } catch(Exception e) {
                uri = Gravatar.init().with(this.author).size(42).defaultImage(Gravatar.DefaultImage.IDENTICON).build();
            } finally {
                Log.d("Image", "Author: " + this.author + ", ID: " + this.view.getId() + ", URI: " + uri);
                Picasso.with(view.getContext()).load(uri).into((ImageView) this.view.findViewById(R.id.profilePicture));
            }
        }
    }
}
