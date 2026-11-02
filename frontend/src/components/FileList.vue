<template>
  <div v-if="files && files.length" class="file-list">
    <div v-for="f in files" :key="f.id" class="file-item" @click="open(f)">
      <div v-if="isImage(f)" class="thumb">
        <img :src="fileApi.url(f.id)" :alt="f.name" />
      </div>
      <div v-else class="thumb doc"><el-icon><Document /></el-icon></div>
      <div class="info">
        <div class="name">{{ f.name }}</div>
        <div class="meta">{{ f.contentType || '文件' }} · {{ fileSize(f.size) }}</div>
      </div>
      <a class="download" :href="fileApi.url(f.id, true)" @click.stop><el-icon><Download /></el-icon></a>
    </div>

    <el-image-viewer
      v-if="preview"
      :url-list="imageUrls"
      :initial-index="previewIndex"
      hide-on-click-modal
      @close="preview = false"
    />
  </div>
  <div v-else class="muted small">暂无附件</div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Document, Download } from '@element-plus/icons-vue'
import { fileApi } from '../api'
import { fileSize } from '../utils/format'

const props = defineProps({ files: { type: Array, default: () => [] } })

const preview = ref(false)
const previewIndex = ref(0)
const isImage = (f) => (f.contentType || '').startsWith('image/')
const images = computed(() => props.files.filter(isImage))
const imageUrls = computed(() => images.value.map((f) => fileApi.url(f.id)))

function open(f) {
  if (isImage(f)) {
    previewIndex.value = Math.max(0, images.value.findIndex((x) => x.id === f.id))
    preview.value = true
  } else {
    window.open(fileApi.url(f.id), '_blank')
  }
}
</script>

<style scoped>
.file-list { display: flex; flex-direction: column; gap: 10px; }
.file-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px;
  border: 1px solid var(--line); border-radius: 10px; background: #fbfcff; cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.file-item:hover { border-color: var(--el-color-primary-light-5); background: #f7f8ff; }
.thumb {
  width: 46px; height: 46px; border-radius: 8px; overflow: hidden; flex-shrink: 0;
  background: #eef0ff; display: grid; place-items: center;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.thumb.doc { color: var(--el-color-primary); font-size: 22px; }
.info { min-width: 0; flex: 1; }
.name { font-size: 13.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { font-size: 11.5px; color: var(--ink-3); margin-top: 3px; }
.download { color: var(--ink-3); padding: 6px; border-radius: 6px; }
.download:hover { color: var(--el-color-primary); background: #eef0ff; }
</style>
