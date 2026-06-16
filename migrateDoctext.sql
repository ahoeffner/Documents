-- Run once per existing tenant schema to migrate to the doctext/lexvector full-text search design.
-- Usage: psql -v schema=<tenant_name> -f migrateDoctext.sql

ALTER TABLE documents.documents ADD COLUMN IF NOT EXISTS doctext text;

UPDATE documents.documents d
SET doctext = sub.doctext
FROM (
    SELECT docid, string_agg(text, ' ' ORDER BY line) AS doctext
    FROM documents.textchunks
    GROUP BY docid
) sub
WHERE sub.docid = d.id;

ALTER TABLE documents.documents
    ADD COLUMN IF NOT EXISTS lexvector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(doctext, ''))) STORED;

CREATE INDEX IF NOT EXISTS documents_lex ON documents.documents USING gin(lexvector);


DROP TRIGGER IF EXISTS ginvec ON documents.textchunks;
DROP INDEX IF EXISTS documents.textchunks_lex;
ALTER TABLE documents.textchunks DROP COLUMN IF EXISTS lexvector;
ALTER TABLE documents.textchunks DROP COLUMN IF EXISTS lang;

DROP TABLE IF EXISTS documents.languages;

DROP FUNCTION IF EXISTS public.ginvec();
