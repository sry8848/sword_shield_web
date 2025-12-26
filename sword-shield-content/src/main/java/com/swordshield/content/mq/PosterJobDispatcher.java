package com.swordshield.content.mq;

public interface PosterJobDispatcher {
    void dispatch(String jobId);
}

