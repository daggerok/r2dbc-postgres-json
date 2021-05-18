# R2DBC EventStore [![CI](https://github.com/daggerok/r2dbc-postgres-json/actions/workflows/ci.yaml/badge.svg)](https://github.com/daggerok/r2dbc-postgres-json/actions/workflows/ci.yaml)

IN PROGRESS: Postgres JSON java application based on Spring Boot and Spring WebFlux with R2DBC, Postgres and nkonev
r2dbc migration tool

## Getting started

```bash
./mvnw -P pg-start
./mvnw spring-boot:run

curl -iv localhost:8080/event-stream/00000000-0000-0000-0000-000000000001
curl -iv localhost:8080/event-stream

curl -sSH'Content-Type:application/json' localhost:8080/append-event -d'{"aggregateId":"00000000-0000-0000-0000-000000000001","jsonData":"{\"aggregateId\":\"00000000-0000-0000-0000-000000000001\",\"eventType\":\"VisitorRegisteredEvent\",\"name\":\"Test visitor\"}"}'
curl -sSH'Content-Type:application/json' localhost:8080/append-event -d'{"aggregateId":"00000000-0000-0000-0000-000000000001","jsonData":"{\"aggregateId\":\"00000000-0000-0000-0000-000000000001\",\"eventType\":\"PassCardDeliveredEvent\"}"}'
curl -sSH'Content-Type:application/json' localhost:8080/append-event -d'{"aggregateId":"00000000-0000-0000-0000-000000000001","jsonData":"{\"aggregateId\":\"00000000-0000-0000-0000-000000000001\",\"eventType\":\"EnteredTheDoorEvent\",\"doorId\":\"IN-1\"}"}'
curl -sSH'Content-Type:application/json' localhost:8080/append-event -d'{"aggregateId":"00000000-0000-0000-0000-000000000001","jsonData":"{\"aggregateId\":\"00000000-0000-0000-0000-000000000001\",\"eventType\":\"EnteredTheDoorEvent\",\"doorId\":\"IN-2\"}"}'
curl -sSH'Content-Type:application/json' localhost:8080/append-event -d'{"aggregateId":"00000000-0000-0000-0000-000000000001","jsonData":"{\"aggregateId\":\"00000000-0000-0000-0000-000000000001\",\"eventType\":\"EnteredTheDoorEvent\",\"doorId\":\"OUT-2\"}"}'

./mvnw -P pg-stop
```

<!--

## Backup & Restore

### Backup

```bash
rm -rf ~/google.drive/r2dbc-postgres-json* ; cd ~/Downloads/_code/r2dbc-postgres-json && zip -r ~/google.drive/r2dbc-postgres-json.zip "../$(basename "$PWD")" && cd -;
# or
cd /tmp ; rm -rf /tmp/r2dbc-postgres-json* ; cd ~/Downloads/_code/r2dbc-postgres-json && zip -r /tmp/r2dbc-postgres-json.zip "../$(basename "$PWD")" && cd -;
```

### Restore

```bash
rm -rf /tmp/r2dbc-postgres-json* ; unzip ~/google.drive/r2dbc-postgres-json.zip -d /tmp
# or
rm -rf /tmp/r2dbc-postgres-json* ; unzip /tmp/r2dbc-postgres-json.zip -d /tmp
```

-->

## RTFM

* https://github.com/pgjdbc/pgjdbc/issues/265#issuecomment-428336719
* http://json-b.net/docs/user-guide.html#mapping-an-object
* https://www.postgresqltutorial.com/postgresql-json/
* https://stackoverflow.com/a/58147091/1490636
* https://projectreactor.io/docs/core/release/reference/#processors
