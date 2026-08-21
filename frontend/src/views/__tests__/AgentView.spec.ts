import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AgentView from '@/views/AgentView.vue'

vi.mock('@/api/qa', () => ({
  listCompanies: vi.fn(),
}))

vi.mock('@/api/agent', () => ({
  runAgent: vi.fn(),
}))

import { listCompanies } from '@/api/qa'
import { runAgent } from '@/api/agent'

describe('AgentView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(listCompanies).mockResolvedValue([
      { id: 1, stockCode: '600519', name: '贵州茅台' },
    ])
  })

  it('renders form and empty submit disabled', async () => {
    const wrapper = mount(AgentView)
    await flushPromises()
    expect(wrapper.text()).toContain('启动 Agent')
    expect(wrapper.find('button.primary').attributes('disabled')).toBeDefined()
  })

  it('shows steps and answer after run', async () => {
    vi.mocked(runAgent).mockResolvedValue({
      intentType: 'QA',
      question: '营收如何',
      answer: '营收稳健',
      companyName: '贵州茅台',
      preferredDocTypes: [],
      steps: [
        { name: '意图识别', tool: 'COMPANY_LOOKUP', status: 'DONE', note: '贵州茅台 · QA', latencyMs: 0 },
        { name: '问答检索', tool: 'QA', status: 'DONE', note: 'QA', latencyMs: 10 },
      ],
      citations: [],
      chunks: [],
      latencyMs: 42,
      insufficientEvidence: false,
    })

    const wrapper = mount(AgentView)
    await flushPromises()
    await wrapper.find('textarea').setValue('营收如何')
    await wrapper.find('button.primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('营收稳健')
    expect(wrapper.text()).toContain('意图识别')
  })

  it('shows empty-ish error when agent fails', async () => {
    vi.mocked(runAgent).mockRejectedValue(new Error('Agent 执行失败'))
    const wrapper = mount(AgentView)
    await flushPromises()
    await wrapper.find('textarea').setValue('营收如何')
    await wrapper.find('button.primary').trigger('click')
    await flushPromises()
    expect(wrapper.find('.error').text()).toContain('Agent 执行失败')
  })
})
