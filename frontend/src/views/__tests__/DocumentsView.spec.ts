import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import DocumentsView from '@/views/DocumentsView.vue'

vi.mock('@/api/qa', () => ({
  listCompanies: vi.fn(),
}))

vi.mock('@/api/documents', () => ({
  listDocuments: vi.fn(),
  reprocessDocument: vi.fn(),
  getDocumentFileUrl: vi.fn((id: number) => `/api/v1/documents/${id}/file`),
}))

vi.mock('@/api/chunks', () => ({
  listChunksByDocument: vi.fn(),
}))

import { listCompanies } from '@/api/qa'
import { listDocuments } from '@/api/documents'
import { listChunksByDocument } from '@/api/chunks'

describe('DocumentsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(listCompanies).mockResolvedValue([
      { id: 1, stockCode: '600519', name: '贵州茅台' },
    ])
    vi.mocked(listChunksByDocument).mockResolvedValue([])
  })

  it('renders empty state when no documents', async () => {
    vi.mocked(listDocuments).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
    })

    const wrapper = mount(DocumentsView)
    await flushPromises()

    expect(wrapper.text()).toContain('文档管理')
    expect(wrapper.text()).toContain('暂无文档')
  })

  it('renders document list after load', async () => {
    vi.mocked(listDocuments).mockResolvedValue({
      records: [
        {
          id: 11,
          companyId: 1,
          title: '2024 年报',
          docType: 'FINANCIAL_REPORT',
          processStatus: 'DONE',
        },
      ],
      total: 1,
      pageNum: 1,
      pageSize: 10,
    })

    const wrapper = mount(DocumentsView)
    await flushPromises()

    expect(wrapper.text()).toContain('2024 年报')
    expect(wrapper.text()).toContain('文档列表')
  })

  it('shows error when list fails', async () => {
    vi.mocked(listDocuments).mockRejectedValue(new Error('加载失败'))
    const wrapper = mount(DocumentsView)
    await flushPromises()
    expect(wrapper.find('.error').text()).toContain('加载失败')
  })
})
