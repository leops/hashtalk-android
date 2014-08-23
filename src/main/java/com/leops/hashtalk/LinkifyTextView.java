package com.leops.hashtalk;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by user on 17/08/2014.
 */
public class LinkifyTextView  extends TextView {
    LinkClickListener mListener;
    Pattern screenNamePattern = Pattern.compile("(@[a-zA-Z0-9_]+)");
    Pattern hashTagsPattern = Pattern.compile("(#[a-zA-Z0-9_-]+)");

    public LinkifyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLinkText(String text) {
        SpannableString linkableText = new SpannableString(text);

        ArrayList<Hyperlink> links = this.gatherLinks(linkableText, screenNamePattern);
        links.addAll(this.gatherLinks(linkableText, hashTagsPattern));

        for(int i = 0; i < links.size(); i++) {
            Hyperlink linkSpec = links.get(i);
            linkableText.setSpan(linkSpec.span, linkSpec.start, linkSpec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        setText(linkableText);
    }

    public void setOnLinkClickListener(LinkClickListener newListener) {
        mListener = newListener;
    }

    private ArrayList<Hyperlink> gatherLinks(SpannableString s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        ArrayList<Hyperlink> ret = new ArrayList<Hyperlink>();

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            Hyperlink spec = new Hyperlink();

            spec.textSpan = s.subSequence(start, end);
            spec.span = new InternalURLSpan(spec.textSpan.toString());
            spec.start = start;
            spec.end = end;

            ret.add(spec);
        }

        return ret;
    }

    public class InternalURLSpan extends ClickableSpan {
        private String clickedSpan;

        public InternalURLSpan(String clickedString) {
            clickedSpan = clickedString;
        }

        @Override
        public void onClick(View textView) {
            mListener.onTextLinkClick(textView, clickedSpan);
        }
    }

    class Hyperlink {
        CharSequence textSpan;
        InternalURLSpan span;
        int start;
        int end;
    }
}
