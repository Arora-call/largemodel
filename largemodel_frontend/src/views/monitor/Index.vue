<template>
  <div class="monitor-page">
    <h1 class="page-title">📊 监控大盘</h1>
    <p class="section-subtitle">实时追踪 AI 调用量、Token 消耗与服务性能</p>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--accent-bg); color: var(--accent)">
          <el-icon :size="20"><TrendCharts /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">--</div>
          <div class="stat-card__label">今日调用次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--success-bg); color: var(--success)">
          <el-icon :size="20"><CircleCheck /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">--%</div>
          <div class="stat-card__label">成功率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--warning-bg); color: var(--warning)">
          <el-icon :size="20"><Odometer /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">--</div>
          <div class="stat-card__label">今日 Token 消耗</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--danger-bg); color: var(--danger)">
          <el-icon :size="20"><Timer /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">--ms</div>
          <div class="stat-card__label">平均延迟</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-grid">
      <!-- 调用量趋势 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>调用量趋势</span>
            <el-radio-group v-model="callPeriod" size="small">
              <el-radio-button value="24h">24h</el-radio-button>
              <el-radio-button value="7d">7天</el-radio-button>
              <el-radio-button value="30d">30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="chart-placeholder">
          <el-icon :size="48"><TrendCharts /></el-icon>
          <p>调用量趋势图（即将上线）</p>
        </div>
      </el-card>

      <!-- Token 消耗分布 -->
      <el-card class="chart-card">
        <template #header>
          <span>Token 消耗分布</span>
        </template>
        <div class="chart-placeholder">
          <el-icon :size="48"><PieChart /></el-icon>
          <p>Token 消耗分布图（即将上线）</p>
        </div>
      </el-card>

      <!-- 响应延迟 -->
      <el-card class="chart-card">
        <template #header>
          <span>响应延迟 (P50 / P95 / P99)</span>
        </template>
        <div class="chart-placeholder">
          <el-icon :size="48"><Histogram /></el-icon>
          <p>延迟监控图（即将上线）</p>
        </div>
      </el-card>

      <!-- 模型调用占比 -->
      <el-card class="chart-card">
        <template #header>
          <span>模型调用占比</span>
        </template>
        <div class="chart-placeholder">
          <el-icon :size="48"><DataAnalysis /></el-icon>
          <p>模型分布图（即将上线）</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  TrendCharts, CircleCheck, Odometer, Timer, PieChart, Histogram, DataAnalysis
} from '@element-plus/icons-vue'

const callPeriod = ref('24h')
</script>

<style scoped>
.monitor-page {
  max-width: 1400px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.chart-card {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

.chart-card :deep(.el-card__header) {
  color: var(--text-heading);
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-placeholder {
  height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-dim);
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
}

.chart-placeholder .el-icon {
  opacity: 0.4;
  margin-bottom: 12px;
}

.chart-placeholder p {
  font-size: 13px;
}

@media (max-width: 1024px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
