-- Taules mínimes que fan servir els writers (PostgreSQL).

CREATE TABLE IF NOT EXISTS external_call_result (
    item_id BIGINT PRIMARY KEY,
    status  VARCHAR(32) NOT NULL,
    payload TEXT
);

CREATE TABLE IF NOT EXISTS command_outcome (
    item_id  BIGINT PRIMARY KEY,
    executed BOOLEAN NOT NULL,
    detail   TEXT
);

-- Taula d'origen que llegeix el step 1 (manager).
CREATE TABLE IF NOT EXISTS source_item (
    id      BIGINT PRIMARY KEY,
    payload TEXT
);
