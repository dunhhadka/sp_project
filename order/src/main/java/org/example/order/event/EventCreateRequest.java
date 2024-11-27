package org.example.order.event;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonRootName("event")
public class EventCreateRequest {
    private String arguments;
    private String description;
    private String path;
    private String message;
    private int subjectId;
    private String subjectType;
    private String author;
    private String authorId;
    private String authorType;
    private String verb;
    private String keywords;
}
