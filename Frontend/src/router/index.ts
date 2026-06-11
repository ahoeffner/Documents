import ChatView from '../views/ChatView.vue'
import CreateView from '../views/CreateView.vue'
import SearchView from '../views/SearchView.vue'
import { createRouter, createWebHistory } from 'vue-router'


export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: SearchView },
    { path: '/create', component: CreateView },
    { path: '/chat', component: ChatView }
  ]
})
