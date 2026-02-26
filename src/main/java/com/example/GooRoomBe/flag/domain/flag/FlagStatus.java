package com.example.GooRoomBe.flag.domain.flag;

public enum FlagStatus {
    RECRUITING,
    WAITING,
    IN_ACTIVITY,
    ENDED;

    public boolean isRecruiting() { return this == RECRUITING; }
    public boolean isWaiting() { return this == WAITING; }
    public boolean isInActivity() { return this == IN_ACTIVITY; }
    public boolean isEnded() { return this == ENDED; }

    public boolean isBeforeActivity() {
        return this == RECRUITING || this == WAITING;
    }
}