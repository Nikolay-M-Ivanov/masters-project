-- Minimal schema for persistence tracking tables used in MVP.
-- Only required tables are declared here.

CREATE TABLE IF NOT EXISTS agent_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender TEXT NOT NULL,
    receiver TEXT NOT NULL,
    performative TEXT NOT NULL,
    content TEXT,
    conversation_id TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS import_batches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_path TEXT NOT NULL,
    status TEXT NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    imported_rows INTEGER NOT NULL DEFAULT 0,
    skipped_rows INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TEXT
);


CREATE INDEX IF NOT EXISTS idx_agent_logs_created_at
    ON agent_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_logs_conversation_id
    ON agent_logs (conversation_id);

CREATE INDEX IF NOT EXISTS idx_import_batches_started_at
    ON import_batches (started_at DESC);

