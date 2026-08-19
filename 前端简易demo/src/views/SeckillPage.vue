<template>
  <n-layout>
    <n-layout-header bordered class="header">
      <div class="title">限时抢购系统</div>
      <div class="user-select-wrap">
        <n-select
          v-model:value="selectedUserId"
          :options="userOptions"
          placeholder="请选择当前用户"
          clearable
          class="user-select"
          @update:value="onUserChange"
        />
      </div>
    </n-layout-header>

    <n-layout-content content-style="padding: 24px;">
      <div class="section-title">活动列表</div>

      <n-empty v-if="activities.length === 0" description="暂无活动数据" style="padding: 40px 0" />

      <n-grid :cols="3" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
        <n-grid-item v-for="act in activities" :key="act.id" span="8 m:4 l:4">
          <n-card :title="act.activityName" size="medium" hoverable>
            <template #header-extra>
              <n-tag :type="statusTagType(act.status)">{{ statusText(act.status) }}</n-tag>
            </template>
            <div class="card-row">商品：{{ act.goodsName }}</div>
            <div class="card-row">
              秒杀价：<n-text strong type="error">¥{{ act.seckillPrice }}</n-text>
            </div>
            <div class="card-row">剩余库存：{{ act.stock }}</div>
            <div class="card-row">
              时间：{{ formatTime(act.startTime) }} ~ {{ formatTime(act.endTime) }}
            </div>
            <template #footer>
              <n-button
                type="primary"
                block
                :loading="loadingId === act.id"
                :disabled="!selectedUserId"
                @click="doSeckill(act)"
              >
                {{ selectedUserId ? '立即抢购' : '请先选择用户' }}
              </n-button>
            </template>
          </n-card>
        </n-grid-item>
      </n-grid>

      <div class="section-title">抢购结果</div>
      <n-alert
        v-if="resultText"
        :type="resultType"
        :title="resultTitle"
        closable
        @close="clearResult"
      >
        {{ resultText }}
      </n-alert>
      <n-empty v-else description="尚未发起抢购，选择一个活动试试吧" style="padding: 20px 0" />

      <n-space style="margin-top: 20px">
        <n-button secondary @click="loadActivities">刷新库存</n-button>
      </n-space>
    </n-layout-content>
  </n-layout>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useMessage } from 'naive-ui'
import {
  getUserList,
  getActivityList,
  seckill,
  getResult,
  setUserId
} from '../api/index.js'

const message = useMessage()

const selectedUserId = ref(null)
const userOptions = ref([])
const activities = ref([])
const loadingId = ref(null)

const resultText = ref('')
const resultType = ref('success')
const resultTitle = ref('')

let pollTimer = null
let currentActivity = null

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

function statusText(status) {
  if (status === 0) return '未开始'
  if (status === 1) return '进行中'
  return '已结束'
}

function statusTagType(status) {
  if (status === 1) return 'success'
  if (status === 0) return 'info'
  return 'default'
}

async function loadUsers() {
  try {
    const list = await getUserList()
    userOptions.value = list.map((u) => ({
      label: `${u.id} - ${u.username}`,
      value: u.id
    }))
  } catch (e) {
    message.error('加载用户列表失败：' + e.message)
  }
}

async function loadActivities() {
  try {
    activities.value = await getActivityList()
  } catch (e) {
    message.error('加载活动列表失败：' + e.message)
  }
}

function onUserChange(userId) {
  if (userId == null) return
  setUserId(userId)
  message.success('已切换用户：' + userId)
}

async function doSeckill(act) {
  if (selectedUserId.value == null) {
    message.warning('请先选择用户')
    return
  }
  setUserId(selectedUserId.value)
  loadingId.value = act.id
  resultText.value = ''
  currentActivity = act
  try {
    const msg = await seckill(act.id)
    resultType.value = 'success'
    resultTitle.value = '抢购成功'
    resultText.value = msg
    message.success(msg)
    startPolling()
  } catch (e) {
    resultType.value = 'error'
    resultTitle.value = '抢购失败'
    resultText.value = e.message
    message.error(e.message)
    loadActivities()
  } finally {
    loadingId.value = null
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!currentActivity) return
    try {
      const msg = await getResult(currentActivity.id)
      resultText.value = msg
      if (msg.includes('订单号')) {
        stopPolling()
        resultType.value = 'success'
        resultTitle.value = '下单结果'
        message.success(msg)
        loadActivities()
      }
    } catch (e) {
      stopPolling()
    }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function clearResult() {
  resultText.value = ''
  currentActivity = null
  stopPolling()
}

onMounted(() => {
  loadUsers()
  loadActivities()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
}

.title {
  font-size: 20px;
  font-weight: 600;
}

.user-select-wrap {
  width: 260px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.card-row {
  line-height: 1.9;
  color: #666;
}
</style>
