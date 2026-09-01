<template>
  <div id="backend">
    <div class="top">
      <router-link to="/backend" class="logo">
        <img src="../../assets/images/cloud.png" class="couldLogo" alt=""> 云用地管理系统
      </router-link>
      <nav class="nav">
        <router-link to="/">首页</router-link>
        <div id="user-msg" v-show="user.id!==null&&user.username!==null" @click="openDialog">
          欢迎您，{{ user.username }}
          <img
              style="width: 40px; height: 40px;border-radius: 5px;background-color: white"
              v-if="user.img!==null" :src="filePath+user.img">
        </div>
      </nav>
    </div>
    <div class="main">
      <div class="navigation">
        <el-row class="tac">
          <el-col>
            <!-- router 模式：menu-item 的 index 即跳转路径；
                 default-active 绑定当前路由，激活项自动高亮 -->
            <el-menu
                :default-active="$route.path"
                class="el-menu-vertical-demo"
                background-color="#232626"
                text-color="#fff"
                router
                active-text-color="#20a895">
              <el-submenu index="user-mgmt" v-if="user.power===2">
                <template slot="title">
                  <i class="el-icon-user-solid"></i>
                  <span>用户管理</span>
                </template>
                <el-menu-item-group>
                  <el-menu-item index="/backend/customerA">普通用户</el-menu-item>
                  <el-menu-item index="/backend/customerB">加盟用户</el-menu-item>
                  <el-menu-item index="/backend/employee">平台员工</el-menu-item>
                </el-menu-item-group>
              </el-submenu>
              <el-submenu index="order-mgmt">
                <template slot="title">
                  <i class="el-icon-s-claim"></i>
                  <span slot="title">订单管理</span>
                </template>
                <el-menu-item-group>
                  <el-menu-item index="/backend/orderLand">用地订单</el-menu-item>
                  <el-menu-item index="/backend/orderProduct">产品订单</el-menu-item>
                </el-menu-item-group>
              </el-submenu>
              <el-menu-item index="/backend/land">
                <i class="el-icon-s-grid"></i>
                <span slot="title">用地管理</span>
              </el-menu-item>
              <el-menu-item index="/backend/product">
                <i class="el-icon-apple"></i>
                <span slot="title">产品管理</span>
              </el-menu-item>
              <el-menu-item index="/backend/pushMsg" v-if="user.power===2">
                <i class="el-icon-chat-line-round"></i>
                <span slot="title">推送与反馈</span>
              </el-menu-item>
            </el-menu>
          </el-col>
        </el-row>
      </div>
      <div class="content">
        <router-view :parentData="user"></router-view>
      </div>
    </div>
    <!--登录表单（统一组件，后台模式：仅登录）-->
    <LoginForm ref="loginForm" :frond="false" system-name="云用地管理系统" @login-success="onLoginSuccess"/>
  </div>
</template>

<script>
import router from "@/router";
import {APP_CONFIG} from "@/config/app";
import {clearAuthState, createEmptyUser, fetchCurrentUser} from "@/utils/auth";
import LoginForm from "@/components/LoginForm";

export default {
  name: "Backend",
  components: {
    LoginForm
  },
  data() {
    return {
      filePath: APP_CONFIG.resourceUrls.userFile,
      user: createEmptyUser()
    };
  },
  mounted() {
    this.openCheck();
  },
  watch: {
    '$route'() {
      if (this.$route.path === '/backend') {
        this.refreshParentComponent();
      }
    },
  },
  methods: {
    notifyInfo(message, title = '系统提示') {
      this.$notify.info({
        title,
        message,
        duration: 1000,
        showClose: false
      });
    },
    refreshParentComponent() {
      this.openCheck();
    },
    openLogin() {
      this.$refs.loginForm && this.$refs.loginForm.open();
    },
    async openCheck() {
      this.user = createEmptyUser();
      const {user, res: authRes} = await fetchCurrentUser(false);
      if (!user) {
        if (authRes) {
          this.notifyInfo(`${authRes.msg}!`, '登录提示');
          if (authRes.code === 20003) {
            await this.$router.push('/');
          }
        }
        this.openLogin();
        return;
      }
      if (user.power === 1) {
        sessionStorage.setItem('uid', user.id);
      } else {
        sessionStorage.removeItem('uid');
      }
      this.user = user;
    },
    onLoginSuccess(user) {
      if (user.power === 1) {
        sessionStorage.setItem('uid', user.id);
      } else {
        sessionStorage.removeItem('uid');
      }
      this.user = user;
    },
    openDialog() {
      this.$confirm('退出登录?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        clearAuthState();
        router.go(0);
      }).catch(() => {
      });
    }
  }
}
</script>

<style scoped>
/* ==================== 布局骨架（flex，固定尺寸替代原 vh/vw 混用） ====================
 * 原实现 13vh/87vh/15vw/85vw 导致侧栏宽度随窗口比例漂移，现改为：
 * 顶栏固定 80px、侧栏固定 220px、内容区 flex 自适应并独立滚动 */
#backend {
  height: 100vh;
  min-width: 1080px;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

/* 顶栏 */
.top {
  flex: 0 0 80px;
  display: flex;
  align-items: center;
  background-color: var(--color-primary);
  justify-content: space-between;
  padding: 0 24px;
}

.top > a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 2rem;
  color: #fff;
  font-weight: bolder;
}

.top > a:hover {
  color: var(--color-primary-muted);
}

/* 顶栏链接 */
.nav {
  display: flex;
  align-items: center;
}

.nav > a {
  display: inline-block;
  font-size: 1.4rem;
  color: #fff;
  font-weight: bolder;
  margin-right: 3vw;
}

.nav > a:hover {
  color: var(--color-primary-muted);
}

/* 侧边栏菜单用户图标 */
.el-icon-user-solid {
  font-size: 2rem;
}

/* 主体：侧栏 + 内容区 */
.main {
  flex: 1;
  display: flex;
  min-height: 0; /* 允许子元素收缩滚动 */
  overflow: hidden;
}

/* 侧边导航 */
.navigation {
  flex: 0 0 220px;
  background-color: var(--color-bg-dark);
  overflow-y: auto; /* 菜单超高时侧栏内滚动 */
}

/* 内容区 */
.content {
  flex: 1;
  background-color: #ffffff;
  overflow: auto;
}

/* element ui 去除导航栏边框 */
.el-menu {
  border-right: none !important;
}

/* element ui 加粗导航栏字体 */
.tac {
  font-weight: bolder;
}

#user-msg {
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1.4rem;
  margin-right: 2rem;
  color: #fff;
  font-weight: bolder;
  gap: 10px;
  cursor: pointer;
}
</style>
