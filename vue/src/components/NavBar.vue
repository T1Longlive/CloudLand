<template>
  <div>
    <!-- ==================== 首页模式：透明导航，滚动后变品牌绿 ==================== -->
    <header v-if="mode === 'home'" class="header fixed-top">
      <div class="container">
        <div class="row align-items-center">
          <a href="/#home" class="logo mr-auto"> <img src="../assets/images/cloud.png" class="couldLogo" alt=""> 云用地
          </a>
          <nav class="nav">
            <a href="/#home">首页</a>
            <a href="/#about">介绍</a>
            <a href="/#menu">功能</a>
            <a href="/#contact">联系我们</a>
            <a href="/#newsletter">订阅</a>
            <a href="/backend">后台</a>
          </nav>
          <div class="icons">
            <div id="menu-btn" class="fas fa-bars"></div>
            <div id="login-btn" class="fas fa-user" @click="openLogin"
                 v-show="user.id===null||user.username===null"></div>
            <div id="user-msg" v-show="user.id!==null&&user.username!==null" @click="openDialog">
              欢迎您，{{ user.username }}
              <el-image
                  style="width: 40px; height: 40px;background-color: white;border-radius: 5px;"
                  v-if="user.img!==null"
                  :src="filePath+user.img"
              ></el-image>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- ==================== 内页模式：固定品牌绿顶栏 ==================== -->
    <div v-else class="page-top">
      <div class="page-top-inner">
        <router-link to="/" class="logo">
          <img src="../assets/images/cloud.png" class="couldLogo" alt=""> 云用地
        </router-link>
        <div class="page-top-right">
          <router-link to="/">首页</router-link>
          <router-link to="/user" class="user-entry">
            <div class="user-user-msg" v-show="user.username!==null">
              <img v-if="user.img!==null" :src="filePath+user.img" id="uImg"
                   style="width: 40px; height: 40px;background-color: white;border-radius: 5px;">
              <span v-if="user.username!==null">{{ user.username }}</span>
            </div>
            <div class="user-user-msg" v-show="user.username===null">未登录</div>
          </router-link>
        </div>
      </div>
    </div>

    <!--登录/注册表单（仅首页模式提供）-->
    <LoginForm v-if="mode === 'home'" ref="loginForm" frond @login-success="onLoginSuccess"/>
  </div>
</template>
<script>
import {myFunction} from '@/assets/js/script';
import router from "@/router";
import {APP_CONFIG} from "@/config/app";
import {createEmptyUser, fetchCurrentUser} from "@/utils/auth";
import LoginForm from "@/components/LoginForm";

/**
 * 前台统一导航（原 Top.vue 与 Top2.vue 合并）
 * - mode="home"：首页，透明导航（滚动变绿）+ 锚点菜单 + 登录/注册弹窗
 * - mode="page"：列表/详情/个人中心等内页，固定品牌绿顶栏 + 用户入口
 * 两种模式都会恢复登录态，并把 userId 写入 sessionStorage（详情页加购依赖）
 */
export default {
  name: "NavBar",
  components: {
    LoginForm,
  },
  props: {
    mode: {
      type: String,
      default: 'page',
      validator: value => ['home', 'page'].includes(value)
    }
  },
  data() {
    return {
      filePath: APP_CONFIG.resourceUrls.userFile,
      user: createEmptyUser(),
      cleanupHeader: null
    };
  },
  mounted() {
    if (this.mode === 'home') {
      this.cleanupHeader = myFunction();
    }
    this.openCheck();
  },
  beforeDestroy() {
    if (this.cleanupHeader) {
      this.cleanupHeader();
    }
  },
  methods: {
    openLogin() {
      this.$refs.loginForm && this.$refs.loginForm.open();
    },
    notifyInfo(message, title = '系统提示') {
      this.$notify.info({
        title,
        message,
        duration: 1000,
        showClose: false
      });
    },
    async openCheck() {
      this.user = createEmptyUser();
      const {user, res: authRes} = await fetchCurrentUser(true);
      if (!user) {
        if (authRes && authRes.code !== 503) {
          this.notifyInfo(`${authRes.msg}!`, '登录提示');
        }
        return;
      }
      this.user = user;
      // 详情页加购等流程依赖 sessionStorage.userID，统一在此维护
      sessionStorage.setItem('userID', user.id);
    },
    async onLoginSuccess(user) {
      this.user = user;
      sessionStorage.setItem('userID', user.id);
      const currentRoute = router.currentRoute;
      if (currentRoute && currentRoute.path !== '/') {
        await router.push({path: '/'});
      }
    },
    openDialog() {
      router.push({path: '/user'}).then(() => null);
    }
  }
}
</script>

<style scoped>
/* ==================== 内页模式顶栏 ==================== */
.page-top {
  width: 100%;
  height: 13vh;
  min-height: 90px;
  background-color: var(--color-primary);
}

.page-top-inner {
  width: 80%;
  height: 100%;
  margin: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 900px;
}

.page-top-inner .logo {
  display: inline-block;
  font-size: 2.5rem;
  color: #fff;
  font-weight: bolder;
  padding-left: 1vw;
}

.page-top-inner .logo:hover {
  color: var(--color-primary-muted);
}

.page-top-right {
  font-weight: bold;
  font-size: 1.5rem;
  color: #fff;
  display: flex;
  align-items: center;
}

.page-top-right > a {
  margin-right: 30px;
  color: #fff;
}

.page-top-right > a:hover {
  color: var(--color-primary-muted);
}

.user-user-msg {
  font-size: 1.5rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

#uImg:hover {
  border: none;
  box-shadow: 2px 2px 5px rgba(23, 20, 20, 0.8);
}
</style>
