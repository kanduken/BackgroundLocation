package com.loktra.backlocation;

/**
 * Created by lovleen on 6/9/17.
 */

public class TimeTracker {

    long statTime;

    public void startTimer() {
        resetTime();
        statTime = System.currentTimeMillis();
    }

    public long duration() {
        return System.currentTimeMillis() - statTime;
    }

    private void resetTime() {
        statTime = 0;
    }
}
