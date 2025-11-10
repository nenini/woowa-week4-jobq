package com.yerin.jobq.application;

public interface JobHandler {
    String type();
    void handle(String jobId, String payloadJson) throws Exception;
}
