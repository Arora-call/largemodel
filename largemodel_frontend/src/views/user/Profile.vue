<template>
  <div class="profile-page">
    <h2 class="page-title">个人中心</h2>

    <div class="profile-grid">
      <!-- 基本信息 -->
      <div class="profile-section">
        <div class="section-header">
          <span class="section-label">基本信息</span>
        </div>

        <div class="section-body">
          <!-- 头像 -->
          <div class="avatar-row" @click="triggerUpload" title="点击更换头像">
            <div class="avatar-wrap">
              <el-avatar :size="72" :src="avatarPreviewUrl">
                <span class="avatar-text">{{ (authStore.nickname || authStore.username).charAt(0).toUpperCase() }}</span>
              </el-avatar>
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>更换</span>
              </div>
            </div>
            <div class="avatar-info">
              <span class="avatar-label">点击更换头像</span>
              <span class="avatar-hint">支持 JPG/PNG，不超过 2MB</span>
            </div>
          </div>

          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFileChange" />

          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" label-position="top">
            <div class="form-row">
              <el-form-item label="用户名">
                <el-input :model-value="authStore.username" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="form.nickname" placeholder="设置昵称" />
              </el-form-item>
            </div>
            <div class="form-row">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="设置邮箱" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="设置手机号" />
              </el-form-item>
            </div>

            <div class="info-row">
              <div class="info-item">
                <span class="info-label">角色</span>
                <el-tag :type="authStore.isAdmin ? 'danger' : 'primary'" size="small">
                  {{ authStore.user?.roleDisplayName || authStore.userRole }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formatDate(authStore.user?.createdAt) }}</span>
              </div>
            </div>

            <el-button type="primary" :loading="saving" @click="handleUpdate" class="save-btn">
              保存修改
            </el-button>
          </el-form>
        </div>
      </div>

      <!-- 修改密码 -->
      <div class="profile-section">
        <div class="section-header">
          <span class="section-label">修改密码</span>
        </div>
        <div class="section-body">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
            </el-form-item>
            <el-button type="primary" :loading="changingPwd" @click="handleChangePwd" class="save-btn">
              修改密码
            </el-button>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 危险操作 -->
    <div class="danger-section">
      <div class="danger-header">
        <span class="danger-label">危险操作</span>
        <span class="danger-desc">注销账户后，所有数据将被清除且无法恢复</span>
      </div>
      <el-button type="danger" :loading="deleting" @click="handleDelete" plain>
        注销账户
      </el-button>
    </div>

    <!-- 头像预览弹窗 -->
    <el-dialog v-model="previewVisible" title="更换头像" width="420px">
      <div class="preview-body">
        <img :src="previewUrl" alt="Preview" class="preview-img" />
      </div>
      <template #footer>
        <el-button @click="cancelUpload">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">确认上传</el-button>
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
const pwdFormRef = ref(null)
const fileInput = ref(null)
const saving = ref(false)
const changingPwd = ref(false)
const deleting = ref(false)
const uploading = ref(false)
const selectedFile = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')

const form = reactive({ nickname: '', email: '', phone: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

const rules = {
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
}
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const avatarPreviewUrl = computed(() => {
  const avatar = authStore.user?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  return avatar
})

function triggerUpload() { fileInput.value?.click() }

function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('图片大小不能超过 2MB'); return }
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  previewVisible.value = true
  e.target.value = ''
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const res = await uploadAvatar(selectedFile.value)
    authStore.setUser({ ...authStore.user, avatar: res.data })
    ElMessage.success('头像上传成功')
    previewVisible.value = false
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
    selectedFile.value = null
  } catch { /* handled */ }
  finally { uploading.value = false }
}

function cancelUpload() {
  previewVisible.value = false
  URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  selectedFile.value = null
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

onMounted(async () => {
  try {
    await authStore.fetchUserInfo()
    const u = authStore.user
    if (u) {
      form.nickname = u.nickname || ''
      form.email = u.email || ''
      form.phone = u.phone || ''
    }
  } catch { /* ignore */ }
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
  } catch { /* handled */ }
  finally { saving.value = false }
}

async function handleChangePwd() {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
    changingPwd.value = true
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    authStore.logout()
    router.push('/auth/login')
  } catch { /* handled */ }
  finally { changingPwd.value = false }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要注销账户吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定注销', cancelButtonText: '取消', type: 'warning'
    })
    deleting.value = true
    await deleteAccount()
    ElMessage.success('账户已注销')
    authStore.logout()
    router.push('/auth/login')
  } catch { /* cancelled */ }
  finally { deleting.value = false }
}
</script>

<style scoped>
.profile-page { max-width: 860px; margin: 0 auto; }

.profile-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 24px;
}

.profile-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.section-header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-color);
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-heading);
}

.section-body {
  padding: 24px 20px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.avatar-wrap {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-wrap:hover { transform: scale(1.05); transition: transform var(--transition); }

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  font-size: 11px;
  opacity: 0;
  transition: opacity var(--transition);
}

.avatar-wrap:hover .avatar-overlay { opacity: 1; }

.avatar-text { font-size: 28px; font-weight: 600; }

.avatar-info { display: flex; flex-direction: column; gap: 4px; }
.avatar-label { font-size: 14px; color: var(--text-primary); font-weight: 500; }
.avatar-hint { font-size: 12px; color: var(--text-dim); }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.info-row { display: flex; gap: 40px; margin-bottom: 20px; }
.info-item { display: flex; flex-direction: column; gap: 6px; }
.info-label { font-size: 12px; color: var(--text-dim); }
.info-value { font-size: 14px; color: var(--text-primary); }

.save-btn { margin-top: 8px; }

.danger-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: var(--danger-bg);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: var(--radius-lg);
}

.danger-header { display: flex; flex-direction: column; gap: 4px; }
.danger-label { font-size: 14px; font-weight: 600; color: var(--danger); }
.danger-desc { font-size: 13px; color: var(--text-dim); }

.preview-body { display: flex; justify-content: center; padding: 16px 0; }
.preview-img { max-width: 100%; max-height: 360px; border-radius: var(--radius); object-fit: contain; }

@media (max-width: 600px) {
  .form-row { grid-template-columns: 1fr; }
  .danger-section { flex-direction: column; gap: 16px; text-align: center; }
}
</style>
