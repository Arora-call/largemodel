<template>
  <div class="knowledge-page">
    <h1 class="page-title">📚 知识库</h1>
    <p class="section-subtitle">管理文档知识，为 AI 代码生成提供上下文增强</p>

    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文档..."
          prefix-icon="Search"
          class="search-input"
          clearable
        />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Upload" @click="showUpload = true">
          上传文档
        </el-button>
      </div>
    </div>

    <!-- 知识库统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--accent-bg); color: var(--accent)">
          <el-icon :size="20"><Document /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">--</div>
          <div class="stat-card__label">文档总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--success-bg); color: var(--success)">
          <el-icon :size="20"><Collection /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">--</div>
          <div class="stat-card__label">知识库集合</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--warning-bg); color: var(--warning)">
          <el-icon :size="20"><Connection /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">--</div>
          <div class="stat-card__label">向量片段</div>
        </div>
      </div>
    </div>

    <!-- 语义搜索 -->
    <div class="semantic-search">
      <el-input
        v-model="semanticQuery"
        placeholder="输入问题，语义检索相关知识片段..."
        size="large"
        clearable
        @keyup.enter="handleSemanticSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button type="primary" :icon="Search" @click="handleSemanticSearch">
            检索
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 文档列表 -->
    <el-card class="content-card">
      <template #header>
        <div class="card-header">
          <span>文档列表</span>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="pdf">PDF</el-radio-button>
            <el-radio-button value="markdown">Markdown</el-radio-button>
            <el-radio-button value="txt">TXT</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="empty-state">
        <el-icon :size="48"><FolderOpened /></el-icon>
        <p style="margin-top: 12px; font-size: 15px; color: var(--text-secondary)">
          知识库功能即将上线
        </p>
        <p>上传技术文档、API 参考、代码示例等，AI 将自动检索相关上下文生成更准确的代码</p>
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUpload" title="上传文档" width="500px">
      <el-upload
        class="upload-area"
        drag
        action="/api/knowledge/documents"
        :headers="uploadHeaders"
      >
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div style="margin-top: 12px; color: var(--text-secondary)">
          拖拽文件到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div style="margin-top: 8px; color: var(--text-dim); font-size: 12px">
            支持 PDF、Markdown、TXT 格式，单个文件不超过 10MB
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" @click="showUpload = false">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search, Upload, Document, Collection, Connection, FolderOpened, UploadFilled } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const semanticQuery = ref('')
const viewMode = ref('all')
const showUpload = ref(false)

const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('token')}`
}

function handleSemanticSearch() {
  // TODO: 接入知识库语义检索 API
}
</script>

<style scoped>
.knowledge-page {
  max-width: 1200px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.toolbar-left {
  flex: 1;
  max-width: 360px;
}

.search-input {
  width: 100%;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.semantic-search {
  margin-bottom: 24px;
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

.upload-area {
  width: 100%;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
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
