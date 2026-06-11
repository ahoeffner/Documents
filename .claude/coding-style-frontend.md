# JavaScript / TypeScript / Vue Coding Conventions

Apply these rules to every `.ts` and `.vue` file written or edited in this project (script blocks).

## Brace style — Allman

Opening braces on their own line for every block (function, if, else, for, …):

```ts
function startResize(e: MouseEvent)
{
  if (width.value > max)
  {
    width.value = max;
  }
}
```

If both the if-branch and else-branch are each a single short line, drop the braces:

```ts
if (doc.hasFile) openOrDownload(doc.id, doc.filename, msg)
```

Arrow functions used as short inline callbacks (`.map(x => x.id)`, watchers, etc.) keep braces on the same line — Allman applies to named function/method bodies and control-flow blocks, not one-line lambdas.

## Return style — always use parentheses

```ts
return(false)
return(!!ext && VIEWABLE_EXTENSIONS.has(ext))
```

Single-line guard returns are fine on one line:

```ts
if (!filename) return(false)
```

## Imports — sorted by line length, shortest first

No grouping — one flat block sorted ascending by character count:

```ts
import { ref } from 'vue'
import type { DocumentResult } from '../types'
import { useI18nStore } from '../stores/i18n'
import { openOrDownload } from '../utils/file'
```

## Function declarations — always one line

Never split a function signature across multiple lines, unless length exceeds 80 characters.

## Spacing

Two blank lines after the import block, after top-level const/state declarations, and between functions:

```ts
import { ref } from 'vue'
import { useI18nStore } from '../stores/i18n'


const i18n = useI18nStore()
const loading = ref(false)


function load()
{
  loading.value = true
}


function reset()
{
  loading.value = false
}
```

## Unused imports — never allowed

Remove every import not referenced in the file. Do not leave commented-out imports.
