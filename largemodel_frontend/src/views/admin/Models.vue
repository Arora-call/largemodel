<template>
  <div class="models-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">⚙ 模型配置</h1>
        <p class="section-subtitle">管理 AI 大模型接入，支持多模型切换。API Key 采用 AES-256-GCM 加密存储，前端不返回原始值</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 添加模型
      </el-button>
    </div>

    <!-- 模型卡片列表 -->
    <div class="model-grid">
      <div v-for="m in models" :key="m.id" class="model-card" :class="{ disabled: !m.isEnabled }">
        <div class="model-header">
          <div class="model-info">
            <div class="model-name">
              {{ m.name }}
              <el-tag v-if="m.isDefault" type="success" size="small" effect="dark">默认</el-tag>
              <el-tag v-if="!m.isEnabled" type="info" size="small">已禁用</el-tag>
            </div>
            <div class="model-provider">{{ m.provider }} · {{ m.modelName }}</div>
          </div>
        </div>
        <div class="model-body">
          <div class="model-param"><span class="pl">端点</span><span class="pv mono">{{ m.baseUrl }}</span></div>
          <div class="model-param"><span class="pl">API Key</span><span class="pv mono">{{ m.apiKeyMasked || '***' }}</span></div>
          <div class="model-param"><span class="pl">Temperature</span><span class="pv">{{ m.temperature }}</span></div>
          <div class="model-param"><span class="pl">Max Tokens</span><span class="pv">{{ m.maxTokens }}</span></div>
        </div>
        <div class="model-footer">
          <el-button size="small" @click="testConn(m)" :loading="testingId === m.id">测试连接</el-button>
          <el-button size="small" @click="openEdit(m)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(m)">删除</el-button>
        </div>
      </div>

      <div v-if="models.length === 0" class="empty-card" @click="openCreate">
        <el-icon :size="36"><Plus /></el-icon>
        <p>添加第一个 AI 模型</p>
      </div>
    </div>

    <!-- 编辑/新建弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模型' : '添加模型'" width="520px" top="8vh" destroy-on-close>
      <el-form :model="form" label-width="100px" label-position="left">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：DeepSeek V4" maxlength="100" />
        </el-form-item>
        <el-form-item label="提供商" required>
          <el-select v-model="form.provider" style="width:100%">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="OpenAI" value="openai" />
            <el-option label="Zhipu AI (智谱)" value="zhipu" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="API 端点" required>
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" required>
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
          <div class="form-hint">密钥将使用 AES-256-GCM 加密存储。编辑时留空则不更新。</div>
        </el-form-item>
        <el-form-item label="模型标识" required>
          <el-input v-model="form.modelName" placeholder="deepseek-v4-flash" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Max Tokens">
              <el-input-number v-model="form.maxTokens" :min="100" :max="200000" :step="1000" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="99" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用">
              <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listModels, getModel, createModel, updateModel, deleteModel, testModelConnection } from '@/api/admin'

const models = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const saving = ref(false)
const testingId = ref(null)

const defaultForm = () => ({
  name: '', provider: 'deepseek', baseUrl: '', apiKey: '', modelName: '',
  temperature: 0.7, maxTokens: 16384, sortOrder: 0, isEnabled: 1, isDefault: 0
})
const form = reactive(defaultForm())

async function fetchList() {
  try { const res = await listModels(); models.value = res.data || [] }
  catch { models.value = [] }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function openEdit(m) {
  try {
    const res = await getModel(m.id)
    const d = res.data || m
    editingId.value = m.id
    form.name = d.name || ''
    form.provider = d.provider || ''
    form.baseUrl = d.baseUrl || ''
    form.apiKey = ''   // 不回显密钥
    form.modelName = d.modelName || ''
    form.temperature = d.temperature ?? 0.7
    form.maxTokens = d.maxTokens ?? 16384
    form.sortOrder = d.sortOrder ?? 0
    form.isEnabled = d.isEnabled ?? 1
    form.isDefault = d.isDefault ?? 0
    dialogVisible.value = true
  } catch { ElMessage.error('加载模型详情失败') }
}

async function handleSubmit() {
  if (!form.name || !form.baseUrl || !form.modelName) return ElMessage.warning('请填写完整信息')
  if (!editingId.value && !form.apiKey) return ElMessage.warning('请输入 API Key')
  saving.value = true
  try {
    const data = {
      name: form.name, provider: form.provider, baseUrl: form.baseUrl,
      apiKey: form.apiKey, modelName: form.modelName,
      temperature: form.temperature, maxTokens: form.maxTokens,
      sortOrder: form.sortOrder, isEnabled: form.isEnabled, isDefault: form.isDefault
    }
    if (editingId.value) {
      await updateModel(editingId.value, data)
      ElMessage.success('模型已更新')
    } else {
      await createModel(data)
      ElMessage.success('模型已添加')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) { ElMessage.error(e.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handleDelete(m) {
  try {
    await ElMessageBox.confirm(`确定删除「${m.name}」？`, '确认删除', { type: 'warning' })
    await deleteModel(m.id)
    ElMessage.success('已删除')
    fetchList()
  } catch { /* cancelled */ }
}

async function testConn(m) {
  testingId.value = m.id
  try {
    const res = await testModelConnection(m.id)
    if (res.data?.success) ElMessage.success('连接成功')
    else ElMessage.error(res.data?.error || '连接失败: ' + (res.data?.statusCode || ''))
  } catch { ElMessage.error('测试请求失败') }
  finally { testingId.value = null }
}

onMounted(fetchList)
</script>

<style scoped>
.models-page { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.model-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 16px; }
.model-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius); overflow: hidden; transition: all var(--transition); }
.model-card:hover { border-color: var(--border-hover); box-shadow: var(--shadow); }
.model-card.disabled { opacity: 0.55; }
.model-header { display: flex; align-items: center; gap: 12px; padding: 16px 16px 0; }
.model-info { flex: 1; min-width: 0; }
.model-name { font-size: 15px; font-weight: 600; color: var(--text-heading); display: flex; align-items: center; gap: 8px; }
.model-provider { font-size: 12px; color: var(--text-dim); margin-top: 2px; }
.model-body { padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }
.model-param { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.pl { color: var(--text-secondary); }
.pv { color: var(--text-primary); font-weight: 500; }
.pv.mono { font-family: var(--font-mono); font-size: 11px; color: var(--text-dim); }
.model-footer { padding: 12px 16px; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; gap: 6px; }
.empty-card { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 48px; border: 2px dashed var(--border-color); border-radius: var(--radius-lg); cursor: pointer; color: var(--text-dim); transition: all var(--transition); }
.empty-card:hover { border-color: var(--accent); color: var(--accent); background: var(--bg-hover); }
.empty-card p { margin-top: 8px; }
.form-hint { font-size: 11px; color: var(--text-dim); margin-top: 4px; }
</style>
