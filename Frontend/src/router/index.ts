import { createRouter, createWebHistory } from 'vue-router'
import SearchView from '../views/SearchView.vue'
import CreateView from '../views/CreateView.vue'
import ChatView from '../views/ChatView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: SearchView },
    { path: '/create', component: CreateView },
    { path: '/chat', component: ChatView }
  ]
})
