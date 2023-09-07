import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import Ring3D from '@/views/highcharts/3dring/type'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'HelloWorld',
      component: HelloWorld
    },
    {
        path: '/3d-ring',
        name: '3d-ring',
        component: Ring3D
    }
  ]
})
