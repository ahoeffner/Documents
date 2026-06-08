# Java Coding Conventions

Apply these rules to every Java file written or edited in this project.

## Brace style — Allman

Opening braces on their own line for every block (class, method, if, else, for, …):

```java
public List<Category> list()
{
    if (name != null)
    {
        sql += " WHERE name = ?";
    }
}
```

If both the if-branch and else-branch are each a single short line, drop the braces:

```java
if (name != null) sql += " WHERE name = ?";
```

## Return style — always use parentheses

```java
return(null);
return(results.isEmpty() ? null : results.get(0));
return(ResponseEntity.ok(result));
```

Single-line guard returns are fine on one line:

```java
if (records.isEmpty()) return(null);
```

## Imports — sorted by line length, shortest first

No package grouping — one flat block sorted ascending by character count:

```java
import java.util.List;
import rag.dochandler.model.Category;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import rag.dochandler.repository.CategoryRepository;
```

## Method declarations — always one line

Never split a method signature across multiple lines, unless length exceeds 80 characters:

```java
public DocumentRecord process(String date, String fldidStr, String title, String text, String language, MultipartFile file, String url) throws Exception
{
```

Not:

```java
public DocumentRecord process(String date, String fldidStr, String title, String text,
                              String language, MultipartFile file, String url) throws Exception
{
```

## Spacing

Two blank lines after the import block, after class fields, and between methods:

```java
import java.util.List;
import rag.dochandler.entities.Category;
import rag.dochandler.repository.CategoryRepository;


@Service
public class CategoryService
{
    private final CategoryRepository repo;


    public CategoryService(CategoryRepository repo)
    {
        this.repo = repo;
    }


    public List<Category> list()
    {
        return(repo.findAll().stream()
            .map(c -> new Category(c.getId(), c.getName()))
            .toList());
    }
}
```

## Unused imports — never allowed

Remove every import not referenced in the file. Do not leave commented-out imports.

## `application.properties` / `application.yaml` — never create unless asked

If these files are absent, leave them absent. Do not generate them to fix a missing-property error — solve it another way.
