package ai.dochandler.repository;

import java.util.Set;
import java.sql.Types;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import java.sql.PreparedStatement;
import java.util.stream.Collectors;
import ai.dochandler.config.Database;
import ai.dochandler.model.DocumentRecord;
import ai.dochandler.services.GeminiService;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.support.GeneratedKeyHolder;


@Repository
public class DocumentRepository
{
    private static final Logger log = LoggerFactory.getLogger(DocumentRepository.class);

    private final JdbcTemplate jdbc;
    private final Database db;

    @Value("${app.vector-search.boundary[0]}")
    private double boundaryLow;

    @Value("${app.vector-search.boundary[1]}")
    private double boundaryHigh;


    public DocumentRepository(JdbcTemplate jdbc, Database db)
    {
        this.jdbc = jdbc;
        this.db = db;
    }


    public long create(DocumentRecord record, GeminiService geminiService)
    {
        record.createEmbeddings(geminiService);

        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con ->
        {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO " + docs() + " (fldid, date, title, text, extref, content) VALUES (?, ?, ?, ?, ?, ?)",
                new String[]{"id"}
            );
            ps.setLong(1, record.getFldid());
            ps.setDate(2, record.getDate());
            ps.setString(3, record.getTitle());
            ps.setString(4, record.getText());
            ps.setString(5, record.getFile());
            ps.setBytes(6, record.getContent());
            return(ps);
        }, holder);

        @SuppressWarnings("null")
        long docid = holder.getKey().longValue();

        insertChunks(docid, record);
        log.info("Created document id={} with {} chunks", docid, record.getTextChunks().size());
        return(docid);
    }


    public boolean deleteById(long id)
    {
        return(jdbc.update("DELETE FROM " + docs() + " WHERE id = ?", id) > 0);
    }


    public boolean hasDocuments(long fldid)
    {
        String sql =
            "SELECT COUNT(*) FROM (" +
                "SELECT id FROM " + docs() + " WHERE fldid = ? " +
                "UNION ALL " +
                "SELECT docid FROM " + links() + " WHERE fldid = ?" +
            ") x";
        Integer count = jdbc.queryForObject(sql, Integer.class, fldid, fldid);
        return(count != null && count > 0);
    }


    public boolean deleteByTitle(String title)
    {
        return(jdbc.update("DELETE FROM " + docs() + " WHERE upper(title) = upper(?)", title) > 0);
    }


    public DocumentRecord findById(long id)
    {
        return(findByIdFiltered(id, 0L));
    }


    public List<DocumentRecord> findAll(Long fldid)
    {
        if (fldid != null && fldid > 0)
        {
            String sql =
                "SELECT id, fldid, date, title, text, extref, (content IS NOT NULL) AS has_content, FALSE AS is_link, CAST(NULL AS bigint) AS link_id" +
                " FROM " + docs() + " WHERE fldid = ?" +
                " UNION " +
                "SELECT d.id, d.fldid, d.date, d.title, d.text, d.extref, (d.content IS NOT NULL) AS has_content, TRUE AS is_link, l.id AS link_id" +
                " FROM " + docs() + " d JOIN " + links() + " l ON l.docid = d.id WHERE l.fldid = ?";
            return(jdbc.query(sql, (rs, i) -> mapRowWithLink(rs), fldid, fldid));
        }

        String sql = "SELECT id, fldid, date, title, text, extref, (content IS NOT NULL) AS has_content FROM " + docs();
        return(jdbc.query(sql, (rs, i) -> mapRow(rs)));
    }


    @SuppressWarnings("null")
    public List<DocumentRecord> findAll(Long fldid, String q)
    {
        boolean hasFolder = fldid != null && fldid > 0;
        boolean hasQuery  = q != null && !q.isBlank();
        ArrayList<Object> params = new ArrayList<>();
        String sql;

        if (hasFolder)
        {
            String titleCond  = hasQuery ? " AND title ILIKE ?"   : "";
            String dtitleCond = hasQuery ? " AND d.title ILIKE ?" : "";

            sql = "SELECT * FROM (" +
                  "SELECT id, fldid, date, title, text, extref, (content IS NOT NULL) AS has_content" +
                  " FROM " + docs() + " WHERE fldid = ?" + titleCond +
                  " UNION " +
                  "SELECT d.id, d.fldid, d.date, d.title, d.text, d.extref, (d.content IS NOT NULL) AS has_content" +
                  " FROM " + docs() + " d JOIN " + links() + " l ON l.docid = d.id WHERE l.fldid = ?" + dtitleCond +
                  ") sub ORDER BY date DESC, title";

            params.add(fldid);
            if (hasQuery) params.add("%" + q + "%");
            
            params.add(fldid);
            if (hasQuery) params.add("%" + q + "%");
        }
        else
        {
            StringBuilder sqlBuilder = new StringBuilder(
                "SELECT id, fldid, date, title, text, extref, (content IS NOT NULL) AS has_content FROM " + docs()
            );
            ArrayList<String> conditions = new ArrayList<>();

            if (hasQuery)
            {
                conditions.add("title ILIKE ?");
                params.add("%" + q + "%");
            }

            if (!conditions.isEmpty())
                sqlBuilder.append(" WHERE ").append(String.join(" AND ", conditions));

            sqlBuilder.append(" ORDER BY date DESC, title");
            sql = sqlBuilder.toString();
        }

        return(jdbc.query(sql, (rs, i) -> mapRow(rs), params.toArray()));
    }


    public byte[] getContent(long id)
    {
        List<byte[]> results = jdbc.query
        (
            "SELECT content FROM " + docs() + " WHERE id = ?",
            (rs, i) -> rs.getBytes("content"), id
        );

        return(results.isEmpty() ? null : results.get(0));
    }


    public String getFilename(long id)
    {
        List<String> results = jdbc.query
        (
            "SELECT extref FROM " + docs() + " WHERE id = ?",
            (rs, i) -> rs.getString("extref"), id
        );

        return(results.isEmpty() ? null : results.get(0));
    }


    public String getDescription(long id)
    {
        List<String> results = jdbc.query
        (
            "SELECT text FROM " + docs() + " WHERE id = ?",
            (rs, i) -> rs.getString("text"), id
        );

        return(results.isEmpty() ? null : results.get(0));
    }


    public String getAllText(long id)
    {
        List<String> rows = jdbc.query
        (
            "SELECT text FROM " + chunks() + " WHERE docid = ? ORDER BY line",
            (rs, i) -> rs.getString("text"), id
        );

        return(String.join("\n", rows));
    }


    public List<DocumentRecord> lexicalSearch(String[] words, long fldid)
    {
        String tsquery = toTsQuery(words);
        if (tsquery.isEmpty()) return(List.of());

        String sql = "SELECT DISTINCT(docid) FROM " + chunks() + " WHERE lexvector @@ ";
        List<Long> docids;

        try
        {
            docids = jdbc.query(sql + "to_tsquery(lang::regconfig, ?)", (rs, i) -> rs.getLong("docid"), tsquery);
        }
        catch (DataAccessException e)
        {
            log.warn("Invalid tsquery '{}', falling back to plainto_tsquery: {}", tsquery, e.getMessage());
            docids = jdbc.query(sql + "plainto_tsquery(lang::regconfig, ?)", (rs, i) -> rs.getLong("docid"), String.join(" ", words));
        }

        return(fetchByIds(docids, fldid));
    }


    private String toTsQuery(String[] words)
    {
        List<String> tokens = Arrays.stream(words)
            .map(w -> w.replaceAll("[^\\p{L}\\p{N}_|&!()*-]", ""))
            .map(w -> w.replaceAll("([\\p{L}\\p{N}_-]+)\\*", "$1:*"))
            .filter(w -> !w.isEmpty() && !w.equals(":*"))
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();

        for (String token : tokens)
        {
            if (sb.length() > 0)
            {
                char prev = sb.charAt(sb.length() - 1);
                char next = token.charAt(0);
                boolean prevIsOperator = prev == '|' || prev == '&' || prev == '!' || prev == '(';
                boolean nextIsOperator = next == '|' || next == '&' || next == ')';

                sb.append(prevIsOperator || nextIsOperator ? " " : " & ");
            }

            sb.append(token);
        }

        return(sb.toString());
    }


    public List<DocumentRecord> hybridSearch(String semantic, String[] lexical, long fldid, double threshold, GeminiService geminiService)
    {
        Float[] embedding = geminiService.computeEmbedding(semantic, true);
        boolean hasEmbedding = embedding != null && embedding.length > 0;
        String vecStr = hasEmbedding ? toVectorLiteral(embedding) : null;
        String wordlist = String.join(" ", lexical);
        boolean hasLexical = !wordlist.isBlank();

        log.debug("Hybrid search: threshold={}, lexical='{}', semantic='{}', hasEmbedding={}", threshold, wordlist, semantic, hasEmbedding);

        if (!hasEmbedding && !hasLexical) return(List.of());

        Set<Long> docids = new LinkedHashSet<>();

        if (hasEmbedding)
        {
            docids.addAll(jdbc.query(
                "SELECT DISTINCT(docid) FROM " + chunks() + " WHERE (embedding <=> ?::vector) < ?",
                ps ->
                {
                    ps.setObject(1, vecStr, Types.OTHER);
                    ps.setDouble(2, threshold);
                },
                (rs, i) -> rs.getLong("docid")
            ));
        }

        if (hasLexical)
        {
            String tsquery = lexicalToTsQuery(lexical);
            String sql = "SELECT DISTINCT(docid) FROM " + chunks() + " WHERE lexvector @@ ";

            try
            {
                docids.addAll(jdbc.query(sql + "to_tsquery(lang::regconfig, ?)", (rs, i) -> rs.getLong("docid"), tsquery));
            }
            catch (DataAccessException e)
            {
                log.warn("Invalid tsquery '{}', falling back to plainto_tsquery: {}", tsquery, e.getMessage());
                docids.addAll(jdbc.query(sql + "plainto_tsquery(lang::regconfig, ?)", (rs, i) -> rs.getLong("docid"), wordlist));
            }
        }

        return(fetchByIds(new ArrayList<>(docids), fldid));
    }


    private String lexicalToTsQuery(String[] lexical)
    {
        List<String> items = new ArrayList<>();

        for (String entry : lexical)
        {
            List<String> alternatives = new ArrayList<>();
            for (String alt : entry.split("\\|"))
            {
                String[] words = alt.trim().replaceAll("[^\\p{L}\\p{N}_ -]", "").trim().split("\\s+");
                List<String> lexemes = Arrays.stream(words).filter(w -> !w.isEmpty()).collect(Collectors.toList());
                if (!lexemes.isEmpty()) alternatives.add(String.join(" <-> ", lexemes));
            }
            if (alternatives.isEmpty()) continue;
            items.add(alternatives.size() > 1 ? "(" + String.join(" | ", alternatives) + ")" : alternatives.get(0));
        }

        return(String.join(" & ", items));
    }


    public boolean update(long id, DocumentRecord record, GeminiService geminiService)
    {
        StringBuilder sql = new StringBuilder
        (
            "UPDATE " + docs() + " SET fldid=?, date=?, title=?, text=?"
        );

        if (record.getFile() != null) sql.append(", extref=?");
        if (record.getContent() != null) sql.append(", content=?");

        sql.append(" WHERE id=?");
        String finalSql = sql.toString();

        int rows = jdbc.update(con ->
        {
            PreparedStatement ps = con.prepareStatement(finalSql);
            int p = 1;
            ps.setLong(p++, record.getFldid());
            ps.setDate(p++, record.getDate());
            ps.setString(p++, record.getTitle());
            ps.setString(p++, record.getText());
            if (record.getFile() != null) ps.setString(p++, record.getFile());
            if (record.getContent() != null) ps.setBytes(p++, record.getContent());
            ps.setLong(p, id);
            return(ps);
        });

        if (rows == 0) return(false);

        if (!record.getTextChunks().isEmpty())
        {
            jdbc.update("DELETE FROM " + chunks() + " WHERE docid = ?", id);
            record.createEmbeddings(geminiService);
            insertChunks(id, record);
        }

        return(true);
    }


    private DocumentRecord findByIdFiltered(long id, long fldid)
    {
        List<DocumentRecord> results;

        if (fldid > 0)
        {
            String sql =
                "SELECT d.id, d.fldid, d.date, d.title, d.text, d.extref, (d.content IS NOT NULL) AS has_content" +
                " FROM " + docs() + " d WHERE d.id = ? AND" +
                " (d.fldid = ? OR EXISTS (SELECT 1 FROM " + links() + " WHERE docid = d.id AND fldid = ?))";
            results = jdbc.query(sql, (rs, i) -> mapRow(rs), id, fldid, fldid);
        }
        else
        {
            String sql = "SELECT id, fldid, date, title, text, extref, (content IS NOT NULL) AS has_content" +
                         " FROM " + docs() + " WHERE id = ?";
            results = jdbc.query(sql, (rs, i) -> mapRow(rs), id);
        }

        return(results.isEmpty() ? null : results.get(0));
    }


    private List<DocumentRecord> fetchByIds(List<Long> docids, long fldid)
    {
        List<DocumentRecord> results = new ArrayList<>();

        for (Long docid : docids)
        {
            DocumentRecord doc = findByIdFiltered(docid, fldid);
            if (doc != null) results.add(doc);
        }

        return(results);
    }


    public DocumentRecord findByIdWithLang(long id)
    {
        String sql =
            "SELECT d.id, d.fldid, d.date, d.title, d.text, d.extref, " +
            "(d.content IS NOT NULL) AS has_content, t.lang " +
            "FROM " + docs() + " d LEFT JOIN " + chunks() + " t ON t.docid = d.id " +
            "WHERE d.id = ? LIMIT 1";

        List<DocumentRecord> results = jdbc.query(sql, (rs, i) ->
        {
            DocumentRecord doc = mapRow(rs);
            doc.setLang(rs.getString("lang"));
            return(doc);
        }, id);

        return(results.isEmpty() ? null : results.get(0));
    }


    private void insertChunks(long docid, DocumentRecord record)
    {
        String lang = record.getLang();

        ArrayList<String> sections = record.getSections();
        ArrayList<String> chunkList = record.getTextChunks();
        ArrayList<Float[]> embeddings = record.getEmbeddings();

        for (int i = 0; i < chunkList.size(); i++)
        {
            Float[] emb = embeddings.get(i);
            if (emb == null || emb.length == 0)
            {
                log.warn("Skipping chunk {} for docid={}: embedding is empty", i + 1, docid);
                continue;
            }

            int line = i + 1;
            String section = sections.get(i);
            String chunk = chunkList.get(i);
            String vecStr = toVectorLiteral(emb);
            String fLang = lang;

            jdbc.update(con ->
            {
                PreparedStatement ps = con.prepareStatement
                (
                    "INSERT INTO " + chunks() + " (docid, line, section, lang, text, embedding) VALUES (?, ?, ?, ?, ?, ?::vector)"
                );

                ps.setLong(1, docid);
                ps.setInt(2, line);
                ps.setString(3, section);
                ps.setString(4, fLang);
                ps.setString(5, chunk);
                ps.setObject(6, vecStr, Types.OTHER);

                return(ps);
            });
        }
    }


    private DocumentRecord mapRow(ResultSet rs) throws SQLException
    {
        DocumentRecord doc = new DocumentRecord();

        doc.setId(rs.getLong("id"));
        doc.setFldid(rs.getLong("fldid"));
        doc.setDate(rs.getDate("date"));
        doc.setText(rs.getString("text"));
        doc.setTitle(rs.getString("title"));
        doc.setFile(rs.getString("extref"));
        if (rs.getBoolean("has_content")) doc.setContent(new byte[0]);

        return(doc);
    }


    private DocumentRecord mapRowWithLink(ResultSet rs) throws SQLException
    {
        DocumentRecord doc = mapRow(rs);
        doc.setIsLink(rs.getBoolean("is_link"));
        long linkId = rs.getLong("link_id");
        if (!rs.wasNull()) doc.setLinkId(linkId);
        return(doc);
    }


    private String toVectorLiteral(Float[] vector)
    {
        if (vector == null || vector.length == 0) return("[]");

        return
        (
            "[" + Arrays.stream(vector)
            .map(String::valueOf)
            .collect(Collectors.joining(",")) + "]"
        );
    }


    private String docs()
    {
        return(db.getTenant() + ".documents");
    }


    private String chunks()
    {
        return(db.getTenant() + ".textchunks");
    }


    private String links()
    {
        return(db.getTenant() + ".links");
    }
}
