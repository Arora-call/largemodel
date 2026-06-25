<template>
  <div class="agents-page">
    <div class="page-container">
      <!-- 头部 -->
      <div class="page-header">
        <div>
          <h2>🤖 Agent 工作流</h2>
          <p class="subtitle">5 个 AI Agent 链式协作：需求分析 → 架构设计 → 代码生成 → 测试生成 → 代码审查</p>
        </div>
        <el-button type="primary" @click="createVisible = true">
          ✨ 新建工作流
        </el-button>
      </div>

      <!-- 流水线示意图 -->
      <div class="pipeline-viz">
        <div v-for="(a, i) in agentStages" :key="a.key" class="pipe-stage">
          <div class="pipe-icon">{{ a.icon }}</div>
          <span class="pipe-name">{{ a.label }}</span>
          <div v-if="i < agentStages.length - 1" class="pipe-arrow">→</div>
        </div>
      </div>

      <!-- 工作流列表 -->
      <div v-if="workflows.length" class="wf-list">
        <div v-for="wf in workflows" :key="wf.id" class="wf-card" @click="selectWorkflow(wf)">
          <div class="wf-left">
            <span :class="['wf-status', statusClass(wf.status)]">{{ statusLabel(wf.status) }}</span>
          </div>
          <div class="wf-body">
            <h4 class="wf-name">{{ wf.name }}</h4>
            <p class="wf-req">{{ (wf.requirement || '').substring(0, 120) || '无描述' }}</p>
            <div class="wf-meta">
              <span>{{ wf.agentChain || 'analyzer,architect,coder,tester,reviewer' }}</span>
              <span>{{ formatDate(wf.updatedAt) }}</span>
            </div>
          </div>
          <div class="wf-actions" @click.stop>
            <el-button v-if="wf.status === 'COMPLETED' || wf.status === 'FAILED'"
              size="small" @click="viewResult(wf)">📋 查看</el-button>
            <el-button v-if="wf.status !== 'RUNNING' && wf.status !== 'COMPLETED' && wf.status !== 'FAILED'"
              size="small" type="primary" @click="executeWorkflow(wf)">▶ 执行</el-button>
            <el-button v-if="wf.status === 'RUNNING'" size="small" type="warning" disabled>⏳ 运行中</el-button>
            <el-button size="small" text type="danger" @click="confirmDelete(wf)">🗑</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无工作流，点击新建工作流开始" :image-size="80" />

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-area">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
          :total="total" layout="total, prev, pager, next" small @current-change="fetchList" />
      </div>
    </div>

    <!-- SSE 执行面板 -->
    <el-dialog v-model="execVisible" :title="'执行: ' + (execWf?.name || '')" width="720px" top="3vh"
      :close-on-click-modal="false">
      <div v-if="phases.length" class="exec-phases">
        <div v-for="p in phases" :key="p.phase" :class="['exec-phase', p.status]">
          <div class="phase-header">
            <span class="phase-num">{{ agentStages[p.phase - 1]?.icon || '🤖' }} 阶段 {{ p.phase }}/{{ phases.length }}
              — {{ agentStages[p.phase - 1]?.label || p.agent }}</span>
            <span :class="['phase-status-tag', p.status]">
              {{ p.status === 'running' ? '⏳ 执行中' : p.status === 'done' ? '✅ 完成' : '⏸ 等待' }}
            </span>
          </div>
          <div v-if="p.output" class="phase-output markdown" v-html="renderMd(p.output)"></div>
        </div>
      </div>
      <div v-if="execError" class="exec-error">❌ {{ execError }}</div>
      <template #footer>
        <el-button @click="execVisible = false">关闭</el-button>
        <el-button v-if="execDone" type="primary" @click="execVisible = false; fetchList()">完成</el-button>
      </template>
    </el-dialog>

    <!-- 查看结果弹窗 -->
    <el-dialog v-model="resultVisible" width="780px" top="3vh" destroy-on-close>
      <template #header>
        <div class="result-dialog-header">
          <span class="result-dialog-title">📋 {{ resultWf?.name || '执行结果' }}</span>
          <div class="result-dialog-actions">
            <el-button size="small" @click="downloadMarkdown(resultWf)">📥 导出 Markdown</el-button>
            <el-button v-if="resultWf?.status !== 'RUNNING'" size="small" type="primary"
              @click="resultVisible = false; executeWorkflow(resultWf)">🔄 重新执行</el-button>
          </div>
        </div>
      </template>
      <div v-if="resultWf?.result" class="result-content markdown" v-html="renderMd(resultWf.result)"></div>
      <el-empty v-else description="暂无结果" :image-size="60" />
      <template #footer>
        <el-button @click="resultVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新建工作流弹窗 -->
    <el-dialog v-model="createVisible" title="新建工作流" width="520px" top="12vh">
      <el-form label-position="top">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：电商系统开发" maxlength="100" />
        </el-form-item>
        <el-form-item label="需求描述">
          <el-input v-model="form.requirement" type="textarea" :rows="4"
            placeholder="详细描述你的需求，Agent 链会基于此协作..." />
        </el-form-item>
        <el-form-item label="Agent 链">
          <el-input v-model="form.agentChain" placeholder="analyzer,architect,coder,tester,reviewer" />
          <span class="form-hint">逗号分隔，可选: analyzer, architect, coder, tester, reviewer</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :disabled="!form.name.trim()">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWorkflows, createWorkflow, getWorkflow, deleteWorkflow, executeWorkflowStream } from '@/api/agents'
import markdownit from 'markdown-it'

const md = markdownit({ html: true, breaks: true, linkify: true })

const agentStages = [
  { key: 'analyzer', icon: '📋', label: '需求分析' },
  { key: 'architect', icon: '🏗️', label: '架构设计' },
  { key: 'coder', icon: '💻', label: '代码生成' },
  { key: 'tester', icon: '🧪', label: '测试生成' },
  { key: 'reviewer', icon: '🔍', label: '代码审查' },
]

const workflows = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(8)

const createVisible = ref(false)
const form = ref({ name: '', requirement: '', agentChain: 'analyzer,architect,coder,tester,reviewer' })

const execVisible = ref(false)
const execWf = ref(null)
const phases = ref([])
const execError = ref('')
const execDone = ref(false)

const resultVisible = ref(false)
const resultWf = ref(null)

const statusClass = s => ({ PENDING: 's-pending', RUNNING: 's-running', COMPLETED: 's-done', FAILED: 's-failed' }[s] || '')
const statusLabel = s => ({ PENDING: '等待中', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败' }[s] || s)

function formatDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN') : '' }
function renderMd(text) { return text ? md.render(text) : '' }

async function fetchList() {
  try {
    const res = await listWorkflows({ page: currentPage.value - 1, size: pageSize.value })
    workflows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* silent */ }
}

async function handleCreate() {
  try {
    await createWorkflow({
      name: form.value.name,
      requirement: form.value.requirement,
      agentChain: form.value.agentChain,
      description: form.value.requirement?.substring(0, 200) || '',
    })
    ElMessage.success('创建工作流成功')
    createVisible.value = false
    form.value = { name: '', requirement: '', agentChain: 'analyzer,architect,coder,tester,reviewer' }
    fetchList()
  } catch { ElMessage.error('创建失败') }
}

function downloadMarkdown(wf) {
  if (!wf?.result) return
  const md = `# ${wf.name || 'Agent 工作流'}\n\n> 状态: ${wf.status} | 创建: ${formatDate(wf.createdAt)}\n\n---\n\n${wf.result}`
  const blob = new Blob([md], { type: 'text/markdown' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = (wf.name || 'workflow') + '.md'
  a.click()
  URL.revokeObjectURL(a.href)
}

async function viewResult(wf) {
  try {
    const res = await getWorkflow(wf.id)
    resultWf.value = res.data || wf
  } catch {
    resultWf.value = wf
  }
  resultVisible.value = true
}

function selectWorkflow(wf) {
  // 显示执行面板
  execWf.value = wf
  execVisible.value = true
  phases.value = []
  execError.value = ''
  execDone.value = false
}

function executeWorkflow(wf) {
  execWf.value = wf
  execVisible.value = true
  execError.value = ''
  execDone.value = false
  // 初始化阶段
  const agents = (wf.agentChain || 'analyzer,architect,coder,tester,reviewer').split(',')
  phases.value = agents.map((a, i) => ({
    phase: i + 1,
    agent: a.trim(),
    status: 'pending',
    output: '',
  }))

  executeWorkflowStream(wf.id, {}, {
    onPhase: (data) => {
      const idx = data.phase - 1
      if (idx >= 0 && idx < phases.value.length) {
        phases.value[idx].status = 'running'
      }
    },
    onProgress: (data) => {
      const idx = data.phase - 1
      if (idx >= 0 && idx < phases.value.length) {
        phases.value[idx].output += data.token || ''
      }
    },
    onDone: (data) => {
      phases.value.forEach(p => {
        if (p.status === 'running') p.status = 'done'
      })
      execDone.value = true
      if (data.status === 'FAILED') execError.value = '工作流执行失败，请查看各阶段输出'
    },
    onError: (err) => {
      execError.value = err.message || '执行出错'
      phases.value.forEach(p => {
        if (p.status === 'running') p.status = 'done'
      })
      execDone.value = true
    },
  })
}

async function confirmDelete(wf) {
  try {
    await ElMessageBox.confirm(`确定删除「${wf.name}」？`, '确认删除', { type: 'warning' })
    await deleteWorkflow(wf.id)
    ElMessage.success('已删除')
    fetchList()
  } catch { /* cancel */ }
}

onMounted(fetchList)
</script>

<style scoped>
.agents-page { padding: 24px 0; display: flex; justify-content: center; }
.page-container { width: 100%; max-width: 900px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 22px; color: var(--text-heading); }
.subtitle { color: var(--text-dim); font-size: 13px; margin: 4px 0 0; }

/* 流水线可视化 */
.pipeline-viz { display: flex; align-items: center; gap: 0; padding: 16px 20px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius); margin-bottom: 20px; overflow-x: auto; }
.pipe-stage { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.pipe-icon { font-size: 22px; }
.pipe-name { font-size: 12px; color: var(--text-dim); white-space: nowrap; }
.pipe-arrow { margin: 0 12px; color: var(--accent); font-size: 16px; font-weight: bold; }

/* 工作流列表 */
.wf-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.wf-card { display: flex; align-items: center; gap: 14px; padding: 14px 16px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius); cursor: pointer; transition: all .15s; }
.wf-card:hover { border-color: var(--accent); }
.wf-left { flex-shrink: 0; }
.wf-status { font-size: 11px; padding: 3px 10px; border-radius: 10px; font-weight: 500; white-space: nowrap; }
.s-pending { background: rgba(251,191,36,.15); color: #fbbf24; }
.s-running { background: rgba(124,138,255,.15); color: var(--accent); }
.s-done { background: rgba(52,211,153,.15); color: #34d399; }
.s-failed { background: rgba(248,81,73,.15); color: #f85149; }
.wf-body { flex: 1; min-width: 0; }
.wf-name { margin: 0 0 4px; font-size: 14px; color: var(--text-heading); }
.wf-req { margin: 0 0 6px; font-size: 12px; color: var(--text-dim); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wf-meta { display: flex; gap: 12px; font-size: 11px; color: var(--text-dim); }
.wf-actions { flex-shrink: 0; display: flex; gap: 6px; align-items: center; }

.form-hint { font-size: 11px; color: var(--text-dim); margin-top: 4px; display: block; }

/* 执行面板 */
.exec-phases { display: flex; flex-direction: column; gap: 10px; }
.exec-phase { border: 1px solid var(--border-color); border-radius: var(--radius); overflow: hidden; }
.exec-phase.running { border-color: var(--accent); box-shadow: 0 0 8px rgba(124,138,255,.15); }
.exec-phase.done { border-color: #34d399; }
.phase-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 14px; background: var(--bg-card); font-size: 13px; }
.phase-num { color: var(--text-primary); font-weight: 500; }
.phase-status-tag { font-size: 11px; padding: 2px 8px; border-radius: 8px; }
.phase-status-tag.pending { background: rgba(107,114,128,.15); color: var(--text-dim); }
.phase-status-tag.running { background: rgba(124,138,255,.15); color: var(--accent); animation: pulse 2s infinite; }
.phase-status-tag.done { background: rgba(52,211,153,.1); color: #34d399; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: .5; } }
.phase-output { padding: 12px 14px; font-size: 13px; line-height: 1.7; color: var(--text-secondary); max-height: 260px; overflow-y: auto; }
.phase-output :deep(pre) { background: var(--bg-code); padding: 10px; border-radius: 6px; overflow-x: auto; font-size: 12px; }
.phase-output :deep(code) { font-family: 'Fira Code', monospace; font-size: 12px; }

.exec-error { padding: 12px; color: #f85149; font-size: 13px; background: rgba(248,81,73,.08); border-radius: var(--radius); margin-top: 12px; }

.pagination-area { display: flex; justify-content: center; }

.result-dialog-header { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.result-dialog-title { font-size: 16px; font-weight: 600; color: var(--text-heading); }
.result-dialog-actions { display: flex; gap: 8px; flex-shrink: 0; }
</style>
