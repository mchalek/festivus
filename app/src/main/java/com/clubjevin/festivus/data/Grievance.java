package com.clubjevin.festivus.data;

/**
 * Created by kevin on 12/1/16.
 */

public class Grievance {
    private final Long timestamp;
    public Long getTimestamp() {
        return timestamp;
    }

    private final String content;
    public String getContent() {
        return content;
    }

    public Grievance(Long timestamp, String content) {
        this.timestamp = timestamp;
        this.content = content;
    }
}
