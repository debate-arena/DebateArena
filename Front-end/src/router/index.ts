import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/Home.vue'),
  },
  {
    path: '/matching',
    name: 'Matching',
    component: () => import('@/pages/Matching.vue'),
  },
  {
    path: '/login/success',
    name: 'LoginSuccess',
    component: () => import('@/pages/LoginSuccess.vue'),
  },
  {
    path: '/debate-room/:id',
    name: 'DebateRoom',
    component: () => import('@/pages/DebateRoom.vue'),
  },
  {
    path: '/debate/:id',
    name: 'Debate',
    component: () => import('@/pages/DebateRoom.vue'),
  },
  {
    path: '/custom-room',
    name: 'CustomRoom',
    component: () => import('@/pages/CustomRoom.vue'),
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router 