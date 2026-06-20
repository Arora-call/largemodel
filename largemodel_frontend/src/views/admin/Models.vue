<template>
  <div class="models-page">
    <h1 class="page-title">⚙ 模型配置</h1>
    <p class="section-subtitle">管理 AI 大模型接入配置，支持多模型切换与参数调优</p>

    <!-- 模型列表 -->
    <div class="model-grid">
      <div class="model-card" v-for="model in models" :key="model.id">
        <div class="model-header">
          <div class="model-icon" :style="{ background: model.color }">
            <el-icon :size="24"><Box /></el-icon>
          </div>
          <div class="model-info">
            <div class="model-name">{{ model.name }}</div>
            <div class="model-provider">{{ model.provider }}</div>
          </div>
          <el-switch v-model="model.enabled" size="small" />
        </div>
        <div class="model-body">
          <div class="model-param">
            <span class="param-label">Temperature</span>
            <span class="param-value">{{ model.temperature }}</span>
          </div>
          <div class="model-param">
            <span class="param-label">Max Tokens</span>
            <span class="param-value">{{ model.maxTokens }}</span>
          </div>
          <div class="model-param">
            <span class="param-label">API Endpoint</span>
            <span class="param-value mono">{{ model.endpoint }}</span>
          </div>
        </div>
        <div class="model-footer">
          <el-button size="small" @click="handleEdit(model)">配置</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(model)">删除</el-button>
        </div>
      </div>
    </div>

    <!-- 添加模型 -->
    <el-card class="add-card">
      <div class="add-content">
        <el-icon :size="36"><Plus /></el-icon>
        <p style="color: var(--text-secondary); margin: 8px 0">接入新模型</p>
        <el-button type="primary" :icon="Plus">添加模型</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Box, Plus } from '@element-plus/icons-vue'

const models = ref([
  {
    id: 1,
    name: 'DeepSeek V4',
    provider: 'DeepSeek',
    endpoint: 'https://api.deepseek.com/v1',
    temperature: 0.7,
    maxTokens: 16384,
    enabled: true,
    color: 'linear-gradient(135deg, #7c8aff, #6366f1)'
  },
  {
    id: 2,
    name: 'GLM-4.5-air',
    provider: 'Zhipu AI',
    endpoint: 'https://open.bigmodel.cn/api/paas/v4',
    temperature: 0.7,
    maxTokens: 8192,
    enabled: false,
    color: 'linear-gradient(135deg, #34d399, #10b981)'
  }
])

function handleEdit(model) {
  // TODO: 编辑模型配置
}

function handleDelete(model) {
  // TODO: 删除模型
}
</script>

<style scoped>
.models-page {
  max-width: 1200px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.model-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  overflow: hidden;
  transition: all var(--transition);
}

.model-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow);
}

.model-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.model-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.model-info {
  flex: 1;
  min-width: 0;
}

.model-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-heading);
}

.model-provider {
  font-size: 12px;
  color: var(--text-dim);
}

.model-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.model-param {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.param-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.param-value {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.param-value.mono {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-dim);
}

.model-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.add-card {
  background: var(--bg-card) !important;
  border: 1px dashed var(--border-color) !important;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all var(--transition);
}

.add-card:hover {
  border-color: var(--border-hover) !important;
  background: var(--bg-hover) !important;
}

.add-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: var(--text-dim);
}
</style>
