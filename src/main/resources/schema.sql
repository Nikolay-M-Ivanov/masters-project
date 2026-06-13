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


CREATE INDEX IF NOT EXISTS idx_agent_logs_created_at
    ON agent_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_agent_logs_conversation_id
    ON agent_logs (conversation_id);


