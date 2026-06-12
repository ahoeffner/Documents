<template>
  <Teleport to="body">
    <div v-if="confirm.visible" class="modal-backdrop" @click.self="confirm.resolve(false)">
      <div class="modal-popup modal-popup-sm">
        <div class="modal-header" v-if="confirm.title">
          <span class="modal-header-title">{{ confirm.title }}</span>
          <button type="button" class="modal-close" @click="confirm.resolve(false)">✕</button>
        </div>
        <div class="modal-body">
          <p class="modal-text">{{ confirm.message }}</p>
        </div>
        <div class="modal-actions">
          <button class="btn btn-ghost btn-sm" @click="confirm.resolve(false)">{{ confirm.cancelLabel || i18n.t('common.cancel') }}</button>
          <button :class="['btn', 'btn-sm', confirm.danger ? 'btn-danger' : 'btn-primary']" @click="confirm.resolve(true)">{{ confirm.confirmLabel || i18n.t('common.ok') }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useI18nStore } from '../stores/i18n'
import { useConfirmStore } from '../stores/confirm'


const i18n = useI18nStore()
const confirm = useConfirmStore()


function onKeydown(e: KeyboardEvent)
{
  if (!confirm.visible) return
  if (e.key === 'Escape') confirm.resolve(false)
  if (e.key === 'Enter') confirm.resolve(true)
}


onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>
