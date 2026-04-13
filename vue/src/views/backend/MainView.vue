<template>
  <div id="backend">
    <div class="top">
      <router-link to="/backend" class="logo"><img src="../../assets/images/cloud.png" class="couldLogo" alt=""> 云用地管理系统
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
            <el-menu
                default-active="0"
                class="el-menu-vertical-demo"
                background-color="#232626"
                text-color="#fff"
                router
                active-text-color="#187062">
              <el-submenu index="1" :index="'/backend/customerA'" v-if="user.power===2">
                <template slot="title">
                  <i class="el-icon-user-solid"></i>
                  <span>用户管理</span>
                </template>
                <el-menu-item-group>
                  <el-menu-item index="1-1" :index="'/backend/customerA'">· 普通用户</el-menu-item>
                  <el-menu-item index="1-2" :index="'/backend/customerB'">· 加盟用户</el-menu-item>
                  <el-menu-item index="1-3" :index="'/backend/employee'">· 平台员工</el-menu-item>
                </el-menu-item-group>
              </el-submenu>
              <el-submenu index="2">
                <template slot="title">
                  <i class="el-icon-s-claim"></i>
                  <span slot="title">订单管理</span>
                </template>
                <el-menu-item-group>
                  <el-menu-item index="1-1" :index="'/backend/orderLand'">· 用地订单</el-menu-item>
                  <el-menu-item index="1-2" :index="'/backend/orderProduct'">· 产品订单</el-menu-item>
                </el-menu-item-group>
              </el-submenu>
              <el-menu-item index="3" :index="'/backend/land'">
                <i class="el-icon-s-grid"></i>
                <span slot="title">用地管理</span>
              </el-menu-item>
              <el-menu-item index="4" :index="'/backend/product'">
                <i class="el-icon-apple"></i>
                <span slot="title">产品管理</span>
              </el-menu-item>
              <el-menu-item index="4" :index="'/backend/pushMsg'" v-if="user.power===2">
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
    <div class="login-form" v-show="isLoginVisible">
      <form action="">
        <a href="#" class="logo mr-auto"> <img src="../../assets/images/cloud.png" class="couldLogo" alt=""> 云用地管理系统
        </a>
        <h3>让世界更有价值</h3>
        <input placeholder="请输入你的手机号" id="1" class="box" minlength="11" maxlength="11" v-model="user.phone">
        <input type="password" placeholder="请输入你的密码" id="2" class="box" minlength="6" maxlength="20"
               v-model="user.password">
        <div class="abc"></div>
        <input placeholder="请输入你的验证码" id="3" minlength="6" maxlength="6" class="box" v-model="code" style="width: 60%">
        <button type="button"
                class="link-btn-mail"
                style="width: 40%"
                @click="sendCode()"
                :class="{ 'disabled': isCounting }"
                :disabled="isCounting"
        >
          {{ buttonLabel }}
        </button>
        <div class="flex">
          <el-checkbox disabled>记住我</el-checkbox>
          <div class="mail">忘记密码?</div>
        </div>
        <button type="button" class="link-btn" @click="LoginUp">登录</button>
        <p class="account">云用地后台系统需要二级账号，若没有可联系我们申请</p>
      </form>
    </div>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import router from "@/router";
import {APP_CONFIG} from "@/config/app";
import {clearAuthState, createEmptyUser, fetchCurrentUser, loginWithPassword} from "@/utils/auth";

const PHONE_REGEX = /^1[3-9]\d{9}$/;
const CODE_REGEX = /^\S{6}$/;
const HAS_SPACE_REGEX = /\s/;

export default {
  name: "Backend",
  data() {
    return {
      code: null,
      isCounting: false,
      countdown: 59,
      countdownInterval: null,
      isLoginVisible: false,
      filePath: APP_CONFIG.resourceUrls.userFile,
      user: createEmptyUser()
    };
  },
  mounted() {
    this.openCheck();
  },
  beforeDestroy() {
    this.resetCodeCountdown();
  },
  computed: {
    buttonLabel() {
      return this.isCounting ? `${this.countdown} 秒后重试` : '发送验证码';
    },
  },
  watch: {
    isLoginVisible(newVisibility) {
      if (!newVisibility) {
        this.onLoginFormClose();
      }
    },
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
    notifySuccess(message, title = '登录成功') {
      this.$notify({
        title,
        message,
        type: 'success',
        duration: 1000,
        showClose: false
      });
    },
    resetCodeCountdown() {
      this.isCounting = false;
      this.countdown = 59;
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    },
    startCodeCountdown() {
      this.resetCodeCountdown();
      this.isCounting = true;
      this.countdownInterval = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown -= 1;
          return;
        }
        this.resetCodeCountdown();
      }, 1000);
    },
    refreshParentComponent() {
      this.openCheck();
    },
    checkFrom(code) {
      if (!this.user.phone) {
        this.notifyInfo('请填写手机号!');
        return;
      }
      if (!PHONE_REGEX.test(this.user.phone)) {
        this.notifyInfo('手机号格式有误!');
        return;
      }
      if (!this.user.password) {
        this.notifyInfo('请填写密码!');
        return;
      }
      if (this.user.password.length < 6 || HAS_SPACE_REGEX.test(this.user.password)) {
        this.notifyInfo('密码有误!');
        return;
      }
      if (code) {
        if (!this.code) {
          this.notifyInfo('请先获取验证码!');
          return;
        }
        if (!CODE_REGEX.test(this.code)) {
          this.notifyInfo('验证码有误!');
          return;
        }
      }
      return "pass";
    },
    async sendCode() {
      this.code = null;
      if (this.checkFrom(false) !== "pass") {
        return;
      }
      if (this.isCounting) {
        return;
      }
      this.startCodeCountdown();
      const {data: res} = await axiosInstance.post('/user/code', this.user);
      if (res.code === 30001) {
        alert("发送成功！验证码5分钟内有效");
        return;
      }
      this.resetCodeCountdown();
      alert("发送失败！请检查你的信息是否有效");
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
        this.isLoginVisible = true;
        return;
      }
      if (user.power === 1) {
        sessionStorage.setItem('uid', user.id);
      } else {
        sessionStorage.removeItem('uid');
      }
      this.user = user;
      this.isLoginVisible = false;
    },
    async LoginUp() {
      if (this.checkFrom(true) !== "pass") {
        return;
      }
      const {user, res: loginRes} = await loginWithPassword(this.user, {
        frond: false,
        remember: false,
        code: this.code
      });
      if (!user) {
        this.notifyInfo(`${loginRes.msg}!`, '登录提示');
        this.code = null;
        return;
      }
      this.resetCodeCountdown();
      if (user.power === 1) {
        sessionStorage.setItem('uid', user.id);
      } else {
        sessionStorage.removeItem('uid');
      }
      this.user = user;
      this.isLoginVisible = false;
      this.code = null;
      this.notifySuccess(`欢迎你，${this.user.username}`);
    },
    resetForm() {
      this.user = createEmptyUser();
      this.code = null;
    },
    onLoginFormClose() {
      this.resetCodeCountdown();
      this.code = null;
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

<style>
/*根据屏幕大小设置后台盒子大小*/
#backend {
  width: 100vw;
  height: 100vh;
}

/*顶栏商标页*/
.top {
  display: flex;
  width: 100vw;
  height: 13vh;
  align-items: center; /* 垂直居中 */
  background-color: #105147;
  /*background-color: #187062;*/
  justify-content: space-between; /* 将内容分散对齐，第一个 a 左对齐，nav 和 icons 右对齐 */
  padding: 10px; /* 添加内边距，根据需要调整 */
  min-width: 1050px;
}

/*顶栏商标和图标*/
.top > a {
  display: inline-block; /* 可选：使链接元素变成块级元素，以便设置宽度等属性 */
  font-size: 2.5rem;
  color: #fff;
  font-weight: bolder;
  padding-left: 1vw;
}

.top > a:hover {
  color: #71918b;
}

/*顶栏链接*/
.nav {
  display: flex;
  align-items: center; /* 垂直居中 */
  margin-left: auto; /* 右对齐 */
  margin-right: 2vw;
}

.nav > a {
  display: inline-block; /* 可选：使链接元素变成块级元素，以便设置宽度等属性 */
  font-size: 1.5rem;
  color: #fff;
  font-weight: bolder;
  margin-right: 3vw; /* 将<a>标签左移相对于盒子的左边距 */
}

.mail {
  font-size: 1.5rem;
  color: #105147;
  margin-left: auto;
  cursor: pointer; /* 将鼠标指针设置为手型指针 */
}

.mail:hover {
  color: #FF0000; /* 设置鼠标悬停时的颜色，您可以将 #FF0000 替换为所需的颜色代码 */
}

/*包括导航菜单和路由界面的大盒子*/
.main {
  display: flex;
}

/*导航菜单*/
.navigation {
  width: 15vw;
  height: 87vh;
  min-width: 200px;
  background-color: #232626;
}

/*路由页面盒子*/
.content {
  width: 85vw;
  height: 87vh;
  min-width: 870px;
  background-color: #ffffff;
  overflow: auto; /* 添加滚动条以处理内容溢出 */
}

/*element ui去除导航栏边框*/
.el-menu {
  border-right: none !important;;
}

/*element ui加粗导航栏字体*/
.tac {
  font-weight: bolder;
}

.el-notification.left {
  top: 50vh !important;
  right: 50vw !important;
}

.el-notification {
  background-color: rgba(16, 81, 71, 0.8) !important;
  backdrop-filter: blur(10px) !important;
  border: none !important;
  top: 35% !important; /* 垂直居中 */
  right: 50% !important; /* 水平居中 */
  transform: translate(50%, -50%) !important; /* 通过平移调整位置 */
}

.el-notification__title {
  color: white !important;
}

.el-notification__content {
  color: white !important;
}

#user-msg {
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1.5rem;
  margin-right: 2rem;
  color: #fff;
  font-weight: bolder;
  gap: 10px; /* 设置元素之间的间距 */
  cursor: pointer;
}

.link-btn-mail {
  width: 100%;
  padding: 1.2rem 1.4rem;
  border: 0.1rem solid #105147;
  border-left: none; /* 移除左边框 */
  font-size: 1.6rem;
  margin: 1rem 0;
}

.link-btn-mail:hover {
  background: #105147;
  color: #fff;
}

.disabled {
  background: #105147;
  color: #fff;
  cursor: not-allowed;
}

.nav > a:hover {
  color: #71918b;
}
</style>
