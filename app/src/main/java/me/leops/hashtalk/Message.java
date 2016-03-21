package me.leops.hashtalk;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message {
    public static final Pattern RX_HASHTAG = Pattern.compile("\\#(\\w+)");
    public static final Pattern RX_MENTION = Pattern.compile("\\@(\\w+)");

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
        Matcher mm = RX_MENTION.matcher(query);
        while (mm.find()) {
            if(!author.equals(mm.group(1)))
                return false;
        }

        Matcher hm = RX_HASHTAG.matcher(query);
        while (hm.find()) {
            if(!hashtag.contains(hm.group(1)))
                return false;
        }

        return true;
    }
}
