<template>
  <div>
    <div
      class="tree-row"
      :class="{ selected: selectedId === folder.id }"
      :style="{ paddingLeft: depth * 14 + 8 + 'px' }"
      @click="$emit('select', folder.id)"
      @contextmenu.stop.prevent="$emit('context', { id: folder.id, e: $event })"
    >
      <span class="tree-toggle" @click.stop="open = !open">
        <svg v-if="folder.children.length" width="10" height="10" viewBox="0 0 10 10" fill="currentColor">
          <path v-if="open" d="M1 3 L5 7 L9 3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          <path v-else d="M3 1 L7 5 L3 9" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </span>

      <svg class="tree-icon" width="13" height="13" viewBox="0 0 20 20" fill="currentColor">
        <path v-if="open && folder.children.length" d="M2 6a2 2 0 012-2h4l2 2h6a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
        <path v-else d="M2 6a2 2 0 012-2h4l2 2h6a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" opacity="0.7"/>
      </svg>

      <span class="tree-name">{{ folder.name }}</span>
    </div>

    <template v-if="open">
      <FolderTreeItem
        v-for="child in folder.children"
        :key="child.id"
        :folder="child"
        :depth="depth + 1"
        :selected-id="selectedId"
        @select="$emit('select', $event)"
        @context="$emit('context', $event)"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Folder } from '../types'
import FolderTreeItem from './FolderTreeItem.vue'


defineProps<{
  folder: Folder
  depth: number
  selectedId: number | null
}>()

defineEmits<{
  select: [id: number]
  context: [payload: { id: number; e: MouseEvent }]
}>()


const open = ref(true)
</script>

<style scoped>
.tree-row {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  cursor: pointer;
  border-radius: 3px;
  color: var(--text);
  font-size: 12px;
  font-weight: 400;
  user-select: none;
}
.tree-row:hover { background: var(--bg-muted); }
.tree-row.selected { background: var(--accent-ring); color: var(--accent-dark); font-weight: 500; }

.tree-toggle {
  width: 14px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-faint);
  border-radius: 2px;
}
.tree-toggle:hover { color: var(--text-muted); }

.tree-icon {
  flex-shrink: 0;
  color: var(--warn);
}
.tree-row.selected .tree-icon { color: var(--accent); }

.tree-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
