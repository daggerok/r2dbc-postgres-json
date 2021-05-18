package com.github.daggerok.r2dbc.core;

import jakarta.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Log4j2
@RestController
@RequiredArgsConstructor
public class EventStreamerResource {

  private final EventStore eventStore;
  private final Flux<Event> eventSubscription;

  @GetMapping(
      path = "/event-stream/{aggregateId}",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE
  )
  Flux<String> streamAggregateEvents(@PathVariable UUID aggregateId) {
    log.info("Stream {} events", aggregateId);
    return Flux.concat(eventStore.findByAggregateIdOrderBySequenceNumberAsc(aggregateId)
                                 .map(Event::getJsonData),
                       eventSubscription.map(event -> Event.jsonb.fromJson(event.getJsonData(), JsonObject.class))
                                        .filter(jsonObject -> aggregateId.toString().equals(jsonObject.getString("aggregateId", null)))
                                        .map(Object::toString))
               .distinct();
  }

  @GetMapping(
      path = "/event-stream",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE
  )
  Flux<Event> streamEvents() {
    log.info("Share event stream to clients...");
    return Flux.concat(eventStore.findAll(), eventSubscription)
               .distinct();
  }
}
