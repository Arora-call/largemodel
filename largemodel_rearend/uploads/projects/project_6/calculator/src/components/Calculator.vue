<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ expression || '0' }}</div>
      <div class="result" :class="{ 'result-small': current.length > 10 }">{{ current || '0' }}</div>
    </div>
    <div class="buttons">
      <button
        v-for="btn in buttons"
        :key="btn.label"
        :class="['btn', btn.type]"
        @click="handleClick(btn)"
      >
        {{ btn.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const current = ref('0')
const expression = ref('')
const operator = ref(null)
const previous = ref(null)
const resetNext = ref(false)

const buttons = [
  { label: 'C', type: 'clear', action: 'clear' },
  { label: '±', type: 'toggle', action: 'toggle' },
  { label: '%', type: 'percent', action: 'percent' },
  { label: '÷', type: 'operator', action: 'divide' },
  { label: '7', type: 'number', value: 7 },
  { label: '8', type: 'number', value: 8 },
  { label: '9', type: 'number', value: 9 },
  { label: '×', type: 'operator', action: 'multiply' },
  { label: '4', type: 'number', value: 4 },
  { label: '5', type: 'number', value: 5 },
  { label: '6', type: 'number', value: 6 },
  { label: '-', type: 'operator', action: 'subtract' },
  { label: '1', type: 'number', value: 1 },
  { label: '2', type: 'number', value: 2 },
  { label: '3', type: 'number', value: 3 },
  { label: '+', type: 'operator', action: 'add' },
  { label: '0', type: 'number zero', value: 0 },
  { label: '.', type: 'decimal', action: 'decimal' },
  { label: '=', type: 'equals', action: 'equals' },
]

function handleClick(btn) {
  if (btn.type === 'number') {
    inputDigit(btn.value)
  } else if (btn.type === 'decimal') {
    inputDecimal()
  } else if (btn.type === 'operator') {
    handleOperator(btn.action)
  } else if (btn.type === 'equals') {
    calculate()
  } else if (btn.action === 'clear') {
    clear()
  } else if (btn.action === 'toggle') {
    toggleSign()
  } else if (btn.action === 'percent') {
    percent()
  }
}

function inputDigit(num) {
  if (resetNext.value) {
    current.value = String(num)
    resetNext.value = false
  } else {
    if (current.value === '0' && num !== 0) {
      current.value = String(num)
    } else if (current.value === '0' && num === 0) {
      return
    } else {
      if (current.value.replace('-', '').replace('.', '').length >= 14) return
      current.value += num
    }
  }
}

function inputDecimal() {
  if (resetNext.value) {
    current.value = '0.'
    resetNext.value = false
    return
  }
  if (!current.value.includes('.')) {
    current.value += '.'
  }
}

function handleOperator(op) {
  const val = parseFloat(current.value)
  if (operator.value && !resetNext.value) {
    calculate()
  }
  previous.value = parseFloat(current.value)
  operator.value = op
  resetNext.value = true
  expression.value = `${formatDisplay(previous.value)} ${getOpSymbol(op)}`
}

function calculate() {
  if (operator.value === null || previous.value === null) return
  const a = previous.value
  const b = parseFloat(current.value)
  let result = 0
  switch (operator.value) {
    case 'add': result = a + b; break
    case 'subtract': result = a - b; break
    case 'multiply': result = a * b; break
    case 'divide':
      result = b === 0 ? 'Error' : a / b
      break
    default: return
  }
  if (result === 'Error') {
    current.value = 'Error'
    expression.value = ''
    operator.value = null
    previous.value = null
    resetNext.value = true
    return
  }
  const formatted = formatDisplay(result)
  expression.value = `${formatDisplay(a)} ${getOpSymbol(operator.value)} ${formatDisplay(b)} =`
  current.value = formatted
  operator.value = null
  previous.value = null
  resetNext.value = true
}

function clear() {
  current.value = '0'
  expression.value = ''
  operator.value = null
  previous.value = null
  resetNext.value = false
}

function toggleSign() {
  if (current.value === '0') return
  if (current.value.startsWith('-')) {
    current.value = current.value.slice(1)
  } else {
    current.value = '-' + current.value
  }
}

function percent() {
  const num = parseFloat(current.value)
  if (isNaN(num)) return
  current.value = formatDisplay(num / 100)
}

function formatDisplay(num) {
  if (Number.isInteger(num)) {
    return String(num)
  }
  const s = num.toPrecision(12)
  return parseFloat(s).toString()
}

function getOpSymbol(op) {
  const map = { add: '+', subtract: '-', multiply: '×', divide: '÷' }
  return map[op] || ''
}
</script>

<style scoped>
.calculator {
  width: 360px;
  background: #2d2d44;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.display {
  background: #1e1e32;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  word-break: break-all;
}

.expression {
  font-size: 18px;
  color: #9a9ab0;
  min-height: 28px;
  line-height: 1.4;
}

.result {
  font-size: 42px;
  font-weight: 600;
  color: #ffffff;
  line-height: 1.2;
  transition: font-size 0.15s;
}

.result-small {
  font-size: 32px;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.btn {
  padding: 18px 0;
  font-size: 24px;
  font-weight: 500;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  user-select: none;
  outline: none;
}

.btn:active {
  transform: scale(0.94);
}

.btn.number {
  background: #3b3b54;
  color: #ffffff;
}

.btn.number:hover {
  background: #4a4a66;
}

.btn.zero {
  grid-column: span 2;
}

.btn.operator {
  background: #f59e0b;
  color: #ffffff;
}

.btn.operator:hover {
  background: #d48a0a;
}

.btn.equals {
  background: #f59e0b;
  color: #ffffff;
}

.btn.equals:hover {
  background: #d48a0a;
}

.btn.clear {
  background: #a0a0b8;
  color: #1a1a2e;
}

.btn.clear:hover {
  background: #b8b8ce;
}

.btn.toggle,
.btn.percent,
.btn.decimal {
  background: #3b3b54;
  color: #ffffff;
}

.btn.toggle:hover,
.btn.percent:hover,
.btn.decimal:hover {
  background: #4a4a66;
}
</style>