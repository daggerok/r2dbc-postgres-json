package com.github.daggerok.r2dbc;

import com.fasterxml.jackson.annotation.JsonInclude;
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

import static com.github.daggerok.r2dbc.API.*;

/**
 * Jsonp requires public classes / constructors...
 */
interface API {

  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @PersistenceConstructor)
  class JsonData {

    @Setter(AccessLevel.PUBLIC)
    private Long sequenceNumber;

    @Setter(AccessLevel.PUBLIC)
    private LocalDateTime occurredAt;

    private UUID aggregateId;

    private String eventType;

    /* register visitor event fields: */
    private String name;
    private LocalDateTime expireAt;

    /* pass card delivered event fields: none */

    /* door entered event fields: */
    private String doorId;
  }

  @Data
  @Table("domain_events")
  @JsonInclude(JsonInclude.Include.NON_NULL)
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

  @Data
  @Getter
  @Table("domain_events")
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  class VisitorRegisteredEvent extends Event {

    public static VisitorRegisteredEvent from(Event event) {
      var eventData = API.Event.jsonb.fromJson(event.getJsonData(), JsonData.class);
      var aggregateId = Optional.ofNullable(eventData.getAggregateId())
                                .orElseGet(UUID::randomUUID);
      var expireAt = Optional.ofNullable(eventData.getExpireAt())
                             .orElseGet(() -> LocalDateTime.now().plus(1, ChronoUnit.DAYS));
      return of(aggregateId, eventData.getName(), expireAt);
    }

    public static VisitorRegisteredEvent of(UUID aggregateId, String name, LocalDateTime expireAt) {
      return new VisitorRegisteredEvent(aggregateId, name, expireAt);
    }

    public VisitorRegisteredEvent(UUID aggregateId, String name, LocalDateTime expireAt) {
      super(null, aggregateId,
            API.Event.jsonb.toJson(
                new JsonData(null, LocalDateTime.now(), aggregateId,
                             VisitorRegisteredEvent.class.getSimpleName(),
                             name, expireAt, null)));
    }
  }

  @Data
  @Getter
  @Table("domain_events")
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
      // @AllArgsConstructor(onConstructor_ = @PersistenceConstructor)
  class PassCardDeliveredEvent extends Event {

    public static PassCardDeliveredEvent from(Event event) {
      var eventData = API.Event.jsonb.fromJson(event.getJsonData(), JsonData.class);
      return of(eventData.getAggregateId());
    }

    public static PassCardDeliveredEvent of(UUID aggregateId) {
      return new PassCardDeliveredEvent(aggregateId);
    }

    public PassCardDeliveredEvent(UUID aggregateId) {
      super(null, aggregateId,
            API.Event.jsonb.toJson(
                new JsonData(null, LocalDateTime.now(), aggregateId,
                             PassCardDeliveredEvent.class.getSimpleName(),
                             null, null, null)));
    }
  }

  @Data
  @Getter
  @Table("domain_events")
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor(access = AccessLevel.PACKAGE)
  class EnteredTheDoorEvent extends Event {

    public static EnteredTheDoorEvent from(Event event) {
      var eventData = API.Event.jsonb.fromJson(event.getJsonData(), JsonData.class);
      return of(eventData.getAggregateId(), eventData.getDoorId());
    }

    public static EnteredTheDoorEvent of(UUID aggregateId, String doorId) {
      return new EnteredTheDoorEvent(aggregateId, doorId);
    }

    public EnteredTheDoorEvent(UUID aggregateId, String doorId) {
      super(null, aggregateId,
            API.Event.jsonb.toJson(
                new JsonData(null, LocalDateTime.now(), aggregateId,
                             EnteredTheDoorEvent.class.getSimpleName(),
                             null, null, doorId)));
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

  private static final Function<Event, Mono<Event>> toSpecific = event -> {
    var eventData = API.Event.jsonb.fromJson(event.getJsonData(), JsonData.class);
    var type = eventData.getEventType();
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
  Mono<Event> appendEvent(@RequestBody Event event) {
    log.info("Process event append: {}", event);
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
  Flux<JsonData> streamAggregateEvents(@PathVariable UUID aggregateId) {
    log.info("Stream {} events", aggregateId);
    return Flux.concat(eventStore.findByAggregateIdOrderBySequenceNumberAsc(aggregateId)
                                 .map(event -> API.Event.jsonb.fromJson(event.getJsonData(), JsonData.class)),
                       eventSubscription.map(event -> API.Event.jsonb.fromJson(event.getJsonData(), JsonData.class))
                                        .filter(eventData -> aggregateId.equals(eventData.getAggregateId())))
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
