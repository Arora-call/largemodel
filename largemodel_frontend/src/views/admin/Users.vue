<template>
  <div class="admin-users">
    <h2 class="page-title">用户管理</h2>

    <!-- 搜索与筛选 -->
    <el-card shadow="hover" class="search-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="用户名/昵称/邮箱"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="全部" :value="undefined" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card shadow="hover">
      <el-table
        v-loading="loading"
        :data="userList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="nickname" label="昵称" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
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
            <el-button
              text
              type="primary"
              size="small"
              @click="handleEditRole(row)"
            >
              改角色
            </el-button>
            <el-button
              text
              type="warning"
              size="small"
              @click="handleResetPwd(row)"
            >
              重置密码
            </el-button>
            <el-button
              text
              type="danger"
              size="small"
              :disabled="row.role === 'ADMIN'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <!-- 编辑角色对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="修改角色"
      width="400px"
    >
      <el-form v-if="editingUser" label-width="80px">
        <el-form-item label="用户名">
          <span>{{ editingUser.username }}</span>
        </el-form-item>
        <el-form-item label="当前角色">
          <el-tag :type="editingUser.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
            {{ editingUser.roleDisplayName }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="newRole" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="confirmEditRole">
          确认
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="pwdDialogVisible"
      title="重置密码"
      width="400px"
    >
      <el-form v-if="editingUser" label-width="80px">
        <el-form-item label="用户名">
          <span>{{ editingUser.username }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="confirmResetPwd">
          确认重置
        </el-button>
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
  page: 0,
  size: 10,
  keyword: '',
  role: '',
  status: undefined
})

// 角色编辑对话框
const roleDialogVisible = ref(false)
const roleLoading = ref(false)
const editingUser = ref(null)
const newRole = ref('USER')

// 重置密码对话框
const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const newPassword = ref('')

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function fetchUsers() {
  loading.value = true
  try {
    const params = {}
    Object.keys(query).forEach(key => {
      if (query[key] !== '' && query[key] !== undefined && query[key] !== null) {
        params[key] = query[key]
      }
    })
    const res = await listUsers(params)
    userList.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch {
    userList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 0
  fetchUsers()
}

function handleReset() {
  query.keyword = ''
  query.role = ''
  query.status = undefined
  query.page = 0
  fetchUsers()
}

async function handleStatusChange(row, enabled) {
  try {
    await updateUserStatus(row.id, enabled ? 1 : 0)
    ElMessage.success(`用户已${enabled ? '启用' : '禁用'}`)
    fetchUsers()
  } catch {
    // ignore
  }
}

function handleEditRole(row) {
  editingUser.value = row
  newRole.value = row.role
  roleDialogVisible.value = true
}

async function confirmEditRole() {
  if (!editingUser.value || newRole.value === editingUser.value.role) {
    roleDialogVisible.value = false
    return
  }
  roleLoading.value = true
  try {
    await updateUserRole(editingUser.value.id, newRole.value)
    ElMessage.success('角色修改成功')
    roleDialogVisible.value = false
    fetchUsers()
  } catch {
    // ignore
  } finally {
    roleLoading.value = false
  }
}

function handleResetPwd(row) {
  editingUser.value = row
  newPassword.value = ''
  pwdDialogVisible.value = true
}

async function confirmResetPwd() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  pwdLoading.value = true
  try {
    await resetUserPassword(editingUser.value.id, newPassword.value)
    ElMessage.success(`用户「${editingUser.value.username}」的密码已重置`)
    pwdDialogVisible.value = false
  } catch {
    // ignore
  } finally {
    pwdLoading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除用户「${row.username}」吗？`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('用户已删除')
      fetchUsers()
    } catch {
      // ignore
    }
  }).catch(() => {
    // cancelled
  })
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.admin-users {
  max-width: 1400px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 20px;
  color: #1a1a2e;
}

.search-card {
  margin-bottom: 20px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
