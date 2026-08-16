import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: DefaultLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'documents',
          name: 'documents',
          component: () => import('@/views/DocumentsView.vue'),
          meta: { title: '文档管理' },
        },
        {
          path: 'qa',
          name: 'qa',
          component: () => import('@/views/QaView.vue'),
          meta: { title: '研究问答' },
        },
        {
          path: 'summary',
          name: 'summary',
          component: () => import('@/views/SummaryView.vue'),
          meta: { title: '研究摘要' },
        },
        {
          path: 'agent',
          name: 'agent',
          component: () => import('@/views/AgentView.vue'),
          meta: { title: 'Agent' },
        },
      ],
    },
  ],
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : 'Stock Research Copilot'
  document.title = `${title} · Stock Research Copilot`
})

export default router
