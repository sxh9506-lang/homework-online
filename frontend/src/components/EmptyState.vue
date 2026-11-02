<template>
  <div class="empty">
    <el-icon><component :is="icon" /></el-icon>
    <div class="t">{{ title }}</div>
    <div class="d">{{ desc }}</div>
    <slot />
  </div>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue'

defineProps({
  icon: { type: [String, Object], default: () => Document },
  title: { type: String, default: '暂无数据' },
  desc: { type: String, default: '' }
})
</script>

<style scoped>
.empty .t { font-size: 14px; color: var(--ink-2); }
.empty .d { font-size: 12.5px; margin-top: 6px; }
.empty :slotted(button) { margin-top: 14px; }
</style>
