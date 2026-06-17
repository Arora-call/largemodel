<template>
  <div class="register-form">
    <h2 class="form-title">创建账号</h2>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="handleRegister"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="请输入用户名（3-50个字符）"
          :prefix-icon="User"
        />
      </el-form-item>

      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="form.nickname"
          placeholder="请输入昵称（选填）"
          :prefix-icon="EditPen"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码（至少6位）"
          show-password
          :prefix-icon="Lock"
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          show-password
          :prefix-icon="Lock"
        />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="form.email"
          placeholder="请输入邮箱（选填）"
          :prefix-icon="Message"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          class="submit-btn"
        >
          {{ loading ? '注册中...' : '注 册' }}
        </el-button>
      </el-form-item>
    </el-form>

    <div class="form-footer">
      <span>已有账号？</span>
      <router-link to="/auth/login" class="link">立即登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, EditPen } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value && value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度应在3-50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const registerData = {
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined
    }

    await authStore.register(registerData)

    ElMessage.success('注册成功，请登录')
    router.push('/auth/login')
  } catch (err) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-form {
  width: 100%;
}

.form-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 24px;
}

.submit-btn {
  width: 100%;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.link {
  color: #409eff;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>
