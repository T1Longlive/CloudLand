import Vue from 'vue'
import App from './App.vue'
import router from './router'

// ==================== Element UI 按需引入（JS） ====================
// 仅引入项目实际使用的组件与服务，替代原先的全量 Vue.use(ElementUI)，
// 打包体积从全量 ~650KB 降至实际所需部分
import Button from 'element-ui/lib/button'
import Carousel from 'element-ui/lib/carousel'
import CarouselItem from 'element-ui/lib/carousel-item'
import Cascader from 'element-ui/lib/cascader'
import Col from 'element-ui/lib/col'
import Container from 'element-ui/lib/container'
import Dialog from 'element-ui/lib/dialog'
import Form from 'element-ui/lib/form'
import FormItem from 'element-ui/lib/form-item'
import Image from 'element-ui/lib/image'
import Input from 'element-ui/lib/input'
import InputNumber from 'element-ui/lib/input-number'
import Menu from 'element-ui/lib/menu'
import MenuItem from 'element-ui/lib/menu-item'
import MenuItemGroup from 'element-ui/lib/menu-item-group'
import Option from 'element-ui/lib/option'
import Pagination from 'element-ui/lib/pagination'
import Row from 'element-ui/lib/row'
import Select from 'element-ui/lib/select'
import Submenu from 'element-ui/lib/submenu'
import Switch from 'element-ui/lib/switch'
import Table from 'element-ui/lib/table'
import TableColumn from 'element-ui/lib/table-column'
import Tag from 'element-ui/lib/tag'
import Tooltip from 'element-ui/lib/tooltip'
import Upload from 'element-ui/lib/upload'

// 服务类（this.$message / $confirm / $notify / $loading）
import Message from 'element-ui/lib/message'
import MessageBox from 'element-ui/lib/message-box'
import Notification from 'element-ui/lib/notification'
import Loading from 'element-ui/lib/loading'

// collapse 展开折叠过渡
import CollapseTransition from 'element-ui/lib/transitions/collapse-transition'

const components = [
  Button, Carousel, CarouselItem, Cascader, Col, Container, Dialog,
  Form, FormItem, Image, Input, InputNumber, Menu, MenuItem, MenuItemGroup,
  Option, Pagination, Row, Select, Submenu, Switch, Table, TableColumn, Tag,
  Tooltip, Upload
]
components.forEach(component => Vue.use(component))

Vue.use(Loading)
Vue.component(CollapseTransition.name, CollapseTransition)
Vue.prototype.$message = Message
Vue.prototype.$confirm = MessageBox.confirm
Vue.prototype.$notify = Notification

// ==================== 全局样式（引入顺序即层叠顺序） ====================
// 1. 设计变量（全站唯一色值来源）
import '@/styles/tokens.css'
// 2. Element 基础样式 + 按需组件样式
import 'element-ui/lib/theme-chalk/base.css'
import 'element-ui/lib/theme-chalk/button.css'
import 'element-ui/lib/theme-chalk/carousel.css'
import 'element-ui/lib/theme-chalk/carousel-item.css'
import 'element-ui/lib/theme-chalk/cascader.css'
/* checkbox 样式保留：el-table 选择列（type="selection"）内部渲染复选框 */
import 'element-ui/lib/theme-chalk/checkbox.css'
import 'element-ui/lib/theme-chalk/col.css'
import 'element-ui/lib/theme-chalk/container.css'
import 'element-ui/lib/theme-chalk/dialog.css'
import 'element-ui/lib/theme-chalk/form.css'
import 'element-ui/lib/theme-chalk/form-item.css'
import 'element-ui/lib/theme-chalk/image.css'
import 'element-ui/lib/theme-chalk/input.css'
import 'element-ui/lib/theme-chalk/input-number.css'
import 'element-ui/lib/theme-chalk/menu.css'
import 'element-ui/lib/theme-chalk/menu-item.css'
import 'element-ui/lib/theme-chalk/menu-item-group.css'
import 'element-ui/lib/theme-chalk/option.css'
import 'element-ui/lib/theme-chalk/pagination.css'
import 'element-ui/lib/theme-chalk/row.css'
import 'element-ui/lib/theme-chalk/scrollbar.css'
import 'element-ui/lib/theme-chalk/select.css'
import 'element-ui/lib/theme-chalk/submenu.css'
import 'element-ui/lib/theme-chalk/switch.css'
import 'element-ui/lib/theme-chalk/table.css'
import 'element-ui/lib/theme-chalk/table-column.css'
import 'element-ui/lib/theme-chalk/tag.css'
import 'element-ui/lib/theme-chalk/tooltip.css'
import 'element-ui/lib/theme-chalk/upload.css'
import 'element-ui/lib/theme-chalk/message.css'
import 'element-ui/lib/theme-chalk/message-box.css'
import 'element-ui/lib/theme-chalk/notification.css'
import 'element-ui/lib/theme-chalk/loading.css'
// 3. 模板基础样式与 Bootstrap 工具类
import './assets/css/HomeView/style.css'
import './assets/css/HomeView/bootstrap.min.css'
// 4. Element 品牌化覆盖与跨组件共享样式（必须置于以上样式之后）
import '@/styles/element-overrides.css'
import '@/styles/shared.css'

import {APP_CONFIG} from "@/config/app";
import {setupInterceptors} from "@/request/interceptor";

Vue.config.productionTip = false

Vue.prototype.$appConfig = APP_CONFIG;
setupInterceptors();

new Vue({
    router,
    render: (h) => h(App)
}).$mount('#app')
