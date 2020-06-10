package com.hq.hqmusic.Lrc;

public class LrcList {
    //保存当前时间
    private long currentTime;
    //保存内容
    private String content;

    public long getCurrentTime() {
        return currentTime;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public String getContent() {
        return content;
    }
}
