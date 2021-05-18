drop table if exists snapshots CASCADE
;

drop table if exists domain_events CASCADE
;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; -- required by uuid_generate_v1

create table domain_events
(
    sequence_number SERIAL PRIMARY KEY,
    -- aggregate_id VARCHAR(36) NOT NULL,
    aggregate_id    UUID NOT NULL,
    json_data       JSON NOT NULL
)
;

-- create table snapshots (
--     id SERIAL PRIMARY KEY,
--     version BIGINT,
--     aggregate_id VARCHAR(36) NOT NULL,
--     json_data jJSON NOT NULL
-- )
-- ;
