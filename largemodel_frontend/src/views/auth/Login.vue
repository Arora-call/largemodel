<template>
  <div class="login-form">
    <div class="form-header">
      <h2 class="form-title">欢迎回来</h2>
      <p class="form-subtitle">登录你的账户以继续</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      size="large"
      @submit.prevent="handleLogin"
    >
      <el-form-item prop="username">
        <el-input
          v-model="form.username"
          placeholder="用户名"
          :prefix-icon="User"
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码"
          show-password
          :prefix-icon="Lock"
        />
      </el-form-item>

      <div class="form-extra">
        <router-link to="/auth/forgot-password" class="forgot-link">忘记密码？</router-link>
      </div>

      <el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          class="submit-btn"
          size="large"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form-item>
    </el-form>

    <div class="form-footer">
      <span>还没有账号？</span>
      <router-link to="/auth/register">立即注册</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度应在3-50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度应在6-100个字符之间', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    await authStore.login(form)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-form {
  width: 100%;
}

.form-header {
  margin-bottom: 32px;
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

.form-extra {
  display: flex;
  justify-content: flex-end;
  margin-top: -8px;
  margin-bottom: 24px;
}

.forgot-link {
  font-size: 13px;
  color: var(--text-dim);
  text-decoration: none;
  transition: color var(--transition);
}

.forgot-link:hover {
  color: var(--accent);
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-dim);
}

.form-footer a {
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
}

.form-footer a:hover {
  color: var(--accent-hover);
}
</style>
