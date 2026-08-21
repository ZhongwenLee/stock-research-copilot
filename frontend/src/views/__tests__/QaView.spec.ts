import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import QaView from '@/views/QaView.vue'

vi.mock('@/api/qa', () => ({
  listCompanies: vi.fn(),
  askQuestion: vi.fn(),
}))

vi.mock('@/api/history', () => ({
  listQuestionHistory: vi.fn(),
  getQuestionHistory: vi.fn(),
}))

import { listCompanies, askQuestion } from '@/api/qa'
import { listQuestionHistory } from '@/api/history'

describe('QaView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(listCompanies).mockResolvedValue([
      { id: 1, stockCode: '600519', name: '贵州茅台' },
    ])
    vi.mocked(listQuestionHistory).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 5,
    })
  })

  it('renders composer and disables submit when empty', async () => {
    const wrapper = mount(QaView)
    await flushPromises()

    expect(wrapper.text()).toContain('研究问题')
    const button = wrapper.find('button.primary')
    expect(button.attributes('disabled')).toBeDefined()
  })

  it('shows answer and citations after submit', async () => {
    vi.mocked(askQuestion).mockResolvedValue({
      questionId: 9,
      question: '营收如何',
      answer: '营收增长 [1]',
      intentType: 'QA',
      companyName: '贵州茅台',
      stockCode: '600519',
      insufficientEvidence: false,
      citations: [
        {
          chunkId: 1,
          documentId: 2,
          rankNo: 1,
          quoteText: '营收同比增长',
          documentTitle: '年报',
        },
      ],
      chunks: [{ id: 1, documentId: 2, companyId: 1, chunkIndex: 0, content: '营收同比增长' }],
      latencyMs: 120,
    })

    const wrapper = mount(QaView)
    await flushPromises()

    await wrapper.find('textarea').setValue('营收如何')
    await wrapper.find('button.primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('营收增长 [1]')
    expect(wrapper.text()).toContain('引用来源')
    expect(wrapper.text()).toContain('年报')
  })

  it('shows error state when ask fails', async () => {
    vi.mocked(askQuestion).mockRejectedValue(new Error('服务不可用'))
    const wrapper = mount(QaView)
    await flushPromises()

    await wrapper.find('textarea').setValue('营收如何')
    await wrapper.find('button.primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.error').text()).toContain('服务不可用')
  })
})
