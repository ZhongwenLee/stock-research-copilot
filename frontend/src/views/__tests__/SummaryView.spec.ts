import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import SummaryView from '@/views/SummaryView.vue'

vi.mock('@/api/qa', () => ({
  listCompanies: vi.fn(),
}))

vi.mock('@/api/summary', () => ({
  generateSummary: vi.fn(),
}))

vi.mock('@/api/history', () => ({
  listSummaryHistory: vi.fn(),
  getSummaryHistory: vi.fn(),
}))

import { listCompanies } from '@/api/qa'
import { generateSummary } from '@/api/summary'
import { listSummaryHistory } from '@/api/history'

describe('SummaryView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(listCompanies).mockResolvedValue([
      { id: 1, stockCode: '600519', name: '贵州茅台' },
    ])
    vi.mocked(listSummaryHistory).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 5,
    })
  })

  it('disables generate until company selected', async () => {
    const wrapper = mount(SummaryView)
    await flushPromises()
    expect(wrapper.find('button.primary').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('结构化研究摘要')
  })

  it('shows generated summary result', async () => {
    vi.mocked(generateSummary).mockResolvedValue({
      summaryId: 3,
      companyId: 1,
      companyName: '贵州茅台',
      stockCode: '600519',
      mode: 'FAST',
      title: '贵州茅台 研究摘要',
      overview: '公司概况良好',
      sections: [{ title: '公司概况', content: '公司概况良好' }],
      citations: [],
      chunks: [],
      docTypes: [],
      latencyMs: 88,
      insufficientEvidence: false,
    })

    const wrapper = mount(SummaryView)
    await flushPromises()

    await wrapper.find('select').setValue('1')
    await wrapper.find('button.primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('公司概况良好')
  })
})
