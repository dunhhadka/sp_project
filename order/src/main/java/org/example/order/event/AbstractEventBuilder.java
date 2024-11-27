package org.example.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractEventBuilder<T> {
    static ObjectMapper objectMapper;
    static JavaType objectListType;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRAP_ROOT_VALUE);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDateFormat(new ISO8601DateFormat());

        objectListType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Object.class);
    }

    protected final String verb;
    private final int subjectId;
    private final int storeId;
    private final T subject;
    private AuthorInfo author;

    private List<String> keywords = List.of();

    protected AbstractEventBuilder(String verb, int subjectId, T subject, int storeId, AuthorInfo author) {
        this.verb = verb;
        this.subjectId = subjectId;
        this.subject = subject;
        this.storeId = storeId;
        this.author = author;
    }

    public final EventCreateRequest buildEvent() {
        var requestBuilder = EventCreateRequest.builder()
                .arguments(marshalArguments(buildEventArgument(subject, author)))
                .subjectId(subjectId)
                .subjectType(getSubjectType())
                .verb(verb)
                .path(buildEventPath(subject))
                .description(buildEventMessage(verb, subject))
                .description(buildEventDescription(verb, subject));
        if (author != null) {
            requestBuilder
                    .author(author.getAuthor())
                    .authorId(author.getAuthorId())
                    .authorType(author.getAuthorType());
        }
        if (!CollectionUtils.isEmpty(keywords)) {
            requestBuilder.keywords(buildKeyWords(keywords));
        } else if (author != null) {
            requestBuilder.keywords(buildKeyWords(List.of(author.getAuthor())));
        }
        return requestBuilder.build();
    }

    private String buildKeyWords(List<String> keywords) {
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (JsonProcessingException e) {
            log.warn("", e);
        }
        return null;
    }

    public abstract String buildEventDescription(String verb, T subject);

    public abstract String buildEventMessage(String verb, T subject);

    public abstract String buildEventPath(T subject);

    public abstract String getSubjectType();

    private String marshalArguments(List<Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            log.warn("error build event: ", e);
        }
        return null;
    }

    public abstract List<Object> buildEventArgument(T subject, AuthorInfo author);
}
