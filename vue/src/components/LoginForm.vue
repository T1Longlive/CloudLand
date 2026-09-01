<template>
  <div>
    <!--登录表单-->
    <div class="login-form" v-show="loginVisible">
      <form action="">
        <div id="close-login-form" class="fas fa-times" v-if="frond" @click="loginVisible=false"></div>
        <a href="#" class="logo mr-auto">
          <img src="../assets/images/cloud.png" class="couldLogo" alt=""> {{ systemName }}
        </a>
        <h3>让世界更有价值</h3>
        <input placeholder="请输入你的手机号" class="box" minlength="11" maxlength="11" v-model="user.phone">
        <input type="password" placeholder="请输入你的密码" class="box" minlength="6" maxlength="20"
               v-model="user.password">
        <div class="abc"></div>
        <input placeholder="请输入你的验证码" minlength="6" maxlength="6" class="box" v-model="code" style="width: 60%">
        <button type="button"
                class="link-btn-mail"
                style="width: 40%"
                @click="sendCode()"
                :class="{ 'disabled': isCounting }"
                :disabled="isCounting"
        >
          {{ buttonLabel }}
        </button>
        <div class="flex" v-if="frond">
          <input type="checkbox" id="remember-me" v-model="remember">
          <label for="remember-me">记住我</label>
          <div class="mail" @click="forgetPassword()" :class="{ 'disabled2': isCounting2 }">{{ buttonLabel2 }}</div>
        </div>
        <button type="button" class="link-btn" @click="LoginUp">登录</button>
        <p class="account" v-if="frond">
          没有账号?
          <a href="#" @click="switchToRegister">注册一个!</a>
        </p>
        <p class="account" v-else>云用地后台系统需要二级账号，若没有可联系我们申请</p>
      </form>
    </div>
    <!--注册表单（仅前台）-->
    <div class="register-form" v-show="frond && registerVisible">
      <div id="close-register-form" class="fas fa-times" @click="registerVisible=false"></div>
      <form action="">
        <a href="#" class="logo mr-auto">
          <img src="../assets/images/cloud.png" class="couldLogo" alt=""> {{ systemName }}
        </a>
        <h3>让世界更有价值</h3>
        <input placeholder="请输入用户名(2-10个字符)" class="box" minlength="2" maxlength="10" v-model="user.username">
        <input placeholder="请输入手机号" class="box" minlength="11" maxlength="11" v-model="user.phone">
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
        <p class="account">！注册即表示同意我们的协议-
          <a href="#" @click="switchToRegister">《云用地协议》</a>
        </p>
        <input placeholder="请输入你的验证码" minlength="6" maxlength="6" class="box" v-model="code" style="width: 57%">
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
                  @click="loginVisible=true;registerVisible=false;">返回登录
          </button>
          <button type="button" class="link-btn" @click="register" style="width: 40%">确认注册</button>
        </div>
      </form>
    </div>
  </div>
</template>
<script>
import axiosInstance from "@/request/axiosInstance";
import {APP_CONFIG} from "@/config/app";
import {createEmptyUser, loginWithPassword} from "@/utils/auth";

const PHONE_REGEX = /^1[3-9]\d{9}$/;
const USERNAME_REGEX = /^\S{2,10}$/;
const AGE_REGEX = /^(1[89]|[2-9]\d|1[01]\d|120)$/;
const QQ_MAIL_REGEX = /^[1-9]\d{3,13}@qq\.com$/;
const DETAIL_ADDRESS_REGEX = /^\S{5,}$/;
const CODE_REGEX = /^\S{6}$/;
const HAS_SPACE_REGEX = /\s/;

/**
 * 统一登录/注册表单（原 Top.vue 与后台 MainView.vue 三份复制代码合并）
 * - frond=true：前台模式（登录 + 注册 + 忘记密码 + 记住我）
 * - frond=false：后台模式（仅登录，二级账号提示，不可关闭）
 * 登录成功后 emit('login-success', user)，由父组件处理各自的后续逻辑
 */
export default {
  name: "LoginForm",
  props: {
    frond: {
      type: Boolean,
      default: true
    },
    systemName: {
      type: String,
      default: '云用地'
    }
  },
  data() {
    return {
      code: null,
      isCounting: false,
      isCounting2: false,
      countdownInterval: null,
      countdownInterval2: null,
      countdown: 59,
      countdown2: 59,
      options: [],
      selectedOptions: [],
      remember: false,
      loginVisible: false,
      registerVisible: false,
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
    loginVisible(newVisibility) {
      if (!newVisibility) {
        this.onFormHide();
      }
    },
    registerVisible(newVisibility) {
      if (!newVisibility) {
        this.onFormHide();
      }
    },
  },
  beforeDestroy() {
    this.resetCodeCountdown();
    this.resetForgetPasswordCountdown();
  },
  methods: {
    open() {
      this.user = createEmptyUser();
      this.loginVisible = true;
    },
    // 切换到注册表单，并按需懒加载省市区数据（约 100KB，避免拖慢首屏）
    async switchToRegister() {
      this.loginVisible = false;
      this.registerVisible = true;
      if (this.options.length === 0) {
        const {regionData} = await import('element-china-area-data');
        this.options = regionData;
      }
    },
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
        this.$message.warning("请稍后再试!");
        return;
      }
      this.startForgetPasswordCountdown();
      const formData = new FormData();
      formData.append('user', JSON.stringify(this.user));
      formData.append('path', this.Path);
      this.$message.info("我们将发送一封邮箱验证链接注意查收!请按照上面指示重置密码。");
      const {data: res} = await axiosInstance.post('/user/forgetPassword', formData);
      if (res.code === 30001) {
        this.$message.success("发送成功！重置链接5分钟内有效（注：该链接只生效一次！）");
        return;
      }
      this.resetForgetPasswordCountdown();
      this.$message.error("发送失败！请检查你的邮箱是否有效");
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
    async LoginUp() {
      if (this.checkFrom(false, true) !== "pass") {
        return;
      }
      const {user, res: loginRes} = await loginWithPassword(this.user, {
        frond: this.frond,
        remember: this.frond && this.remember,
        code: this.code
      });
      if (!user) {
        this.notifyInfo(`${loginRes.msg}!`, '登录提示');
        this.code = null;
        return;
      }
      this.resetCodeCountdown();
      this.loginVisible = false;
      this.code = null;
      this.notifySuccess(`欢迎你，${user.username}`);
      this.$emit('login-success', user);
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
        this.registerVisible = false;
        this.code = null;
        this.loginVisible = true;
        return;
      }
      this.notifyInfo(`${res.msg}!`);
      this.code = null;
    },
    onFormHide() {
      this.code = null;
      this.selectedOptions = [];
    },
    async sendCode() {
      this.code = null;
      if (this.checkFrom(this.frond && this.registerVisible, false) !== "pass") {
        return;
      }
      if (this.isCounting || this.isCounting2) {
        this.$message.warning("请稍后再试!");
        return;
      }
      this.startCodeCountdown();
      const {data: res} = await axiosInstance.post('/user/code', this.user);
      if (res.code === 30001) {
        this.$message.success("发送成功！验证码5分钟内有效");
        return;
      }
      this.resetCodeCountdown();
      this.$message.error("发送失败！请检查你的邮箱是否有效");
    }
  }
}
</script>

<style lang="less" scoped>
#remember-me {
  accent-color: var(--color-primary);
}

.link-btn-mail {
  width: 100%;
  padding: 1.2rem 1.4rem;
  border: 0.1rem solid var(--color-primary);
  border-left: none; /* 移除左边框，与左侧验证码输入框拼合 */
  font-size: 1.6rem;
  margin: 1rem 0;
}

.link-btn-mail:hover {
  background: var(--color-primary);
  color: #fff;
}

.disabled {
  background: var(--color-primary);
  color: #fff;
  cursor: not-allowed;
}

.disabled2 {
  color: #a1a1a1;
}

.mail {
  font-size: 1.5rem;
  color: var(--color-primary);
  margin-left: auto;
  cursor: pointer;
}

.mail:hover {
  color: var(--color-danger);
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
  position: relative;
  width: 50rem;
  padding: 4rem;
  background: #fff;
  text-align: center;
  animation: fadeIn .2s linear;
  max-height: 80vh;
  overflow-y: auto;
}

#close-register-form {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 26px;
  cursor: pointer;
  color: #fff;
  transition: transform 0.5s;
}

#close-register-form:hover {
  transform: rotate(90deg);
}

#close-login-form {
  font-size: 24px;
  cursor: pointer;
  transition: transform 0.5s;
}

#close-login-form:hover {
  transform: rotate(90deg);
}

.register-form form .logo {
  font-size: 2.5rem;
  color: var(--color-primary);
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
  border: 0.1rem solid var(--color-primary);
  font-size: 1.6rem;
  margin: 1rem 0;
}

.register-form form .flex {
  display: flex;
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
  color: var(--color-primary);
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
  color: var(--color-primary);
}

.register-form form .account a:hover {
  text-decoration: underline !important;
}

/* 注册表单内的级联选择器：边框由外层 .box 提供，内层输入框去边框 */
.register-form ::v-deep .el-input__inner {
  border: none !important;
}
</style>
