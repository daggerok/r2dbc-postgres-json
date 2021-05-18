package com.github.daggerok.r2dbc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.daggerok.r2dbc.InternalAPI.EnteredTheDoorEvent;
import com.github.daggerok.r2dbc.InternalAPI.PassCardDeliveredEvent;
import com.github.daggerok.r2dbc.InternalAPI.VisitorRegisteredEvent;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.daggerok.r2dbc.API.Event;

/**
 * Jsonp requires public classes / constructors...
 */
interface API {

  @Data
  @Table("domain_events")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @PersistenceConstructor)
  class Event {

    protected static final Jsonb jsonb = JsonbBuilder.create();

    /* common event fields: */
    @Id
    @Setter(AccessLevel.PUBLIC)
    private Long sequenceNumber;

    private UUID aggregateId;

    private String jsonData;
  }
}

/**
 * Jsonp requires public classes / constructors...
 */
interface InternalAPI {

  @Data
  @Getter
  @Table("domain_events")
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  class VisitorRegisteredEvent extends Event {

    public static VisitorRegisteredEvent from(JsonObject event) {
      var jsonData = Optional.ofNullable(event.getString("jsonData", null))
                             .orElseThrow(RuntimeException::new);
      var jsonObject = Event.jsonb.fromJson(jsonData, JsonObject.class);
      var aggregateId = Optional.ofNullable(jsonObject.getString("aggregateId", null))
                                .map(UUID::fromString)
                                .orElseGet(UUID::randomUUID);
      var expireAt = Optional.ofNullable(jsonObject.getString("expireAt", null))
                             .map(LocalDateTime::parse)
                             .orElseGet(() -> LocalDateTime.now().plus(1, ChronoUnit.DAYS));
      var json = Json.createObjectBuilder(jsonObject)
                     .add("expireAt", expireAt.toString())
                     .add("updatedAt", LocalDateTime.now().toString())
                     .build()
                     .toString();

      return of(aggregateId, json);
    }

    public static VisitorRegisteredEvent of(UUID aggregateId, String json) {
      return new VisitorRegisteredEvent(aggregateId, json);
    }

    public VisitorRegisteredEvent(UUID aggregateId, String json) {
      super(null, aggregateId, json);
    }
  }

  @Data
  @Getter
  @Table("domain_events")
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  class PassCardDeliveredEvent extends Event {

    public static PassCardDeliveredEvent from(JsonObject event) {
      var jsonData = Optional.ofNullable(event.getString("jsonData", null))
                             .orElseThrow(RuntimeException::new);
      var jsonObject = Event.jsonb.fromJson(jsonData, JsonObject.class);
      var aggregateId = Optional.ofNullable(jsonObject.getString("aggregateId", null))
                                .map(UUID::fromString)
                                .orElseThrow(RuntimeException::new);
      var json = Json.createObjectBuilder(jsonObject)
                     .add("updatedAt", LocalDateTime.now().toString())
                     .build()
                     .toString();
      return of(aggregateId, json);
    }

    public static PassCardDeliveredEvent of(UUID aggregateId, String json) {
      return new PassCardDeliveredEvent(aggregateId, json);
    }

    public PassCardDeliveredEvent(UUID aggregateId, String json) {
      super(null, aggregateId, json);
    }
  }

  @Data
  @Getter
  @Table("domain_events")
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  class EnteredTheDoorEvent extends Event {

    public static EnteredTheDoorEvent from(JsonObject event) {
      var jsonData = Optional.ofNullable(event.getString("jsonData", null))
                             .orElseThrow(RuntimeException::new);
      var jsonObject = Event.jsonb.fromJson(jsonData, JsonObject.class);
      var aggregateId = Optional.ofNullable(jsonObject.getString("aggregateId", null))
                                .map(UUID::fromString)
                                .orElseThrow(RuntimeException::new);
      var json = Json.createObjectBuilder(jsonObject)
                     .add("updatedAt", LocalDateTime.now().toString())
                     .build()
                     .toString();
      return of(aggregateId, json);
    }

    public static EnteredTheDoorEvent of(UUID aggregateId, String json) {
      return new EnteredTheDoorEvent(aggregateId, json);
    }

    public EnteredTheDoorEvent(UUID aggregateId, String json) {
      super(null, aggregateId, json);
    }
  }
}

interface EventStore extends ReactiveCrudRepository<Event, Long> {

  // @Query(" select sequence_number,            " +
  //     "           aggregate_id,               " +
  //     "           json_data,                  " +
  //     "      from domain_events               " +
  //     "     where aggregate_id = :aggregateId " +
  //     "  order by sequence_number ASC         ")
  Flux<Event> findByAggregateIdOrderBySequenceNumberAsc(@Param("aggregateId") UUID aggregateId);
}

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class Infrastructure {

  public static final Function<Throwable, Map<String, String>> wrap =
      throwable -> Map.of("error", throwable.getMessage());

  public static final Function<String, Supplier<RuntimeException>> error =
      message -> () -> new RuntimeException(message);
}

@Configuration
class ReactiveEventStreamConfig {

  @Value("${app.buffer-size:2048}")
  Integer bufferSize;

  @Bean
  Sinks.Many<Event> eventProcessor() {
    return Sinks.many()
                .multicast()
                .directBestEffort(); // .onBackpressureBuffer(bufferSize);
  }

  @Bean
  Consumer<Event> eventSavedPublisher(Sinks.Many<Event> eventProcessor) {
    return eventProcessor::tryEmitNext;
  }

  @Bean
  Scheduler eventStreamScheduler() {
    return Schedulers.newSingle("eventStreamScheduler");
  }

  @Bean
  Flux<Event> eventSubscription(Scheduler eventStreamScheduler,
                                Sinks.Many<Event> eventProcessor) {
    return eventProcessor.asFlux()
                         .publishOn(eventStreamScheduler)
                         .subscribeOn(eventStreamScheduler)
                         .onBackpressureBuffer(bufferSize) // tune me if you wish...
        // .share() // DO NOT SHARE when using newer reactor API, such as Sinks.many()...!
        ;
  }
}

@Log4j2
@Service
@RequiredArgsConstructor
class SavedEventsProcessor {

  private final Flux<Event> eventSubscription;

  @PostConstruct
  public void initializedEventProcessor() {
    eventSubscription.subscribe(event -> log.info("saved: {}", event));
  }
}

@Log4j2
@RestController
@RequiredArgsConstructor
class EventAppenderResource {

  private final EventStore eventStore;
  private final Consumer<Event> eventSavedPublisher;

  private static final Function<JsonObject, Mono<Event>> toSpecific = event -> {
    var type = Optional.ofNullable(event.getString("eventType", null))
                       .orElseThrow(RuntimeException::new);
    if (VisitorRegisteredEvent.class.getSimpleName().equals(type)) return Mono.just(VisitorRegisteredEvent.from(event));
    if (PassCardDeliveredEvent.class.getSimpleName().equals(type)) return Mono.just(PassCardDeliveredEvent.from(event));
    if (EnteredTheDoorEvent.class.getSimpleName().equals(type)) return Mono.just(EnteredTheDoorEvent.from(event));
    return Mono.empty();
  };

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
               .flatMap(toSpecific)
               .flatMap(eventStore::save)
               .doOnSuccess(eventSavedPublisher);
  }
}

@Log4j2
@RestController
@RequiredArgsConstructor
class EventStreamerResource {

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

@SpringBootApplication
@EnableR2dbcRepositories
public class DemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }
}
