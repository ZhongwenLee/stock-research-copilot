import { get } from './http'

export interface HealthInfo {
  status: string
  service: string
  timestamp: string
}

export function fetchHealth() {
  return get<HealthInfo>('/v1/health')
}
