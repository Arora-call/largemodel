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
      >
        {{ btn.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const expression = ref('')
const result = ref(null)
const currentInput = ref('')
const operator = ref(null)
const previousValue = ref(null)
const shouldReset = ref(false)

const buttons = reactive([
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
  { label: '0', type: 'number zero' },
  { label: '.', type: 'decimal' },
  { label: '=', type: 'equals' }
])

function handleClick(btn) {
  if (btn.type === 'number') {
    inputNumber(btn.label)
  } else if (btn.type === 'decimal') {
    inputDecimal()
  } else if (btn.type === 'operator') {
    inputOperator(btn.value)
  } else if (btn.type === 'equals') {
    calculateResult()
  } else if (btn.type === 'clear') {
    clearAll()
  } else if (btn.type === 'toggle') {
    toggleSign()
  } else if (btn.type === 'percent') {
    percent()
  }
}

function inputNumber(num) {
  if (shouldReset.value) {
    currentInput.value = ''
    expression.value = ''
    result.value = null
    shouldReset.value = false
  }
  if (currentInput.value === '0' && num !== '.') {
    currentInput.value = num
  } else {
    currentInput.value += num
  }
  updateExpression(num)
}

function inputDecimal() {
  if (shouldReset.value) {
    currentInput.value = '0.'
    expression.value = '0.'
    result.value = null
    shouldReset.value = false
    return
  }
  if (!currentInput.value.includes('.')) {
    currentInput.value += '.'
    expression.value += '.'
  }
}

function inputOperator(op) {
  const current = parseFloat(currentInput.value)
  if (isNaN(current)) return

  if (previousValue.value !== null && !shouldReset.value) {
    const prev = previousValue.value
    const oldOp = operator.value
    let calc = 0
    switch (oldOp) {
      case '+': calc = prev + current; break
      case '-': calc = prev - current; break
      case '*': calc = prev * current; break
      case '/': calc = prev / current; break
    }
    previousValue.value = calc
    result.value = calc
    expression.value = calc + getOperatorSymbol(op)
  } else {
    previousValue.value = current
    expression.value = currentInput.value + getOperatorSymbol(op)
  }

  operator.value = op
  currentInput.value = ''
  shouldReset.value = false
}

function calculateResult() {
  const current = parseFloat(currentInput.value)
  if (operator.value === null || isNaN(current)) return

  const prev = previousValue.value
  let calc = 0
  switch (operator.value) {
    case '+': calc = prev + current; break
    case '-': calc = prev - current; break
    case '*': calc = prev * current; break
    case '/': 
      if (current === 0) {
        result.value = '错误'
        expression.value = '不能除以0'
        resetState()
        return
      }
      calc = prev / current
      break
  }

  result.value = calc
  expression.value = prev + getOperatorSymbol(operator.value) + current + '='
  previousValue.value = calc
  currentInput.value = String(calc)
  operator.value = null
  shouldReset.value = true
}

function clearAll() {
  expression.value = ''
  result.value = null
  currentInput.value = ''
  operator.value = null
  previousValue.value = null
  shouldReset.value = false
}

function toggleSign() {
  if (currentInput.value && currentInput.value !== '0') {
    if (currentInput.value.startsWith('-')) {
      currentInput.value = currentInput.value.slice(1)
      expression.value = currentInput.value
    } else {
      currentInput.value = '-' + currentInput.value
      expression.value = currentInput.value
    }
  }
}

function percent() {
  const num = parseFloat(currentInput.value)
  if (!isNaN(num)) {
    const val = num / 100
    currentInput.value = String(val)
    expression.value = currentInput.value
    result.value = val
  }
}

function updateExpression(num) {
  if (expression.value === '0' || expression.value === '') {
    expression.value = num
  } else {
    expression.value += num
  }
}

function getOperatorSymbol(op) {
  switch (op) {
    case '+': return ' + '
    case '-': return ' − '
    case '*': return ' × '
    case '/': return ' ÷ '
    default: return ''
  }
}

function resetState() {
  currentInput.value = ''
  operator.value = null
  previousValue.value = null
  shouldReset.value = true
}
</script>

<style scoped>
.calculator {
  width: 340px;
  background: #1c1c2e;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
}

.display {
  background: #0a0a1a;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  word-break: break-all;
  box-shadow: inset 0 4px 10px rgba(0, 0, 0, 0.4);
}

.expression {
  font-size: 22px;
  color: #9a9abf;
  line-height: 1.4;
  min-height: 32px;
}

.result {
  font-size: 42px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.2;
  min-height: 50px;
  margin-top: 4px;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.btn {
  padding: 18px 0;
  font-size: 22px;
  font-weight: 600;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.15s ease;
  background: #2d2d44;
  color: #ffffff;
  box-shadow: 0 4px 0 #111122;
  user-select: none;
}

.btn:active {
  transform: translateY(3px);
  box-shadow: 0 1px 0 #111122;
}

.btn.number {
  background: #2d2d44;
  color: #ffffff;
}

.btn.number:hover {
  background: #3d3d5c;
}

.btn.zero {
  grid-column: span 2;
}

.btn.operator {
  background: #f59e0b;
  color: #1c1c2e;
  box-shadow: 0 4px 0 #b87a0a;
}

.btn.operator:hover {
  background: #fbbf24;
}

.btn.equals {
  background: #10b981;
  color: #1c1c2e;
  box-shadow: 0 4px 0 #0a8f5f;
}

.btn.equals:hover {
  background: #34d399;
}

.btn.clear {
  background: #ef4444;
  color: #ffffff;
  box-shadow: 0 4px 0 #b91c1c;
}

.btn.clear:hover {
  background: #f87171;
}

.btn.toggle,
.btn.percent {
  background: #374151;
  color: #ffffff;
  box-shadow: 0 4px 0 #1f2937;
}

.btn.toggle:hover,
.btn.percent:hover {
  background: #4b5563;
}

.btn.decimal {
  background: #2d2d44;
  color: #ffffff;
}
</style>