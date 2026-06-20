<template>
  <div class="agents-page">
    <h1 class="page-title">🤖 Agent 工作流</h1>
    <p class="section-subtitle">多 Agent 协作编排，将复杂任务拆解为多个 AI Agent 协同完成</p>

    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索工作流..."
          prefix-icon="Search"
          class="search-input"
          clearable
        />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          创建工作流
        </el-button>
      </div>
    </div>

    <!-- Agent 角色介绍 -->
    <div class="agent-roles">
      <div class="role-card">
        <div class="role-icon" style="background: rgba(124, 138, 255, 0.15)">
          <el-icon :size="22"><Reading /></el-icon>
        </div>
        <div class="role-name">需求分析</div>
        <div class="role-desc">解析用户需求，生成结构化规格说明</div>
      </div>
      <el-icon :size="20" class="role-arrow" color="var(--text-dim)"><Right /></el-icon>
      <div class="role-card">
        <div class="role-icon" style="background: rgba(167, 139, 250, 0.15)">
          <el-icon :size="22"><Box /></el-icon>
        </div>
        <div class="role-name">架构设计</div>
        <div class="role-desc">设计系统架构、技术选型与模块划分</div>
      </div>
      <el-icon :size="20" class="role-arrow" color="var(--text-dim)"><Right /></el-icon>
      <div class="role-card">
        <div class="role-icon" style="background: rgba(52, 211, 153, 0.15)">
          <el-icon :size="22"><Edit /></el-icon>
        </div>
        <div class="role-name">代码生成</div>
        <div class="role-desc">按设计生成高质量代码实现</div>
      </div>
      <el-icon :size="20" class="role-arrow" color="var(--text-dim)"><Right /></el-icon>
      <div class="role-card">
        <div class="role-icon" style="background: rgba(251, 191, 36, 0.15)">
          <el-icon :size="22"><Checked /></el-icon>
        </div>
        <div class="role-name">测试生成</div>
        <div class="role-desc">自动生成单元测试与集成测试</div>
      </div>
      <el-icon :size="20" class="role-arrow" color="var(--text-dim)"><Right /></el-icon>
      <div class="role-card">
        <div class="role-icon" style="background: rgba(239, 68, 68, 0.15)">
          <el-icon :size="22"><Search /></el-icon>
        </div>
        <div class="role-name">代码审查</div>
        <div class="role-desc">安全审查 + 性能优化建议</div>
      </div>
    </div>

    <!-- 工作流列表 -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>工作流列表</span>
          <el-tag type="primary" size="small">0 个工作流</el-tag>
        </div>
      </template>

      <div class="empty-state">
        <el-icon :size="48"><Connection /></el-icon>
        <p style="margin-top: 12px; font-size: 15px; color: var(--text-secondary)">
          Agent 工作流功能即将上线
        </p>
        <p style="max-width: 480px; text-align: center">
          通过 LangGraph4j 编排多 Agent 协作，自动完成需求分析 → 架构设计 → 代码生成 → 测试 → 审查的全流程
        </p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  Search, Plus, Connection, Reading, Right, Edit, Checked, Box
} from '@element-plus/icons-vue'

const searchKeyword = ref('')

function handleCreate() {
  // TODO: 创建工作流
}
</script>

<style scoped>
.agents-page {
  max-width: 1200px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
}

.toolbar-left {
  flex: 1;
  max-width: 360px;
}

.search-input {
  width: 100%;
}

/* Agent Roles Pipeline */
.agent-roles {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  overflow-x: auto;
}

.role-card {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: var(--radius);
  transition: all var(--transition);
  min-width: 100px;
  text-align: center;
}

.role-card:hover {
  background: var(--bg-hover);
}

.role-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}

.role-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.role-desc {
  font-size: 11px;
  color: var(--text-dim);
  line-height: 1.4;
}

.role-arrow {
  flex-shrink: 0;
}

.content-card {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--text-heading);
}

@media (max-width: 768px) {
  .agent-roles {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  .role-card {
    flex-direction: row;
    justify-content: flex-start;
    gap: 12px;
    text-align: left;
    min-width: auto;
  }
  .role-arrow {
    transform: rotate(90deg);
    align-self: center;
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-left {
    max-width: none;
  }
}
</style>
