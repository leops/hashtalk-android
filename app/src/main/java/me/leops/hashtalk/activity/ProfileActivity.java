package me.leops.hashtalk.activity;

import android.accounts.Account;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import me.leops.hashtalk.R;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Account account = intent.getParcelableExtra("account");
        String pass = intent.getStringExtra("password");

        final Firebase fbRef = new Firebase("http://hashtalk.firebaseio.com/users");
        fbRef.authWithPassword(account.name, pass, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                final Firebase user = fbRef.child(authData.getUid());

                final ImageView avatar = (ImageView) findViewById(R.id.avatar);

                final String[] nameStr = {null};
                final TextInputEditText nickname = (TextInputEditText) findViewById(R.id.nickname);
                if(nickname != null)
                    nickname.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if(!nameStr[0].equals(v.getText())) {
                                nameStr[0] = v.getText().toString();
                                user.child("displayName").setValue(nameStr[0]);
                            }

                            return true;
                        }
                    });

                user.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (nickname != null) {
                            if(nameStr[0] == null)
                                nickname.setEnabled(true);

                            nameStr[0] = dataSnapshot.child("displayName").getValue(String.class);
                            nickname.setText(nameStr[0]);
                        }

                        if(avatar != null) {
                            Glide.with(ProfileActivity.this)
                                .load(
                                    dataSnapshot.child("avatar")
                                        .getValue(String.class)
                                )
                                .fitCenter()
                                .crossFade(100)
                                .into(avatar);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        Log.e(TAG, "onCancelled", firebaseError.toException());
                    }
                });
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.e(TAG, "onAuthenticationError", firebaseError.toException());
                finish();
            }
        });
    }

    public void editProfilePic(View view) {
        sendBroadcast(new Intent(Intent.ACTION_VIEW, Uri.parse("http://en.gravatar.com/")));
    }
}
