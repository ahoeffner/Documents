-- Run once per tenant schema that already ran migrateDoctext.sql, to reorder
-- doctext/lexvector before content in documents.documents.
-- Usage: psql -f reorderDoctext.sql

BEGIN;

ALTER TABLE documents.links DROP CONSTRAINT dockey;
ALTER TABLE documents.textchunks DROP CONSTRAINT doctext;

ALTER TABLE documents.documents RENAME TO documents_old;

CREATE TABLE documents.documents
(
    id        integer      NOT NULL PRIMARY KEY DEFAULT nextval('documents.documents_id_seq'),
    fldid     integer      NOT NULL,
    date      date         NOT NULL,
    title     varchar(40)  NOT NULL,
    text      text,
    extref    varchar(160),
    doctext   text,
    lexvector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(doctext, ''))) STORED,
    content   bytea
);

INSERT INTO documents.documents (id, fldid, date, title, text, extref, doctext, content)
SELECT id, fldid, date, title, text, extref, doctext, content FROM documents.documents_old;

ALTER SEQUENCE documents.documents_id_seq OWNED BY documents.documents.id;

DROP TABLE documents.documents_old;

ALTER TABLE documents.documents ADD CONSTRAINT dockey
    FOREIGN KEY (fldid) REFERENCES documents.folders (id);

ALTER TABLE documents.links ADD CONSTRAINT dockey
    FOREIGN KEY (docid) REFERENCES documents.documents (id) ON DELETE CASCADE;

ALTER TABLE documents.textchunks ADD CONSTRAINT doctext
    FOREIGN KEY (docid) REFERENCES documents.documents (id) ON DELETE CASCADE;

CREATE INDEX documents_lex ON documents.documents USING gin(lexvector);

COMMIT;
