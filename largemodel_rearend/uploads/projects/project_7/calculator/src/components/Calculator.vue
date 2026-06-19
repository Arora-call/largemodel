<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ display }}</div>
      <div class="result">{{ result !== null ? result : '' }}</div>
    </div>
    <div class="buttons">
      <button class="btn clear" @click="clear">C</button>
      <button class="btn operator" @click="appendOperator('/')">÷</button>
      <button class="btn operator" @click="appendOperator('*')">×</button>
      <button class="btn operator" @click="backspace">⌫</button>

      <button class="btn number" @click="appendNumber('7')">7</button>
      <button class="btn number" @click="appendNumber('8')">8</button>
      <button class="btn number" @click="appendNumber('9')">9</button>
      <button class="btn operator" @click="appendOperator('-')">−</button>

      <button class="btn number" @click="appendNumber('4')">4</button>
      <button class="btn number" @click="appendNumber('5')">5</button>
      <button class="btn number" @click="appendNumber('6')">6</button>
      <button class="btn operator" @click="appendOperator('+')">+</button>

      <button class="btn number" @click="appendNumber('1')">1</button>
      <button class="btn number" @click="appendNumber('2')">2</button>
      <button class="btn number" @click="appendNumber('3')">3</button>
      <button class="btn equal" @click="calculate">=</button>

      <button class="btn number zero" @click="appendNumber('0')">0</button>
      <button class="btn number" @click="appendDot">.</button>
      <button class="btn number" @click="appendNumber('00')">00</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const display = ref('0')
const result = ref(null)
const previousValue = ref(null)
const operator = ref(null)
const resetNext = ref(false)

function appendNumber(num) {
  if (resetNext.value) {
    display.value = ''
    resetNext.value = false
  }
  if (display.value === '0' && num !== '.') {
    display.value = num
  } else {
    display.value += num
  }
}

function appendDot() {
  if (resetNext.value) {
    display.value = '0.'
    resetNext.value = false
    return
  }
  if (!display.value.includes('.')) {
    display.value += '.'
  }
}

function appendOperator(op) {
  if (operator.value && !resetNext.value) {
    calculate()
  }
  previousValue.value = parseFloat(display.value)
  operator.value = op
  resetNext.value = true
  if (op === '+') display.value += ' + '
  else if (op === '-') display.value += ' - '
  else if (op === '*') display.value += ' × '
  else if (op === '/') display.value += ' ÷ '
}

function calculate() {
  if (operator.value === null || previousValue.value === null) return
  const current = parseFloat(display.value.split(' ').pop())
  let res
  switch (operator.value) {
    case '+':
      res = previousValue.value + current
      break
    case '-':
      res = previousValue.value - current
      break
    case '*':
      res = previousValue.value * current
      break
    case '/':
      res = current !== 0 ? previousValue.value / current : '错误'
      break
    default:
      return
  }
  result.value = res
  display.value = previousValue.value + ' ' + getOpSymbol(operator.value) + ' ' + current
  operator.value = null
  previousValue.value = null
  resetNext.value = true
}

function getOpSymbol(op) {
  switch (op) {
    case '+': return '+'
    case '-': return '−'
    case '*': return '×'
    case '/': return '÷'
    default: return ''
  }
}

function clear() {
  display.value = '0'
  result.value = null
  previousValue.value = null
  operator.value = null
  resetNext.value = false
}

function backspace() {
  if (display.value.length <= 1 || display.value === '0') {
    display.value = '0'
  } else {
    display.value = display.value.slice(0, -1).trim()
  }
}
</script>

<style scoped>
.calculator {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  padding: 20px;
}

.display {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  min-height: 70px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
}

.expression {
  font-size: 1.2rem;
  color: #666;
  word-break: break-all;
  min-height: 1.5em;
}

.result {
  font-size: 2rem;
  font-weight: bold;
  color: #222;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.btn {
  padding: 18px 0;
  font-size: 1.3rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
}

.btn:active {
  transform: scale(0.95);
}

.number {
  background: #e8e8e8;
  color: #333;
}

.number:hover {
  background: #ddd;
}

.operator {
  background: #ff9500;
  color: #fff;
}

.operator:hover {
  background: #e08600;
}

.clear {
  background: #ff3b30;
  color: #fff;
}

.clear:hover {
  background: #d62d20;
}

.equal {
  background: #007aff;
  color: #fff;
  grid-row: span 2;
}

.equal:hover {
  background: #0062cc;
}

.zero {
  grid-column: span 2;
}
</style>