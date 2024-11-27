package org.example.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    public void addEvent(AbstractEventBuilder<?> eventBuilder) {
        if (Objects.isNull(eventBuilder)) return;
        CompletableFuture.runAsync(() -> {
            // sent to event service
            log.info("sent {} to event service", eventBuilder.buildEvent());
        });
    }
}
