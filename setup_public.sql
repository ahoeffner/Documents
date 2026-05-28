-- Run once per database.
-- Creates shared trigger functions in the public schema so all tenant schemas can reference them.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS plpython3u;


CREATE OR REPLACE FUNCTION public.getEmbedding(text text, query boolean) RETURNS vector
LANGUAGE plpython3u AS $$
    import json
    import requests

    url = "http://gemini:8000/embed"

    payload = {"text": text}
    if (query): payload = {"query": text}

    headers = {"Content-Type": "application/json"}

    try:
        response = requests.post(url, data=json.dumps(payload), headers=headers, timeout=10)
        response.raise_for_status()

        result = response.json()
        embedding = result.get("embedding")

        if not isinstance(embedding, list) or not all(isinstance(x, (int, float)) for x in embedding):
            plpy.error("API did not return a valid list of numbers for embedding.")

        return embedding

    except requests.exceptions.RequestException as e:
        plpy.error(f"HTTP request to embedding service failed: {e}")
        return None
$$;

ALTER FUNCTION public.getEmbedding(text, boolean) OWNER TO alex;


CREATE OR REPLACE FUNCTION public.ginvec() RETURNS trigger AS $$
BEGIN
    IF (new.text IS NOT NULL AND new.lexvector IS NULL) OR (new.lang != old.lang) THEN
        new.lexvector = to_tsvector(new.lang::regconfig, new.text);
    END IF;
    RETURN new;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION public.ginvec() OWNER TO alex;


CREATE OR REPLACE FUNCTION public.embeddings() RETURNS trigger AS $$
BEGIN
    IF new.text IS NOT NULL AND new.embedding IS NULL THEN
        new.embedding = public.getEmbedding(new.text, false);
    END IF;
    RETURN new;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION public.embeddings() OWNER TO alex;
