import axios from 'axios'

const api = axios.create({
  baseURL: '/', // 실제 API 주소로 변경 필요
  timeout: 10000,
})

export default api 