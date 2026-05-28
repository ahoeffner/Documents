-- Create the Embedding function wrapper --

create or replace function getEmbedding(text text, query boolean) returns vector
language plpython3u as $$
    import json
    import requests

    url = "http://gemini:8000/embed"

    payload = {"text": text}
    if (query) : payload = {"query": text}

    headers = {"Content-Type": "application/json"}


    try:
        response = requests.post(url, data=json.dumps(payload), headers=headers, timeout=10)
        response.raise_for_status()
        
        result = response.json()
        embedding = result.get("embedding")
        
        if not isinstance(embedding, list) or not all(isinstance(x, (int, float)) for x in embedding):
            plpy.error("API did not return a valid list of numbers for embedding.")
            
        return(embedding)

    except requests.exceptions.RequestException as e:
        plpy.error(f"HTTP request to embedding service failed: {e}")
        return None
$$;

alter function getEmbedding owner to alex;


create or replace function ginvec() returns trigger as $$
begin
  if ((new.text is not null and new.lexvector is null) or (new.lang != old.lang)) then
    new.lexvector = to_tsvector(new.lang::regconfig,new.text);  
  end if;
  return(new);
end;
$$ language plpgsql;

alter function ginvec owner to alex;


create or replace function embeddings() returns trigger as $$
begin
  if (new.text is not null and new.embedding is null) then
	new.embedding = getEmbedding(new.text,false);
  end if;

  return(new);
end; 
$$ language plpgsql;

alter function embeddings owner to alex;




-- Create the tables and add embedding triggers --


drop table languages;

create table languages
(
	id varchar not null primary key,
	lang varchar(15) not null
);

alter table languages owner to alex;

insert into languages(id,lang) values ('DA','danish');
insert into languages(id,lang) values ('EN','english');

update languages set id = upper(i)


drop table categories;

create table categories
(
	id serial not null primary key,
	name varchar(15) not null
);

alter table categories owner to alex;

insert into categories(name) values ('Receipts');



drop table documents;

create table documents
(
	id serial not null primary key,
	catid integer not null,	
	date date not null ,
	title varchar(40) not null,
	text text,
	extref varchar(160),
	content bytea
);

alter table documents owner to alex;

alter table documents add constraint doccat 
    foreign key (catid)
	references categories (id);



drop table textchunks;

create table textchunks
(
	docid integer not null,
	line integer not null,
	text text not null,
	lang varchar not null default 'danish',
	lexvector tsvector,
	embedding vector(768),
	primary key (docid,line)
);

alter table textchunks owner to alex;

create index textchunks_lex on textchunks
using gin(lexvector);

alter table textchunks add constraint doctext 
    foreign key (docid)
	references documents (id)
	on delete cascade;


create trigger ginvec
before insert or update on textchunks
for each row execute function ginvec();


create trigger embeddings
before insert or update on textchunks
for each row execute function embeddings();
