import axios from 'axios'
import type { ApiResponse } from '@/types/api'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload && typeof payload.code === 'number' && payload.code !== 0) {
      return Promise.reject(new Error(payload.message || 'request failed'))
    }
    return response
  },
  (error) => Promise.reject(error),
)

export async function get<T>(url: string, params?: Record<string, unknown>) {
  const { data } = await http.get<ApiResponse<T>>(url, { params })
  return data.data
}

export async function post<T>(url: string, body?: unknown) {
  const { data } = await http.post<ApiResponse<T>>(url, body)
  return data.data
}

export default http
