<template>
  <div class="calculator">
    <div class="display">{{ display }}</div>
    <div class="buttons">
      <button v-for="btn in buttons" :key="btn" @click="handleClick(btn)" :class="{ operator: isOperator(btn) }">
        {{ btn }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const display = ref('0')
let currentInput = ''
let operator = ''
let previousValue = null
let waitingForOperand = false

const buttons = [
  '7', '8', '9', '/',
  '4', '5', '6', '*',
  '1', '2', '3', '-',
  'C', '0', '=', '+'
]

function isOperator(btn) {
  return ['+', '-', '*', '/'].includes(btn)
}

function handleClick(btn) {
  if (btn === 'C') {
    reset()
    return
  }

  if (btn === '=') {
    if (operator && previousValue !== null) {
      const result = compute(previousValue, parseFloat(currentInput), operator)
      display.value = String(result)
      previousValue = result
      currentInput = ''
      operator = ''
      waitingForOperand = true
    }
    return
  }

  if (isOperator(btn)) {
    if (operator && currentInput) {
      const result = compute(previousValue, parseFloat(currentInput), operator)
      display.value = String(result)
      previousValue = result
      currentInput = ''
    } else if (currentInput) {
      previousValue = parseFloat(currentInput)
      currentInput = ''
    }
    operator = btn
    waitingForOperand = true
    return
  }

  // 数字
  if (waitingForOperand) {
    currentInput = btn === '.' ? '0.' : btn
    waitingForOperand = false
  } else {
    if (btn === '.' && currentInput.includes('.')) return
    currentInput = currentInput === '0' && btn !== '.' ? btn : currentInput + btn
  }
  display.value = currentInput
}

function compute(a, b, op) {
  switch (op) {
    case '+': return a + b
    case '-': return a - b
    case '*': return a * b
    case '/': return b === 0 ? '错误' : a / b
    default: return b
  }
}

function reset() {
  display.value = '0'
  currentInput = ''
  operator = ''
  previousValue = null
  waitingForOperand = false
}
</script>

<style scoped>
.calculator {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  width: 280px;
}

.display {
  background: #f0f0f0;
  padding: 20px;
  text-align: right;
  font-size: 2em;
  border-radius: 10px;
  margin-bottom: 20px;
  min-height: 60px;
  overflow: hidden;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

button {
  padding: 20px;
  font-size: 1.2em;
  border: none;
  border-radius: 10px;
  background: #e0e0e0;
  cursor: pointer;
  transition: background 0.2s;
}

button:hover {
  background: #d0d0d0;
}

button.operator {
  background: #f5923e;
  color: white;
}

button.operator:hover {
  background: #e5832e;
}

button:last-child {
  background: #4caf50;
  color: white;
}

button:last-child:hover {
  background: #45a049;
}
</style>