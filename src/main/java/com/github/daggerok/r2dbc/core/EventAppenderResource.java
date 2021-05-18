package com.github.daggerok.r2dbc.core;

import jakarta.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Function;

@Log4j2
@RestController
@RequiredArgsConstructor
public class EventAppenderResource {

  private final EventStore eventStore;
  private final Consumer<Event> eventSavedPublisher;
  private final Function<JsonObject, Mono<Event>> eventMapper; // implementation must be provided!

  @PostMapping(
      path = "/append-event",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  Mono<Event> appendEvent(@RequestBody String json) {
    log.info("Process event append: jsonString {}", json);
    var event = Event.jsonb.fromJson(json, JsonObject.class);
    log.info("Process event append: jsonObject {}", event);
    return Mono.justOrEmpty(event)
               .flatMap(eventMapper)
               .flatMap(eventStore::save)
               .doOnSuccess(eventSavedPublisher);
  }
}
