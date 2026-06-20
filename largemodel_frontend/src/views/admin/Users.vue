<template>
  <div class="admin-users">
    <h2 class="page-title">用户管理</h2>

    <!-- Search & Filter -->
    <div class="filter-bar glass-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用户名 / 昵称 / 邮箱"
          clearable
          size="default"
          style="width: 260px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="query.role" placeholder="全部角色" clearable size="default" style="width: 140px">
          <el-option label="普通用户" value="USER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>

        <el-select v-model="query.status" placeholder="全部状态" clearable size="default" style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>

        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- Table -->
    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        :data="userList"
        stripe
        border
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="nickname" label="昵称" width="130">
          <template #default="{ row }">
            {{ row.nickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
              {{ row.roleDisplayName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="row.role === 'ADMIN'"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">
            {{ row.lastLoginAt ? formatDate(row.lastLoginAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEditRole(row)">
              改角色
            </el-button>
            <el-button text type="warning" size="small" @click="handleResetPwd(row)">
              重置密码
            </el-button>
            <el-button
              text type="danger" size="small"
              :disabled="row.role === 'ADMIN'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          small
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </div>

    <!-- Role dialog -->
    <el-dialog v-model="roleVisible" title="修改角色" width="400px">
      <div v-if="editingUser" class="role-form">
        <div class="role-info">
          <span class="role-label">用户</span>
          <span class="role-value">{{ editingUser.username }}</span>
        </div>
        <div class="role-info">
          <span class="role-label">当前角色</span>
          <el-tag :type="editingUser.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
            {{ editingUser.roleDisplayName }}
          </el-tag>
        </div>
        <div class="role-select">
          <span class="role-label">新角色</span>
          <el-select v-model="newRole" style="width: 100%; margin-top: 8px">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="confirmEditRole">确认</el-button>
      </template>
    </el-dialog>

    <!-- Reset password dialog -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="400px">
      <div v-if="editingUser">
        <div class="role-info">
          <span class="role-label">用户</span>
          <span class="role-value">{{ editingUser.username }}</span>
        </div>
        <div style="margin-top: 16px">
          <span class="role-label">新密码</span>
          <el-input
            v-model="newPassword"
            type="password"
            show-password
            placeholder="至少6位"
            style="margin-top: 8px"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="confirmResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, updateUserStatus, updateUserRole, deleteUser, resetUserPassword } from '@/api/admin'
import { Search } from '@element-plus/icons-vue'

const loading = ref(false)
const userList = ref([])
const total = ref(0)

const query = reactive({
  page: 0, size: 10, keyword: '', role: '', status: undefined
})

const roleVisible = ref(false)
const roleLoading = ref(false)
const editingUser = ref(null)
const newRole = ref('USER')

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const newPassword = ref('')

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

async function fetchUsers() {
  loading.value = true
  try {
    const params = {}
    Object.keys(query).forEach(k => {
      if (query[k] !== '' && query[k] !== undefined && query[k] !== null) params[k] = query[k]
    })
    const res = await listUsers(params)
    userList.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch { userList.value = []; total.value = 0 }
  finally { loading.value = false }
}

function handleSearch() { query.page = 0; fetchUsers() }
function handleReset() {
  query.keyword = ''; query.role = ''; query.status = undefined; query.page = 0; fetchUsers()
}

async function handleStatusChange(row, enabled) {
  try {
    await updateUserStatus(row.id, enabled ? 1 : 0)
    ElMessage.success(`用户已${enabled ? '启用' : '禁用'}`)
    fetchUsers()
  } catch { /* handled */ }
}

function handleEditRole(row) {
  editingUser.value = row; newRole.value = row.role; roleVisible.value = true
}

async function confirmEditRole() {
  if (!editingUser.value || newRole.value === editingUser.value.role) {
    roleVisible.value = false; return
  }
  roleLoading.value = true
  try {
    await updateUserRole(editingUser.value.id, newRole.value)
    ElMessage.success('角色修改成功')
    roleVisible.value = false
    fetchUsers()
  } catch { /* handled */ }
  finally { roleLoading.value = false }
}

function handleResetPwd(row) {
  editingUser.value = row; newPassword.value = ''; pwdVisible.value = true
}

async function confirmResetPwd() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少6位'); return
  }
  pwdLoading.value = true
  try {
    await resetUserPassword(editingUser.value.id, newPassword.value)
    ElMessage.success(`用户「${editingUser.value.username}」的密码已重置`)
    pwdVisible.value = false
  } catch { /* handled */ }
  finally { pwdLoading.value = false }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '确认删除', {
    confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('用户已删除')
      fetchUsers()
    } catch { /* handled */ }
  }).catch(() => {})
}

onMounted(() => fetchUsers())
</script>

<style scoped>
.admin-users {
  max-width: 1400px;
  margin: 0 auto;
}

.filter-bar {
  padding: 18px 20px;
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-wrapper {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-footer {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-color);
}

/* Role dialog */
.role-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.role-label {
  font-size: 13px;
  color: var(--text-dim);
  min-width: 70px;
}

.role-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.role-select {
  margin-top: 4px;
}
</style>
