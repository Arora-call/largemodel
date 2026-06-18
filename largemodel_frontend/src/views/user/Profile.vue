<template>
  <div class="profile">
    <h2 class="page-title">个人中心</h2>

    <el-row :gutter="20">
      <!-- 基本信息 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span>基本信息</span>
          </template>

          <!-- 头像区域 -->
          <div class="avatar-section">
            <div class="avatar-wrapper" @click="triggerUpload" title="点击更换头像">
              <el-avatar :size="80" :src="avatarPreviewUrl">
                <span class="avatar-placeholder">{{ (authStore.nickname || authStore.username).charAt(0).toUpperCase() }}</span>
              </el-avatar>
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
            <p class="avatar-hint">点击头像更换，支持 JPG/PNG，不超过 2MB</p>
          </div>

          <!-- 隐藏的文件输入 -->
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleFileChange"
          />

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
          >
            <el-form-item label="用户名">
              <el-input :model-value="authStore.username" disabled />
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="角色">
              <el-tag :type="authStore.isAdmin ? 'danger' : 'primary'">
                {{ authStore.user?.roleDisplayName || authStore.userRole }}
              </el-tag>
            </el-form-item>

            <el-form-item label="注册时间">
              <span>{{ authStore.user?.createdAt ? formatDate(authStore.user.createdAt) : '-' }}</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleUpdate">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 修改密码 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span>修改密码</span>
          </template>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入原密码"
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码（至少6位）"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 注销账户 -->
        <el-card shadow="hover" class="danger-card" style="margin-top: 20px">
          <template #header>
            <span style="color: #f56c6c">危险操作</span>
          </template>

          <p class="danger-text">注销账户后，所有数据将被清除且无法恢复。</p>
          <el-button type="danger" :loading="deleting" @click="handleDeleteAccount">
            注销账户
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 头像预览上传弹窗 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="更换头像"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="preview-body">
        <img :src="previewImageUrl" alt="预览" class="preview-image" />
      </div>
      <template #footer>
        <el-button @click="handleCancelUpload">取消</el-button>
        <el-button type="primary" :loading="uploadingAvatar" @click="handleAvatarUpload">
          确认上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { getUserInfo, updateUserInfo, changePassword, deleteAccount, uploadAvatar } from '@/api/user'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const passwordFormRef = ref(null)
const fileInputRef = ref(null)
const saving = ref(false)
const changingPwd = ref(false)
const deleting = ref(false)
const uploadingAvatar = ref(false)
const selectedFile = ref(null)
const previewDialogVisible = ref(false)
const previewImageUrl = ref('')

const form = reactive({
  nickname: '',
  email: '',
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: ''
})

const rules = {
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

// 构建头像完整 URL（相对路径拼接后端地址）
const avatarPreviewUrl = computed(() => {
  const avatar = authStore.user?.avatar
  if (!avatar) return ''
  // 已经是完整 URL
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  // 相对路径，拼接后端地址（开发时走 Vite proxy，生产时同域）
  return avatar
})

// 触发文件选择
function triggerUpload() {
  fileInputRef.value?.click()
}

// 处理文件选择
function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  // 前端校验
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  selectedFile.value = file
  previewImageUrl.value = URL.createObjectURL(file)
  previewDialogVisible.value = true

  // 重置 input 以便重复选择同一文件
  e.target.value = ''
}

// 确认上传头像
async function handleAvatarUpload() {
  if (!selectedFile.value) return
  try {
    uploadingAvatar.value = true
    const res = await uploadAvatar(selectedFile.value)
    // 更新 authStore 中的用户头像
    authStore.setUser({ ...authStore.user, avatar: res.data })
    ElMessage.success('头像上传成功')
    previewDialogVisible.value = false
    // 释放 blob URL
    URL.revokeObjectURL(previewImageUrl.value)
    previewImageUrl.value = ''
    selectedFile.value = null
  } catch {
    // request interceptor 已处理错误提示
  } finally {
    uploadingAvatar.value = false
  }
}

// 取消上传
function handleCancelUpload() {
  previewDialogVisible.value = false
  URL.revokeObjectURL(previewImageUrl.value)
  previewImageUrl.value = ''
  selectedFile.value = null
}

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

onMounted(async () => {
  try {
    await authStore.fetchUserInfo()
    const user = authStore.user
    if (user) {
      form.nickname = user.nickname || ''
      form.email = user.email || ''
      form.phone = user.phone || ''
    }
  } catch {
    // ignore
  }
})

async function handleUpdate() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    saving.value = true
    const res = await updateUserInfo({
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined
    })
    authStore.setUser(res.data)
    ElMessage.success('保存成功')
  } catch {
    // ignore
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
    changingPwd.value = true
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    authStore.logout()
    router.push('/auth/login')
  } catch {
    // ignore
  } finally {
    changingPwd.value = false
  }
}

async function handleDeleteAccount() {
  try {
    await ElMessageBox.confirm(
      '确定要注销账户吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定注销',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    deleting.value = true
    await deleteAccount()
    ElMessage.success('账户已注销')
    authStore.logout()
    router.push('/auth/login')
  } catch {
    // cancelled
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.profile {
  max-width: 1200px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 20px;
  color: #1a1a2e;
}

.danger-text {
  color: #666;
  font-size: 14px;
  margin: 0 0 16px;
}

/* 头像上传 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  transition: transform 0.2s;
}

.avatar-wrapper:hover {
  transform: scale(1.05);
}

.avatar-wrapper :deep(.el-avatar) {
  display: block;
}

.avatar-placeholder {
  font-size: 28px;
  font-weight: 600;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 13px;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #999;
}

/* 头像预览弹窗 */
.preview-body {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0;
}

.preview-image {
  max-width: 100%;
  max-height: 360px;
  border-radius: 8px;
  object-fit: contain;
}
</style>
