package com.clubjevin.festivus.data;

/**
 * Created by kevin on 12/1/16.
 */

public class Grievance {
    public enum GrievanceType { TEXT, RECORDING };

    final GrievanceType type;
    public GrievanceType getType() {
        return type;
    }

    private final Long timestamp;
    public Long getTimestamp() {
        return timestamp;
    }

    private final String text;
    public String getText() {
        return text;
    }

    private final String recording;
    public String getRecording() {
        return recording;
    }

    public Grievance(Long timestamp, String text, String recording) {
        this.timestamp = timestamp;
        this.text = text;
        this.recording = recording;

        assert(text == null || recording == null);

        GrievanceType type = null;
        if(text == null) {
            type = GrievanceType.RECORDING;
        } else {
            type = GrievanceType.TEXT;
        }

        this.type = type;
    }
}
