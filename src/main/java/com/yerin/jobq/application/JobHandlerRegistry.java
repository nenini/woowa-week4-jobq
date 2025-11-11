package com.yerin.jobq.application;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JobHandlerRegistry {
    private final Map<String, JobHandler> map;
    public JobHandlerRegistry(java.util.List<JobHandler> handlers) {
        this.map = handlers.stream().collect(Collectors.toMap(JobHandler::type, h -> h));
    }
    public JobHandler get(String type) { return map.get(type); }
}
