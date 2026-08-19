import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 统一处理后端 Result 结构：code 200 返回 data，否则抛出后端 msg
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code === 200) {
      return body.data
    }
    return Promise.reject(new Error(body?.msg || '请求失败'))
  },
  (error) => {
    return Promise.reject(new Error(error.response?.data?.msg || error.message || '网络错误'))
  }
)

// 设置当前用户 ID，之后所有请求自动携带 X-User-Id 请求头
export function setUserId(userId) {
  request.defaults.headers.common['X-User-Id'] = String(userId)
}

export function getUserList() {
  return request.get('/user/list')
}

export function getActivityList() {
  return request.get('/seckill/activity/list')
}

export function seckill(activityId) {
  return request.post(`/seckill/${activityId}`)
}

export function getResult(activityId) {
  return request.get(`/seckill/result/${activityId}`)
}
