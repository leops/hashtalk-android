package me.leops.hashtalk.messages;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.leops.hashtalk.R;
import me.leops.hashtalk.activity.MainActivity;

public class MessagesAdapter extends RecyclerView.Adapter<MessageHolder> {
    private static final String TAG = "MessagesAdapter";

    private MainActivity mActivity;
    private final LayoutInflater mInflater;

    private final Firebase mRef;
    private List<Message> mModels;
    private Map<String, Message> mModelKeys;

    public MessagesAdapter(MainActivity activity, Firebase fbRef) {
        mActivity = activity;
        mInflater = LayoutInflater.from(mActivity);

        mRef = fbRef;
        mModels = new ArrayList<>();
        mModelKeys = new HashMap<>();
    }

    @Override
    public MessageHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new MessageHolder(
            mInflater.inflate(R.layout.message_layout, viewGroup, false),
            mActivity
        );
    }

    @Override
    public void onBindViewHolder(MessageHolder viewHolder, final int position) {
        viewHolder.setMessage(mModels.get(position));
    }

    public void setFilter(String filter) {
        mModels.clear();
        mModelKeys.clear();
        notifyDataSetChanged();

        if(filter.length() > 0) {
            mRef.child(filter).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mActivity.onDoneLoading(dataSnapshot.getChildrenCount() > 0);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    //
                }
            });

            mRef.child(filter).addChildEventListener(new ChildEventListener() {
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
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }
}

