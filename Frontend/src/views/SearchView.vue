<template>
  <div class="search-view">

    <div class="toolbar">
      <span class="section-label">Folder</span>
      <select v-model="selectedCategory" class="select folder-select">
        <option value="0">All</option>
        <option v-for="cat in categoriesStore.categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>

      <form @submit.prevent="doSearch" class="search-form">
        <input
          ref="searchInputEl"
          v-model="query"
          type="text"
          placeholder="Search documents…"
          class="search-input"
        />
        <button type="submit" :disabled="!query.trim() || loading" class="btn btn-primary btn-lg">
          {{ loading ? 'Searching…' : 'Search' }}
        </button>
      </form>

      <span v-if="searched && !loading" class="result-count">
        {{ documents.length }} result{{ documents.length !== 1 ? 's' : '' }}
      </span>
    </div>

    <div class="results">
      <div v-if="error" class="notice notice-error" style="margin:12px">{{ error }}</div>

      <div v-else-if="loading" class="empty-state">
        <span class="spinner spinner-lg"></span>
      </div>

      <div v-else-if="!searched" class="empty-state">Enter a query to search documents</div>

      <div v-else-if="documents.length === 0" class="empty-state">
        No documents matched <em>"{{ lastQuery }}"</em>
      </div>

      <template v-else>
        <DocumentCard v-for="doc in documents" :key="doc.id" :doc="doc" />
      </template>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useCategoriesStore } from '../stores/categories'
import { search } from '../api/documents'
import DocumentCard from '../components/DocumentCard.vue'
import type { DocumentResult } from '../types'

const categoriesStore = useCategoriesStore()
const searchInputEl = ref<HTMLInputElement | null>(null)
const query = ref('')
const lastQuery = ref('')
const selectedCategory = ref(0)
const documents = ref<DocumentResult[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const searched = ref(false)

onMounted(() => categoriesStore.load())

defineExpose({ focus: () => searchInputEl.value?.focus() })

async function doSearch() {
  if (!query.value.trim()) return
  loading.value = true
  error.value = null
  lastQuery.value = query.value
  try {
    const res = await search({ query: query.value, category: selectedCategory.value })
    documents.value = (res.data.documents || []) as DocumentResult[]
    searched.value = true
  } catch {
    error.value = 'Search failed — could not reach the server.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.search-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  height: 54px;
  background: var(--bg);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.folder-select { width: auto; height: 38px; }

.search-form {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Larger / more prominent search input */
.search-input {
  flex: 1;
  height: 42px;
  padding: 0 14px;
  border: 1.5px solid var(--border-input);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 15px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-ring);
}
.search-input::placeholder { color: var(--text-faint); }

.result-count {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  font-weight: 500;
}

.results { flex: 1; overflow-y: auto; }
</style>
