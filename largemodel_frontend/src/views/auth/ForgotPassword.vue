<template>
  <div class="forgot-form">
    <h2 class="form-title">找回密码</h2>

    <el-steps :active="step" align-center class="steps">
      <el-step title="验证身份" />
      <el-step title="完成" />
    </el-steps>

    <!-- 步骤1：验证身份 -->
    <el-form
      v-if="step === 0"
      ref="verifyFormRef"
      :model="verifyForm"
      :rules="verifyRules"
      label-position="top"
      size="large"
      @submit.prevent="handleVerify"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="verifyForm.username"
          placeholder="请输入用户名"
          :prefix-icon="User"
        />
      </el-form-item>

      <el-form-item label="注册邮箱" prop="email">
        <el-input
          v-model="verifyForm.email"
          placeholder="请输入注册时填写的邮箱"
          :prefix-icon="Message"
        />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="verifyForm.newPassword"
          type="password"
          placeholder="请输入新密码（至少6位）"
          show-password
          :prefix-icon="Lock"
        />
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="verifyForm.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
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
        >
          {{ loading ? '重置中...' : '重置密码' }}
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 步骤2：完成 -->
    <div v-else class="success-block">
      <el-result icon="success" title="密码重置成功" sub-title="请使用新密码登录">
        <template #extra>
          <el-button type="primary" @click="router.push('/auth/login')">去登录</el-button>
        </template>
      </el-result>
    </div>

    <div class="form-footer">
      <router-link to="/auth/login" class="link">返回登录</router-link>
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
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
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
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-form {
  width: 100%;
}

.form-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 24px;
}

.steps {
  margin-bottom: 32px;
}

.submit-btn {
  width: 100%;
}

.success-block {
  padding: 20px 0;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  margin-top: 16px;
}

.link {
  color: #409eff;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>
