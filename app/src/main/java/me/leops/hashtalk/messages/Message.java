package me.leops.hashtalk.messages;

import java.util.regex.Pattern;

public class Message {
    public static final Pattern RX_HASHTAG = Pattern.compile("\\#(\\w+)");
    public static final Pattern RX_MENTION = Pattern.compile("\\@(\\w+)");

    private String author;
    private String content;
    private String hashtag;
    private long time;

    @SuppressWarnings("unused")
    private Message() {}

    public Message(String author, String content, String hashtag, long time) {
        this.content = content;
        this.author = author;
        this.hashtag = hashtag;
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public String getHashtag() {
        return hashtag;
    }

    public long getTime() {
        return time;
    }

    public boolean test(CharSequence query) {
        return hashtag.contains(query);
    }
}
