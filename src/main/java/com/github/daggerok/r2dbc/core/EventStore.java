package com.github.daggerok.r2dbc.core;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface EventStore extends ReactiveCrudRepository<Event, Long> {

  // @Query(" select sequence_number,            " +
  //     "           aggregate_id,               " +
  //     "           json_data,                  " +
  //     "      from domain_events               " +
  //     "     where aggregate_id = :aggregateId " +
  //     "  order by sequence_number ASC         ")
  Flux<Event> findByAggregateIdOrderBySequenceNumberAsc(@Param("aggregateId") UUID aggregateId);
}
