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
          <div class="stat-card__value">{{ overview.todayCalls ?? '--' }}</div>
          <div class="stat-card__label">今日调用次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--success-bg); color: var(--success)">
          <el-icon :size="20"><CircleCheck /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">{{ overview.successRate ?? '--' }}%</div>
          <div class="stat-card__label">成功率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--warning-bg); color: var(--warning)">
          <el-icon :size="20"><Odometer /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">{{ formatNumber(overview.todayTokens) }}</div>
          <div class="stat-card__label">今日 Token 消耗</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card__icon" style="background: var(--danger-bg); color: var(--danger)">
          <el-icon :size="20"><Timer /></el-icon>
        </div>
        <div>
          <div class="stat-card__value">{{ fmtMs(overview.avgLatency) }}</div>
          <div class="stat-card__label">平均 AI 响应耗时</div>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>调用量趋势</span>
            <el-radio-group v-model="period" size="small" @change="fetchTrends">
              <el-radio-button value="7">7天</el-radio-button>
              <el-radio-button value="30">30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="callsChart" class="chart-box"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header><span>Token 消耗趋势</span></template>
        <div ref="tokensChart" class="chart-box"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header><span>模型调用占比</span></template>
        <div ref="modelChart" class="chart-box"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header><span>AI 响应耗时</span></template>
        <div class="chart-box center">
          <div class="big-number">{{ fmtMs(overview.avgLatency) }}</div>
          <p class="desc">今日平均首 Token 响应时间（AI 思考延迟）</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { TrendCharts, CircleCheck, Odometer, Timer } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getStatsOverview, getCallStats, getTokenStats, getModelDistribution } from '@/api/monitor'

const overview = reactive({})
const period = ref(7)
const callsChart = ref(null)
const tokensChart = ref(null)
const modelChart = ref(null)

function formatNumber(n) {
  if (n == null) return '--'
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return String(n)
}
function fmtMs(n) {
  if (n == null || n === 0) return '--'
  if (n >= 60000) return (n / 1000).toFixed(1) + 's'
  if (n >= 1000) return (n / 1000).toFixed(1) + 's'
  return Math.round(n) + 'ms'
}

async function fetchOverview() {
  try { const r = await getStatsOverview(); Object.assign(overview, r.data || {}) } catch {}
}

async function fetchTrends() {
  try {
    const [calls, tokens] = await Promise.all([
      getCallStats({ days: period.value }),
      getTokenStats({ days: period.value })
    ])
    renderLine(callsChart.value, (calls.data||[]).map(d => ({ name: d.dt, value: d.cnt })), '调用次数')
    renderLine(tokensChart.value, (tokens.data||[]).map(d => ({ name: d.dt, value: d.tokens })), 'Token')
  } catch {}
}

async function fetchModels() {
  try {
    const r = await getModelDistribution()
    const data = (r.data || []).map(d => ({ name: d.name || 'Unknown', value: d.value }))
    const chart = echarts.init(modelChart.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['50%', '75%'],
        label: { color: '#8b949e', fontSize: 11 },
        data,
      }]
    })
  } catch {}
}

function renderLine(el, data, label) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.name.slice(5)), axisLine: { lineStyle: { color: '#30363d' } }, axisLabel: { color: '#6b7280', fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#21262d' } }, axisLabel: { color: '#6b7280' } },
    series: [{ data: data.map(d => d.value), type: 'line', smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { color: '#7c8aff' }, itemStyle: { color: '#7c8aff' } }]
  })
}

onMounted(async () => {
  await fetchOverview()
  await fetchTrends()
  await nextTick(fetchModels)
})
</script>

<style scoped>
.monitor-page { max-width: 1400px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.charts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.chart-card { background: var(--bg-card) !important; border-color: var(--border-color) !important; }
.chart-card :deep(.el-card__header) { color: var(--text-heading); font-weight: 600; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-box { height: 260px; }
.chart-box.center { display: flex; flex-direction: column; align-items: center; justify-content: center; }
.big-number { font-size: 48px; font-weight: 700; color: var(--text-heading); }
.unit { font-size: 20px; color: var(--text-dim); margin-left: 4px; }
.desc { color: var(--text-dim); font-size: 13px; margin-top: 8px; }
@media (max-width: 1024px) { .stats-row { grid-template-columns: repeat(2, 1fr); } .charts-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) { .stats-row { grid-template-columns: 1fr; } }
</style>
