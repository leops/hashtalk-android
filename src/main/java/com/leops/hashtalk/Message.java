package com.leops.hashtalk;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 16/08/2014.
 */
public class Message {
    private String author;
    private String content;
    private List<Object> hashtag = new ArrayList<Object>();
    private Long time;

    @SuppressWarnings("unused")
    public Message() { }

    Message(String author, String content, Long time) {
        this.author = author;
        this.content = content;
        this.time = time;
    }

    public void addHashtag(String h) {
        hashtag.add(h);
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public List<Object> getHashtag() {
        return hashtag;
    }

    public Long getTime() { return time; }
}
