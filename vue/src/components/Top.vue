<template>
  <div>
    <!--导航栏-->
    <header class="header fixed-top">
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
            <div id="login-btn" class="fas fa-user" @click="isLoginVisible=true"
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
    <!--导航栏结束-->
    <div class="login-form" v-show="isLoginVisible">
      <form action="">
        <div id="close-login-form" class="fas fa-times" @click="isLoginVisible=false"></div>
        <a href="#" class="logo mr-auto"> <img src="../assets/images/cloud.png" class="couldLogo" alt=""> 云用地 </a>
        <h3>让世界更有价值</h3>
        <input placeholder="请输入你的手机号" id="1" class="box" minlength="11" maxlength="11" v-model="user.phone">
        <input type="password" placeholder="请输入你的密码" id="12" class="box" minlength="6" maxlength="20"
               v-model="user.password">
        <div class="abc"></div>
        <input placeholder="请输入你的验证码" id="9" minlength="6" maxlength="6" class="box" v-model="code" style="width: 60%">
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
          <input type="checkbox" name="" id="remember-me">
          <label for="remember-me">记住我</label>
          <div class="mail" @click="forgetPassword()" :class="{ 'disabled2': isCounting2 }">   {{ buttonLabel2 }} </div>
        </div>
        <button type="button" class="link-btn" @click="LoginUp">登录</button>
        <p class="account">没有账号? <a href="#" @click="isLoginVisible=false;isRegisterVisible=true;">注册一个!</a></p>
      </form>
    </div>
    <!--    注册表单  :visible.sync="isRegisterVisible"-->
    <div class="register-form" v-show="isRegisterVisible">
      <div id="close-register-form" class="fas fa-times" @click="isRegisterVisible=false"></div>
      <form action="" ref="form">
        <a href="#" class="logo mr-auto"> <img src="../assets/images/cloud.png" class="couldLogo" alt=""> 云用地 </a>
        <h3>让世界更有价值</h3>
        <input placeholder="请输入用户名(2-10个字符)" id="4" class="box" minlength="2" maxlength="10" v-model="user.username">
        <input placeholder="请输入手机号" id="5" class="box" minlength="11" maxlength="11" v-model="user.phone">
        <input type="password" placeholder="请设置你的密码(6-20位)" class="box" minlength="6" maxlength="20"
               v-model="user.password">
        <input placeholder="请输入你的年龄" class="box" minlength="2" maxlength="3" v-model="user.age">
        <input placeholder="请输入QQ邮箱(@qq.com结尾)" class="box" minlength="6" maxlength="20" v-model="user.mail">
        <el-cascader
            placeholder="请选择地址,可搜索"
            :options="options"
            v-model="selectedOptions"
            filterable class="box"></el-cascader>
        <input placeholder="请输入你的详细地址" class="box" v-model="user.detailedAddress">
        <p class="account">！注册即表示同意我们的协议- <a href="#" @click="isLoginVisible=false;isRegisterVisible=true;">《云用地协议》</a>
        </p>
        <input placeholder="请输入你的验证码" id="6" class="box" minlength="6" maxlength="6" v-model="code" style="width: 57%">
        <button type="button"
                class="link-btn-mail"
                style="width: 43%"
                @click="sendCode()"
                :class="{ 'disabled': isCounting }"
                :disabled="isCounting"
        >
          {{ buttonLabel }}
        </button>
        <div class="abc">
          <button type="button" class="link-btn" style="width: 40%"
                  @click="isLoginVisible=true;isRegisterVisible=false;">返回登录
          </button>
          <button type="button" class="link-btn" @click="register" style="width: 40%">确认注册</button>
        </div>
      </form>
    </div>
  </div>
</template>
<script>
import {myFunction} from '@/assets/js/script';
import axiosInstance from "@/request/axiosInstance";
import router from "@/router";
import {regionData} from "element-china-area-data";
import {APP_CONFIG} from "@/config/app";
import {createEmptyUser, fetchCurrentUser, loginWithPassword} from "@/utils/auth";

const PHONE_REGEX = /^1[3-9]\d{9}$/;
const USERNAME_REGEX = /^\S{2,10}$/;
const AGE_REGEX = /^(1[89]|[2-9]\d|1[01]\d|120)$/;
const QQ_MAIL_REGEX = /^[1-9]\d{3,13}@qq\.com$/;
const DETAIL_ADDRESS_REGEX = /^\S{5,}$/;
const CODE_REGEX = /^\S{6}$/;
const HAS_SPACE_REGEX = /\s/;

export default {
  name: "Top",
  data() {
    return {
      code: null,
      isCounting: false,
      isCounting2: false,
      countdownInterval: null,
      countdownInterval2: null,
      countdown: 59,
      countdown2: 59,
      options: regionData,
      selectedOptions: [],
      imgFileList: [],
      remember: false,
      isLoginVisible: false,
      isRegisterVisible: false,
      filePath: APP_CONFIG.resourceUrls.userFile,
      Path: APP_CONFIG.forgetPasswordPath,
      user: createEmptyUser()
    };
  },
  computed: {
    buttonLabel() {
      return this.isCounting ? `${this.countdown} 秒后重试` : '发送验证码';
    },
    buttonLabel2() {
      return this.isCounting2 ? `${this.countdown2} 秒后重试` : '忘记密码';
    }
  },
  watch: {
    isLoginVisible(newVisibility) {
      if (!newVisibility) {
        this.onLoginFormClose();
      }
    },
    isRegisterVisible(newVisibility) {
      if (!newVisibility) {
        this.onLoginFormClose();
      }
    },
  },
  mounted() {
    myFunction();
    this.openCheck();
  },
  beforeDestroy() {
    this.resetCodeCountdown();
    this.resetForgetPasswordCountdown();
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
    resetForgetPasswordCountdown() {
      this.isCounting2 = false;
      this.countdown2 = 59;
      clearInterval(this.countdownInterval2);
      this.countdownInterval2 = null;
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
    startForgetPasswordCountdown() {
      this.resetForgetPasswordCountdown();
      this.isCounting2 = true;
      this.countdownInterval2 = setInterval(() => {
        if (this.countdown2 > 0) {
          this.countdown2 -= 1;
          return;
        }
        this.resetForgetPasswordCountdown();
      }, 1000);
    },
    validatePhone() {
      if (!this.user.phone) {
        this.notifyInfo('请填写手机号!');
        return false;
      }
      if (!PHONE_REGEX.test(this.user.phone)) {
        this.notifyInfo('手机号格式有误!');
        return false;
      }
      return true;
    },
    validatePassword() {
      if (!this.user.password) {
        this.notifyInfo('请填写密码!');
        return false;
      }
      if (this.user.password.length < 6 || HAS_SPACE_REGEX.test(this.user.password)) {
        this.notifyInfo('密码有误!');
        return false;
      }
      return true;
    },
    validateRegisterFields() {
      if (!this.user.username) {
        this.notifyInfo('请填写用户名!');
        return false;
      }
      if (!USERNAME_REGEX.test(this.user.username)) {
        this.notifyInfo('用户名填写有误!');
        return false;
      }
      if (!this.user.age) {
        this.notifyInfo('请填写年龄');
        return false;
      }
      if (!AGE_REGEX.test(String(this.user.age))) {
        this.notifyInfo('年龄填写有误!');
        return false;
      }
      if (!this.user.mail) {
        this.notifyInfo('请填写邮箱');
        return false;
      }
      if (!QQ_MAIL_REGEX.test(this.user.mail)) {
        this.notifyInfo('邮箱填写有误!');
        return false;
      }
      if (this.selectedOptions.length === 0) {
        this.notifyInfo('请选择地址!');
        return false;
      }
      if (!this.user.detailedAddress) {
        this.notifyInfo('请填写详细地址');
        return false;
      }
      if (!DETAIL_ADDRESS_REGEX.test(this.user.detailedAddress)) {
        this.notifyInfo('详细地址最低填写五个字!');
        return false;
      }
      return true;
    },
    validateCode() {
      if (!this.code) {
        this.notifyInfo('请先获取验证码!');
        return false;
      }
      if (!CODE_REGEX.test(this.code)) {
        this.notifyInfo('验证码有误!');
        return false;
      }
      return true;
    },
    async forgetPassword() {
      if (!this.validatePhone()) {
        return;
      }
      if (this.isCounting || this.isCounting2) {
        alert("请稍后再试!");
        return;
      }
      this.startForgetPasswordCountdown();
      const formData = new FormData();
      formData.append('user', JSON.stringify(this.user));
      formData.append('path', this.Path);
      alert("我们将发送一封邮箱验证链接注意查收!请按照上面指示重置密码。");
      const {data: res} = await axiosInstance.post('/user/forgetPassword', formData);
      if (res.code === 30001) {
        alert("发送成功！重置链接5分钟内有效（注：该链接只生效一次！）");
        return;
      }
      this.resetForgetPasswordCountdown();
      alert("发送失败！请检查你的邮箱是否有效");
    },
    checkFrom(condition, code) {
      if (!this.validatePhone() || !this.validatePassword()) {
        return;
      }
      if (condition && !this.validateRegisterFields()) {
        return;
      }
      if (code && !this.validateCode()) {
        return;
      }
      return "pass";
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
      this.isLoginVisible = false;
    },
    async LoginUp() {
      if (this.checkFrom(false, true) !== "pass") {
        return;
      }
      const checkbox = document.getElementById("remember-me");
      this.remember = Boolean(checkbox && checkbox.checked);
      const {user, res: loginRes} = await loginWithPassword(this.user, {
        frond: true,
        remember: this.remember,
        code: this.code
      });
      if (!user) {
        this.notifyInfo(`${loginRes.msg}!`, '登录提示');
        this.code = null;
        return;
      }
      this.resetCodeCountdown();
      this.user = user;
      this.isLoginVisible = false;
      this.code = null;
      this.notifySuccess(`欢迎你，${this.user.username}`);
      const currentRoute = router.currentRoute;
      if (currentRoute && currentRoute.path !== '/') {
        await router.push({path: '/'});
      }
    },
    async register() {
      if (this.checkFrom(true, true) !== "pass") {
        return;
      }
      const formData = new FormData();
      this.user.address = this.selectedOptions.join(",");
      formData.append('user', JSON.stringify(this.user));
      const config = {
        headers: {
          frond: 'true',
          code: this.code,
        }
      };
      const {data: res} = await axiosInstance.post('/user/register', formData, config);
      if (res.code === 20006) {
        this.notifyInfo(`${res.msg}!`);
        this.resetCodeCountdown();
        this.isRegisterVisible = false;
        this.code = null;
        this.isLoginVisible = true;
        return;
      }
      this.notifyInfo(`${res.msg}!`);
      this.code = null;
    },
    ImgHandChange(file, fileList) {
      this.imgFileList = fileList;
    },
    ImgHandleRemove(file, fileList) {
      this.imgFileList = fileList;
    },
    onLoginFormClose() {
      this.code = null;
      this.selectedOptions = [];
    },
    async sendCode() {
      this.code = null;
      if (this.checkFrom(this.isRegisterVisible, false) !== "pass") {
        return;
      }
      if (this.isCounting || this.isCounting2) {
        alert("请稍后再试!");
        return;
      }
      this.startCodeCountdown();
      const {data: res} = await axiosInstance.post('/user/code', this.user);
      console.log("状态码为",res.code);
      if (res.code === 30001) {
        alert("发送成功！验证码5分钟内有效");
        return;
      }
      this.resetCodeCountdown();
      alert("发送失败！请检查你的邮箱是否有效");
    },
    openDialog() {
      router.push({path: '/user'}).then(() => null);
    }
  }
}
</script>

<style lang="less">
.el-icon-user-solid {
  //font-weight: bolder;
  font-size: 2rem;
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
.disabled2 {
  color: #a1a1a1;
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

.abc {
  justify-content: space-between;
  display: flex;
}

.register-form {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1100;
  height: 100%;
  width: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}


.register-form form {
  position: relative; /* 相对定位 */
  width: 50rem;
  padding: 4rem;
  background: #fff;
  text-align: center;
  animation: fadeIn .2s linear;
  max-height: 80vh; /* 设置表单最大高度为屏幕视窗高度的80% */
  overflow-y: auto; /* 启用垂直滚动条 */
}

#close-register-form {
  position: absolute; /* 绝对定位 */
  top: 20px;
  right: 20px;
  font-size: 26px;
  cursor: pointer;
  color: #fff;
}

#close-register-form:hover {
  transform: rotate(90deg);
}


.register-form form .logo {
  font-size: 2.5rem;
  color: #105147;
  font-weight: bolder;
}

.register-form form h3 {
  padding: 1rem 0;
  font-size: 2rem;
  text-transform: capitalize;
  color: #222;
  margin-top: 1rem;
}

.register-form form .box {
  width: 100%;
  padding: 1.2rem 1.4rem;
  border: 0.1rem solid #105147;
  font-size: 1.6rem;
  margin: 1rem 0;
}

.register-form form .flex {
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  -webkit-box-align: center;
  -ms-flex-align: center;
  align-items: center;
  gap: .5rem;
  margin: 1rem 0;
}

.register-form form .flex label {
  font-size: 1.5rem;
  line-height: 2;
  color: #666;
  margin-bottom: 0;
  cursor: pointer;
}

.register-form form .flex a {
  font-size: 1.5rem;
  color: #105147;
  margin-left: auto;
}

.register-form form .flex a:hover {
  text-decoration: underline !important;
}

.register-form form .link-btn {
  width: 100%;
  margin-bottom: 2rem;
}

.register-form form .account {
  padding: 1.5rem .5rem;
  background: #eee;
  font-size: 1.5rem;
  line-height: 2;
  color: #666;
  margin-bottom: 0;
}

.register-form form .account a {
  color: #105147;
}

.register-form form .account a:hover {
  text-decoration: underline !important;
}

#close-register-form {
  font-size: 24px;
  cursor: pointer;
  transition: transform 0.5s;
}

#close-register-form:hover {
  animation: rotate 0.5s linear;
}

#close-login-form {
  font-size: 24px;
  cursor: pointer;
  transition: transform 0.5s;
}

#close-login-form:hover {
  animation: rotate 0.5s linear;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(90deg);
  }
}

.el-input__inner {
  border: none !important;
  font-size: 1.3rem !important;
  font-weight: bolder !important;
}

.el-cascader-node.in-active-path, .el-cascader-node.is-active, .el-cascader-node.is-selectable.in-checked-path {
  color: #0c5460 !important;
}

</style>
