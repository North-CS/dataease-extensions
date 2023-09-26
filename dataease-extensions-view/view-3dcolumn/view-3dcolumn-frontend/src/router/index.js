import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import Bar3D from '@/views/highcharts/3dcolumn/type'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'HelloWorld',
      component: HelloWorld
    },
    {
        path: '/3d-column',
        name: '3d-column',
        component: Bar3D
    }
  ]
})
