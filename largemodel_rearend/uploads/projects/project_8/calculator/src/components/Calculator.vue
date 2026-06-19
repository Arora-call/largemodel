<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ expression || '0' }}</div>
      <div class="result">{{ result !== null ? result : '' }}</div>
    </div>

    <div class="buttons">
      <button
        v-for="btn in buttons"
        :key="btn.label"
        :class="['btn', btn.type]"
        @click="handleClick(btn)"
        :style="btn.span ? { gridColumn: `span ${btn.span}` } : {}"
      >
        {{ btn.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const expression = ref('')
const result = ref(null)
const currentInput = ref('')
const operator = ref(null)
const previousValue = ref(null)
const shouldResetInput = ref(false)

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
  { label: '-', type: 'operator', value: '-' },
  { label: '1', type: 'number' },
  { label: '2', type: 'number' },
  { label: '3', type: 'number' },
  { label: '+', type: 'operator', value: '+' },
  { label: '0', type: 'number', span: 2 },
  { label: '.', type: 'dot' },
  { label: '=', type: 'equal' },
]

function handleClick(btn) {
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
    case 'equal':
      calculate()
      break
    case 'clear':
      clear()
      break
    case 'toggle':
      toggleSign()
      break
    case 'percent':
      percent()
      break
  }
}

function inputNumber(num) {
  if (shouldResetInput.value) {
    currentInput.value = ''
    shouldResetInput.value = false
  }
  if (currentInput.value === '0' && num !== '.') {
    currentInput.value = num
  } else {
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
    currentInput.value += '.'
  }
  updateExpression()
}

function inputOperator(op) {
  const current = parseFloat(currentInput.value)
  if (isNaN(current)) return

  if (operator.value && !shouldResetInput.value) {
    calculate()
  }

  previousValue.value = current
  operator.value = op
  shouldResetInput.value = true
  expression.value = `${formatNumber(current)} ${getOperatorSymbol(op)} `
  result.value = null
}

function calculate() {
  const current = parseFloat(currentInput.value)
  if (isNaN(current) || operator.value === null || previousValue.value === null) return

  let calcResult
  const prev = previousValue.value

  switch (operator.value) {
    case '+':
      calcResult = prev + current
      break
    case '-':
      calcResult = prev - current
      break
    case '*':
      calcResult = prev * current
      break
    case '/':
      calcResult = current === 0 ? 'Error' : prev / current
      break
    default:
      return
  }

  result.value = calcResult
  expression.value = `${formatNumber(prev)} ${getOperatorSymbol(operator.value)} ${formatNumber(current)} =`
  currentInput.value = String(calcResult)
  previousValue.value = null
  operator.value = null
  shouldResetInput.value = true
}

function clear() {
  expression.value = ''
  result.value = null
  currentInput.value = ''
  operator.value = null
  previousValue.value = null
  shouldResetInput.value = false
}

function toggleSign() {
  if (currentInput.value && currentInput.value !== '0') {
    if (currentInput.value.startsWith('-')) {
      currentInput.value = currentInput.value.slice(1)
    } else {
      currentInput.value = '-' + currentInput.value
    }
    updateExpression()
  }
}

function percent() {
  const num = parseFloat(currentInput.value)
  if (!isNaN(num)) {
    currentInput.value = String(num / 100)
    updateExpression()
  }
}

function updateExpression() {
  if (!operator.value) {
    expression.value = currentInput.value || '0'
  } else {
    expression.value = `${formatNumber(previousValue.value)} ${getOperatorSymbol(operator.value)} ${currentInput.value || '0'}`
  }
}

function formatNumber(num) {
  if (num === null || num === undefined) return '0'
  const str = String(num)
  if (str.length > 12) {
    return Number(num).toExponential(4)
  }
  return str
}

function getOperatorSymbol(op) {
  switch (op) {
    case '+': return '+'
    case '-': return '−'
    case '*': return '×'
    case '/': return '÷'
    default: return op
  }
}
</script>

<style scoped>
.calculator {
  width: 320px;
  background: #1c1c1e;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.display {
  min-height: 100px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 10px 5px 20px;
  margin-bottom: 10px;
}

.expression {
  font-size: 24px;
  color: #8e8e93;
  word-break: break-all;
  text-align: right;
  min-height: 32px;
  max-width: 100%;
  overflow-x: auto;
  white-space: nowrap;
}

.result {
  font-size: 48px;
  font-weight: 300;
  color: #ffffff;
  min-height: 56px;
  line-height: 1.2;
  word-break: break-all;
  text-align: right;
  max-width: 100%;
  overflow-x: auto;
  white-space: nowrap;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.btn {
  padding: 16px 0;
  border: none;
  border-radius: 50%;
  font-size: 24px;
  font-weight: 400;
  cursor: pointer;
  transition: filter 0.15s, transform 0.1s;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.btn:active {
  transform: scale(0.92);
}

.btn.number {
  background: #333336;
  color: #ffffff;
}

.btn.number:hover {
  filter: brightness(1.3);
}

.btn.operator {
  background: #ff9f0a;
  color: #ffffff;
}

.btn.operator:hover {
  filter: brightness(1.15);
}

.btn.clear,
.btn.toggle,
.btn.percent {
  background: #a5a5a8;
  color: #1c1c1e;
}

.btn.clear:hover,
.btn.toggle:hover,
.btn.percent:hover {
  filter: brightness(1.2);
}

.btn.equal {
  background: #ff9f0a;
  color: #ffffff;
}

.btn.equal:hover {
  filter: brightness(1.15);
}

.btn.dot {
  background: #333336;
  color: #ffffff;
}

.btn.dot:hover {
  filter: brightness(1.3);
}

/* 0 按钮跨两列 */
.btn[style*="span"] {
  border-radius: 40px;
  text-align: left;
  padding-left: 28px;
}
</style>