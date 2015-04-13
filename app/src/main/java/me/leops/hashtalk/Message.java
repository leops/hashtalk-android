package me.leops.hashtalk;

import java.util.List;

public class Message {
    private String author;
    private String content;
    private List<String> hashtag;
    private long time;

    @SuppressWarnings("unused")
    private Message() {}

    Message(String author, String content, List<String> hashtag, long time) {
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

    public List<String> getHashtag() {
        return hashtag;
    }

    public long getTime() {
        return time;
    }

    public boolean test(CharSequence query) {
        if(author.contains(query))
            return true;

        for (String h : hashtag)
            if (h.contains(query))
                return true;

        return false;
    }
}
