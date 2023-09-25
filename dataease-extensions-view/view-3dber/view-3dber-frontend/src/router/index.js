import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import Bar3D from '@/views/highcharts/3dber/type'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'HelloWorld',
      component: HelloWorld
    },
    {
        path: '/3d-ber',
        name: '3d-ber',
        component: Bar3D
    }
  ]
})
