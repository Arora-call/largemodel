<template>
  <div class="calculator">
    <div class="calculator__display">
      <div class="calculator__expression">{{ expression || '0' }}</div>
      <div class="calculator__result" :class="{ 'calculator__result--error': isError }">
        {{ displayValue }}
      </div>
    </div>
    <div class="calculator__buttons">
      <button
        v-for="btn in buttons"
        :key="btn.label"
        :class="['calculator__btn', `calculator__btn--${btn.type}`]"
        @click="handleClick(btn)"
      >
        {{ btn.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// ── 状态 ──
const currentInput = ref('')
const previousInput = ref('')
const operator = ref(null)
const shouldResetInput = ref(false)
const expression = ref('')
const isError = ref(false)

// ── 显示值 ──
const displayValue = computed(() => {
  if (isError.value) return '错误'
  if (currentInput.value === '' && !operator.value) return '0'
  return currentInput.value || previousInput.value || '0'
})

// ── 按钮配置 ──
const buttons = [
  { label: 'C', type: 'clear' },
  { label: '±', type: 'toggle' },
  { label: '%', type: 'percent' },
  { label: '÷', type: 'operator', value: '/' },

  { label: '7', type: 'number' },
  { label: '8', type: 'number' },
  { label: '9', type: 'number' },
  { label: '×', type: 'operator', value: '*' },

  { label: '4', type: 'number' },
  { label: '5', type: 'number' },
  { label: '6', type: 'number' },
  { label: '−', type: 'operator', value: '-' },

  { label: '1', type: 'number' },
  { label: '2', type: 'number' },
  { label: '3', type: 'number' },
  { label: '+', type: 'operator', value: '+' },

  { label: '0', type: 'number', span: true },
  { label: '.', type: 'dot' },
  { label: '=', type: 'equals' },
]

// ── 工具函数 ──
function formatNumber(num) {
  const str = String(num)
  // 限制最大显示长度
  if (str.length > 15) {
    return parseFloat(num).toExponential(6)
  }
  return str
}

function calculate(a, op, b) {
  const numA = parseFloat(a)
  const numB = parseFloat(b)
  switch (op) {
    case '+': return numA + numB
    case '-': return numA - numB
    case '*': return numA * numB
    case '/':
      if (numB === 0) throw new Error('除数不能为零')
      return numA / numB
    default: return numB
  }
}

// ── 事件处理 ──
function handleClick(btn) {
  isError.value = false

  switch (btn.type) {
    case 'number':
      inputNumber(btn.label)
      break
    case 'dot':
      inputDot()
      break
    case 'operator':
      inputOperator(btn.value)
      break
    case 'equals':
      calculateResult()
      break
    case 'clear':
      clearAll()
      break
    case 'toggle':
      toggleSign()
      break
    case 'percent':
      toPercent()
      break
  }
}

function inputNumber(num) {
  if (shouldResetInput.value) {
    currentInput.value = num
    shouldResetInput.value = false
  } else {
    if (currentInput.value.length >= 15) return
    currentInput.value += num
  }
  updateExpression()
}

function inputDot() {
  if (shouldResetInput.value) {
    currentInput.value = '0.'
    shouldResetInput.value = false
    updateExpression()
    return
  }
  if (!currentInput.value.includes('.')) {
    currentInput.value += currentInput.value === '' ? '0.' : '.'
  }
  updateExpression()
}

function inputOperator(op) {
  if (operator.value && !shouldResetInput.value) {
    // 连续运算
    try {
      const result = calculate(previousInput.value, operator.value, currentInput.value)
      previousInput.value = formatNumber(result)
      currentInput.value = ''
    } catch (e) {
      isError.value = true
      previousInput.value = ''
      currentInput.value = ''
    }
  } else {
    previousInput.value = currentInput.value || previousInput.value || '0'
    currentInput.value = ''
  }
  operator.value = op
  shouldResetInput.value = false
  updateExpression()
}

function calculateResult() {
  if (!operator.value || !previousInput.value) return
  if (currentInput.value === '') currentInput.value = previousInput.value

  try {
    const result = calculate(previousInput.value, operator.value, currentInput.value)
    const formatted = formatNumber(result)
    expression.value = `${previousInput.value} ${operatorSymbol(operator.value)} ${currentInput.value} =`
    currentInput.value = formatted
    previousInput.value = ''
    operator.value = null
    shouldResetInput.value = true
  } catch (e) {
    isError.value = true
    expression.value = '错误'
    currentInput.value = ''
    previousInput.value = ''
    operator.value = null
  }
}

function clearAll() {
  currentInput.value = ''
  previousInput.value = ''
  operator.value = null
  shouldResetInput.value = false
  expression.value = ''
  isError.value = false
}

function toggleSign() {
  if (currentInput.value === '' || currentInput.value === '0') return
  currentInput.value = currentInput.value.startsWith('-')
    ? currentInput.value.slice(1)
    : '-' + currentInput.value
  updateExpression()
}

function toPercent() {
  if (currentInput.value === '') return
  const num = parseFloat(currentInput.value) / 100
  currentInput.value = formatNumber(num)
  updateExpression()
}

function operatorSymbol(op) {
  const map = { '+': '+', '-': '−', '*': '×', '/': '÷' }
  return map[op] || op
}

function updateExpression() {
  if (operator.value && previousInput.value) {
    expression.value = `${previousInput.value} ${operatorSymbol(operator.value)} ${currentInput.value || '...'}`
  } else if (operator.value) {
    expression.value = `${previousInput.value || '0'} ${operatorSymbol(operator.value)}`
  } else {
    expression.value = currentInput.value || ''
  }
}
</script>

<style scoped>
.calculator {
  width: 340px;
  background: #1c1c1e;
  border-radius: 20px;
  padding: 24px 20px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(255, 255, 255, 0.05);
  user-select: none;
}

/* ── 显示区 ── */
.calculator__display {
  padding: 8px 6px 20px;
  margin-bottom: 12px;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  overflow: hidden;
}

.calculator__expression {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.5);
  word-break: break-all;
  text-align: right;
  line-height: 1.4;
  min-height: 26px;
  max-width: 100%;
}

.calculator__result {
  font-size: 48px;
  font-weight: 300;
  color: #ffffff;
  line-height: 1.1;
  word-break: break-all;
  text-align: right;
  max-width: 100%;
  transition: font-size 0.15s ease;
}

.calculator__result--error {
  color: #ff6b6b;
  font-size: 32px;
}

/* ── 按钮网格 ── */
.calculator__buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.calculator__btn {
  height: 64px;
  border: none;
  border-radius: 16px;
  font-size: 26px;
  font-weight: 400;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.08s ease, box-shadow 0.08s ease, filter 0.08s ease;
  outline: none;
  -webkit-tap-highlight-color: transparent;
}

.calculator__btn:active {
  transform: scale(0.94);
  filter: brightness(0.85);
}

/* ── 按钮类型 ── */
.calculator__btn--number {
  background: #3a3a3c;
  color: #ffffff;
}
.calculator__btn--number:hover {
  background: #4a4a4c;
}

.calculator__btn--operator {
  background: #ff9f0a;
  color: #ffffff;
}
.calculator__btn--operator:hover {
  background: #ffb340;
}
.calculator__btn--operator:active {
  background: #e08e00;
}

.calculator__btn--clear {
  background: #d1d3d8;
  color: #1c1c1e;
}
.calculator__btn--clear:hover {
  background: #e3e5ea;
}

.calculator__btn--toggle,
.calculator__btn--percent {
  background: #d1d3d8;
  color: #1c1c1e;
}
.calculator__btn--toggle:hover,
.calculator__btn--percent:hover {
  background: #e3e5ea;
}

.calculator__btn--dot {
  background: #3a3a3c;
  color: #ffffff;
}
.calculator__btn--dot:hover {
  background: #4a4a4c;
}

.calculator__btn--equals {
  background: #ff9f0a;
  color: #ffffff;
}
.calculator__btn--equals:hover {
  background: #ffb340;
}

/* ── 0 按钮跨列 ── */
.calculator__btn[class*="--number"]:nth-child(17) {
  grid-column: span 2;
  border-radius: 16px;
}
</style>