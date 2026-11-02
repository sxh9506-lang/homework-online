<template>
  <div class="file-uploader">
    <el-upload
      drag
      :show-file-list="false"
      :http-request="customUpload"
      :multiple="max > 1"
      :accept="accept"
      :disabled="disabled || modelValue.length >= max"
    >
      <div class="upload-inner">
        <el-icon class="upload-ico"><UploadFilled /></el-icon>
        <div class="upload-tip">
          将文件拖到此处，或 <em>点击上传</em>
        </div>
        <div class="upload-desc">{{ tip || `支持图片、PDF、Word、压缩包等，单个文件不超过 20MB，最多 ${max} 个` }}</div>
      </div>
    </el-upload>

    <div v-if="modelValue.length" class="file-chips">
      <div v-for="f in modelValue" :key="f.id" class="file-chip">
        <el-icon class="ico"><Document /></el-icon>
        <a :href="fileApi.url(f.id)" target="_blank" class="name" :title="f.name">{{ f.name }}</a>
        <span class="size">{{ fileSize(f.size) }}</span>
        <el-icon v-if="!disabled" class="del" @click="remove(f)"><Close /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, Close } from '@element-plus/icons-vue'
import { fileApi } from '../api'
import { fileSize } from '../utils/format'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 5 },
  tip: { type: String, default: '' },
  accept: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'change'])

const MAX_SIZE = 20 * 1024 * 1024

async function customUpload({ file, onSuccess, onError }) {
  if (file.size > MAX_SIZE) {
    ElMessage.warning('单个文件不能超过 20MB')
    onError(new Error('too large'))
    return
  }
  try {
    const [uploaded] = await fileApi.upload([file])
    const list = [...props.modelValue, uploaded]
    emit('update:modelValue', list)
    emit('change', list)
    onSuccess(uploaded)
  } catch (e) {
    onError(e)
  }
}

function remove(file) {
  const list = props.modelValue.filter((f) => f.id !== file.id)
  emit('update:modelValue', list)
  emit('change', list)
}
</script>

<style scoped>
.upload-inner { padding: 12px 0 8px; }
.upload-ico { font-size: 40px; color: #a8b2cc; }
.upload-tip { margin-top: 8px; font-size: 13.5px; color: var(--ink-2); }
.upload-tip em { color: var(--el-color-primary); font-style: normal; font-weight: 600; }
.upload-desc { font-size: 12px; color: var(--ink-3); margin-top: 6px; }
.file-chips { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; }
.file-chip {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border: 1px solid var(--line); border-radius: 10px;
  background: #fbfcff; font-size: 13px;
}
.file-chip .ico { color: var(--el-color-primary); }
.file-chip .name {
  color: var(--ink-2); text-decoration: none; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap; flex: 1;
}
.file-chip .name:hover { color: var(--el-color-primary); text-decoration: underline; }
.file-chip .size { color: var(--ink-3); font-size: 12px; }
.file-chip .del { cursor: pointer; color: #b6bece; }
.file-chip .del:hover { color: var(--el-color-danger); }
</style>
