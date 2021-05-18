package com.github.daggerok.r2dbc.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;

@Log4j2
@Service
@RequiredArgsConstructor
public class SavedEventsProcessor {

  private final Flux<Event> eventSubscription;

  @PostConstruct
  public void initializedEventProcessor() {
    eventSubscription.subscribe(event -> log.info("saved: {}", event));
  }
}
