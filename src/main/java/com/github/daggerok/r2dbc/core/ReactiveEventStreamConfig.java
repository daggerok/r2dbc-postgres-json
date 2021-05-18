package com.github.daggerok.r2dbc.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;

@Configuration
public class ReactiveEventStreamConfig {

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
