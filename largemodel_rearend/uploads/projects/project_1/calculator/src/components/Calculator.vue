<template>
  <div class="calculator">
    <div class="display">{{ display }}</div>
    <div class="buttons">
      <button class="btn clear" @click="clear">C</button>
      <button class="btn operator" @click="appendOperator('%')">%</button>
      <button class="btn operator" @click="appendOperator('/')">÷</button>
      <button class="btn operator" @click="appendOperator('*')">×</button>

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
      <button class="btn backspace" @click="backspace">⌫</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const display = ref('0')
const currentInput = ref('')
const operator = ref(null)
const previousValue = ref(null)
const waitingForOperand = ref(false)

function appendNumber(num) {
  if (waitingForOperand.value) {
    currentInput.value = num
    waitingForOperand.value = false
  } else {
    if (currentInput.value === '0' && num !== '.') {
      currentInput.value = num
    } else {
      if (currentInput.value.length >= 15) return
      currentInput.value += num
    }
  }
  display.value = currentInput.value
}

function appendDot() {
  if (waitingForOperand.value) {
    currentInput.value = '0.'
    waitingForOperand.value = false
    display.value = currentInput.value
    return
  }
  if (!currentInput.value.includes('.')) {
    currentInput.value += '.'
  }
  display.value = currentInput.value
}

function appendOperator(op) {
  if (operator.value && !waitingForOperand.value) {
    calculate()
  }
  previousValue.value = parseFloat(currentInput.value)
  operator.value = op
  waitingForOperand.value = true
}

function calculate() {
  if (operator.value === null || waitingForOperand.value) return
  const curr = parseFloat(currentInput.value)
  let result
  switch (operator.value) {
    case '+':
      result = previousValue.value + curr
      break
    case '-':
      result = previousValue.value - curr
      break
    case '*':
      result = previousValue.value * curr
      break
    case '/':
      if (curr === 0) {
        result = 'Error'
      } else {
        result = previousValue.value / curr
      }
      break
    case '%':
      result = previousValue.value % curr
      break
    default:
      return
  }
  currentInput.value = String(result)
  display.value = currentInput.value
  operator.value = null
  previousValue.value = null
  waitingForOperand.value = true
}

function clear() {
  currentInput.value = '0'
  display.value = '0'
  operator.value = null
  previousValue.value = null
  waitingForOperand.value = false
}

function backspace() {
  if (waitingForOperand.value) return
  if (currentInput.value.length === 1 || (currentInput.value.length === 2 && currentInput.value.includes('-'))) {
    currentInput.value = '0'
  } else {
    currentInput.value = currentInput.value.slice(0, -1)
  }
  display.value = currentInput.value
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
  padding: 20px;
  font-size: 2.5rem;
  text-align: right;
  margin-bottom: 20px;
  min-height: 80px;
  overflow-x: auto;
  white-space: nowrap;
  font-family: 'Courier New', monospace;
  font-weight: 300;
}
.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.btn {
  padding: 20px;
  border: none;
  border-radius: 8px;
  font-size: 1.4rem;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  font-weight: 500;
}
.btn:active {
  transform: scale(0.95);
}
.number {
  background: #f9f9f9;
  color: #333;
}
.number:hover {
  background: #e8e8e8;
}
.operator {
  background: #e3f2fd;
  color: #1976d2;
}
.operator:hover {
  background: #cddef7;
}
.equal {
  background: #1976d2;
  color: white;
  grid-row: span 2;
}
.equal:hover {
  background: #1565c0;
}
.clear {
  background: #ffebee;
  color: #d32f2f;
}
.clear:hover {
  background: #ffcdd2;
}
.backspace {
  background: #fff3e0;
  color: #e65100;
}
.backspace:hover {
  background: #ffe0b2;
}
.zero {
  grid-column: span 2;
}
</style>