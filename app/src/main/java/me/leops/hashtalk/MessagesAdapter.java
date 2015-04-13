package me.leops.hashtalk;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesAdapter extends RecyclerView.Adapter<MessageHolder> implements Filterable {

    private static final String TAG = "MessagesAdapter";

    private List<Message> mModels;
    private List<Message> mFiltered;
    private Map<String, Message> mModelKeys;
    private Map<String, Uri> mImgCache;
    private CharSequence mFilterQuery;
    private RequestQueue mQueue;
    private Filter mFilter;

    public MessagesAdapter(Firebase fbRef) {
        mModels = new ArrayList<>();
        mFiltered = null; //new ArrayList<>();
        mModelKeys = new HashMap<>();
        mImgCache = new HashMap<>();
        mQueue = null;
        mFilter = null;
        mFilterQuery = "";

        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if(mFiltered != null)
                    for(int i = positionStart; i < positionStart + itemCount; i++) {
                        Message msg = mModels.get(i);
                        if(msg.test(mFilterQuery))
                            mFiltered.add(msg);
                    }
                super.onItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                if(mFiltered != null)
                    for(int i = positionStart; i < positionStart + itemCount; i++) {
                        Message msg = mModels.get(i);
                        if(msg.test(mFilterQuery)) {
                            int idx = mFiltered.indexOf(msg);
                            mFiltered.add(idx, msg);
                        } else {
                            mFiltered.remove(msg);
                        }
                    }
                super.onItemRangeChanged(positionStart, itemCount);
            }
        });

        fbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Message model = dataSnapshot.getValue(Message.class);
                mModelKeys.put(dataSnapshot.getKey(), model);

                int nextIndex = 0;
                if (previousChildName == null) {
                    mModels.add(0, model);
                } else {
                    Message previousModel = mModelKeys.get(previousChildName);
                    int previousIndex = mModels.indexOf(previousModel);
                    nextIndex = previousIndex + 1;
                    if (nextIndex == mModels.size()) {
                        mModels.add(model);
                    } else {
                        mModels.add(nextIndex, model);
                    }
                }

                notifyItemInserted(nextIndex);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String modelName = dataSnapshot.getKey();
                Message oldModel = mModelKeys.get(modelName);
                Message newModel = dataSnapshot.getValue(Message.class);
                int index = mModels.indexOf(oldModel);
                mModels.set(index, newModel);
                mModelKeys.put(modelName, newModel);
                notifyItemChanged(index);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String modelName = dataSnapshot.getKey();
                Message oldModel = mModelKeys.get(modelName);
                int index = mModels.indexOf(oldModel);
                mModels.remove(oldModel);
                mModelKeys.remove(modelName);
                notifyItemRemoved(index);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                String modelName = dataSnapshot.getKey();
                Message oldModel = mModelKeys.get(modelName);
                Message newModel = dataSnapshot.getValue(Message.class);
                int index = mModels.indexOf(oldModel);
                int nextIndex = 0;
                mModels.remove(index);
                if (previousChildName == null) {
                    mModels.add(0, newModel);
                } else {
                    Message previousModel = mModelKeys.get(previousChildName);
                    int previousIndex = mModels.indexOf(previousModel);
                    nextIndex = previousIndex + 1;
                    if (nextIndex == mModels.size()) {
                        mModels.add(newModel);
                    } else {
                        mModels.add(nextIndex, newModel);
                    }
                }
                notifyItemMoved(index, nextIndex);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Listen was cancelled, no more updates will occur");
            }
        });
    }

    @Override
    public MessageHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.message_layout, viewGroup, false);

        if(mQueue == null)
            mQueue = Volley.newRequestQueue(context);

        return new MessageHolder(v, context, mQueue, mImgCache);
    }

    @Override
    public void onBindViewHolder(MessageHolder viewHolder, final int position) {
        Message msg;
        if(mFiltered == null)
            msg = mModels.get(position);
        else
            msg = mFiltered.get(position);

        viewHolder.setContent(msg.getContent());
        viewHolder.setAuthor(msg.getAuthor());
        viewHolder.setTime(msg.getTime());
    }

    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    FilterResults results = new FilterResults();

                    if (charSequence == null || charSequence.length() == 0) {
                        mFilterQuery = "";
                        results.values = null;
                        results.count = -1;
                    } else {
                        mFilterQuery = charSequence;
                        List<Message> nMsgList = new ArrayList<>();

                        for (Message msg : mModels) {
                            if(msg.test(charSequence))
                                nMsgList.add(msg);
                        }

                        results.values = nMsgList;
                        results.count = nMsgList.size();

                    }

                    return results;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    if (filterResults.count > -1)
                        mFiltered = (List<Message>) filterResults.values;
                    else
                        mFiltered = null;

                    notifyDataSetChanged();
                }
            };
        }
        return mFilter;
    }

    @Override
    public int getItemCount() {
        if(mFiltered == null)
            return mModels.size();
        else
            return mFiltered.size();
    }
}

