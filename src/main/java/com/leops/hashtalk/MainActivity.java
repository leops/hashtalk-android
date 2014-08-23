package com.leops.hashtalk;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.internal.widget.ActionBarView;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity implements MessagesFragment.OnMessageClickListener {

    private EditText txtSearch;
    private String search = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String data = intent.getDataString();

        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            search = data;
        } else if (intent.getAction().equals(Intent.ACTION_SEND) && data != null) {
            //Log.i("StartArg", data);
            ((EditText) findViewById(R.id.contentText)).setText(data);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            search = intent.getStringExtra(SearchManager.QUERY);
        }

        final ImageButton button = (ImageButton) findViewById(R.id.sendButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage();
            }
        });

        final SharedPreferences settings = getPreferences(0);
        String name = settings.getString("nickname", "");
        EditText nickname = (EditText) findViewById(R.id.nickname);
        nickname.setText(name);
        nickname.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    String val = ((TextView) v).getText().toString();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("nickname", val);
                    editor.commit();
                }
            }
        });

        /*LinearLayout msgCompose = ((LinearLayout) findViewById(R.id.compose_layout));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            msgCompose.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            msgCompose.setOrientation(LinearLayout.VERTICAL);
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        final MenuItem item = menu.findItem(R.id.search);
        View v = MenuItemCompat.getActionView(item);
        txtSearch = (EditText) v.findViewById(R.id.txt_search);
        txtSearch.setText(search);
        txtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    setFragmentQuery(v.getText().toString());
                    item.collapseActionView();
                    handled = true;
                }
                return handled;
            }
        });
        txtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //NOOP
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setFragmentQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                //NOOP
            }
        });

        ImageButton btn = (ImageButton) v.findViewById(R.id.clear);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtSearch.setText("");
                setFragmentQuery("");
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    /*public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        LinearLayout msgCompose = ((LinearLayout) findViewById(R.id.compose_layout));
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            msgCompose.setOrientation(LinearLayout.HORIZONTAL);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            msgCompose.setOrientation(LinearLayout.VERTICAL);
        }
    }*/

    public void onMessageClicked(String link) {
        //Log.d("Link", link);
        txtSearch.setText(link);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                shareSearch(search);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareSearch(String data) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "http://leops.github.io/hashtalk/#" + data);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.action_share)));
    }

    private void setFragmentQuery(String data) {
        search = data;
        MessagesFragment fragment = (MessagesFragment) getSupportFragmentManager().findFragmentById(R.id.messages_fragment);
        if(fragment != null) {
            fragment.setQuery(data);
        }
    }

    private void sendMessage() {
        EditText inputText = (EditText) findViewById(R.id.contentText);
        String input = inputText.getText().toString();

        EditText nameText = (EditText) findViewById(R.id.nickname);
        String name = nameText.getText().toString();
        if (input != "" && name != "") {
            Message msg = new Message(name, input, System.currentTimeMillis() / 1000);

            Pattern p = Pattern.compile("#(\\S+)", Pattern.CASE_INSENSITIVE);
            Matcher m;
            if(search != "") {
                m = p.matcher(search);
                while (m.find()) {
                    msg.addHashtag(m.group(1));
                }
            } else
                msg.addHashtag("");

            m = p.matcher(input);
            while (m.find()) {
                msg.addHashtag(m.group(1));
            }

            MessagesFragment fragment = (MessagesFragment) getSupportFragmentManager().findFragmentById(R.id.messages_fragment);
            if(fragment != null) {
                fragment.sendMessage(msg);
                inputText.setText("");
            }
        }
    }
}
