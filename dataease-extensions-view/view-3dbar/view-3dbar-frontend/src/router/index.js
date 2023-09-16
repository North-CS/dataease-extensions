import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import Bar3D from '@/views/highcharts/3dbar/type'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'HelloWorld',
      component: HelloWorld
    },
    {
        path: '/3d-bar',
        name: '3d-bar',
        component: Bar3D
    }
  ]
})
