import Vue from 'vue'
import App from './App.vue'
import router from './router'
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import './assets/css/HomeView/style.css'
import './assets/css/HomeView/bootstrap.min.css'
import {APP_CONFIG} from "@/config/app";
import {setupInterceptors} from "@/request/interceptor";
// fade/zoom 等
import 'element-ui/lib/theme-chalk/base.css';
// collapse 展开折叠
import CollapseTransition from 'element-ui/lib/transitions/collapse-transition';

Vue.component(CollapseTransition.name, CollapseTransition)

Vue.config.productionTip = false


//Vue.use()用于安装插件，而不是将它们传递给Vue实例。正确的做法是在Vue实例外部使用Vue.use()来安装Element UI插件。
Vue.use(ElementUI);
Vue.prototype.$appConfig = APP_CONFIG;
setupInterceptors();

new Vue({
    router,
    render: (h) => h(App)
}).$mount('#app')


