<template>
  <div class="forgot-form">
    <div class="form-header">
      <h2 class="form-title">找回密码</h2>
      <p class="form-subtitle">验证你的身份以重置密码</p>
    </div>

    <el-steps :active="step" align-center class="steps">
      <el-step title="验证身份" />
      <el-step title="完成" />
    </el-steps>

    <!-- Step 1: Verify identity -->
    <el-form
      v-if="step === 0"
      ref="verifyFormRef"
      :model="verifyForm"
      :rules="verifyRules"
      size="large"
      @submit.prevent="handleVerify"
    >
      <el-form-item prop="username">
        <el-input
          v-model="verifyForm.username"
          placeholder="用户名"
          :prefix-icon="User"
        />
      </el-form-item>

      <el-form-item prop="email">
        <el-input
          v-model="verifyForm.email"
          placeholder="注册时填写的邮箱"
          :prefix-icon="Message"
        />
      </el-form-item>

      <el-form-item prop="newPassword">
        <el-input
          v-model="verifyForm.newPassword"
          type="password"
          placeholder="新密码（至少6位）"
          show-password
          :prefix-icon="Lock"
        />
      </el-form-item>

      <el-form-item prop="confirmPassword">
        <el-input
          v-model="verifyForm.confirmPassword"
          type="password"
          placeholder="确认新密码"
          show-password
          :prefix-icon="Lock"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          class="submit-btn"
          size="large"
        >
          {{ loading ? '重置中...' : '重置密码' }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- Step 2: Done -->
    <div v-else class="success-block">
      <el-result icon="success" title="密码重置成功" sub-title="请使用新密码登录">
        <template #extra>
          <el-button type="primary" @click="router.push('/auth/login')">去登录</el-button>
        </template>
      </el-result>
    </div>

    <div class="form-footer">
      <router-link to="/auth/login">← 返回登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { forgotPassword } from '@/api/auth'

const router = useRouter()

const step = ref(0)
const loading = ref(false)
const verifyFormRef = ref(null)

const verifyForm = reactive({
  username: '',
  email: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== verifyForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const verifyRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleVerify() {
  if (!verifyFormRef.value) return
  try {
    await verifyFormRef.value.validate()
    loading.value = true
    await forgotPassword({
      username: verifyForm.username,
      email: verifyForm.email,
      newPassword: verifyForm.newPassword
    })
    step.value = 1
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.forgot-form {
  width: 100%;
}

.form-header {
  margin-bottom: 24px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-heading);
  margin: 0 0 6px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-dim);
  margin: 0;
}

.steps {
  margin-bottom: 32px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.success-block {
  padding: 20px 0;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  margin-top: 16px;
}

.form-footer a {
  color: var(--text-dim);
  text-decoration: none;
  transition: color var(--transition);
}

.form-footer a:hover {
  color: var(--accent);
}
</style>
