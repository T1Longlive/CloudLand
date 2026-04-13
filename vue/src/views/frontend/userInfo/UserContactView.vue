<template>
  <div class="password-main">
    <div class="input-msg-title">账号绑定</div>
    <div class="input-msg">
      <div class="input-password">
        <i class="el-icon-info"></i>
        <input placeholder="请输入新的邮箱或手机号" class="input-select-box" v-model="msg">
      </div>
      <div class="input-password">
        <i class="el-icon-lock"></i>
        <input type="password" placeholder="输入密码" class="input-select-box" v-model="user.password">
      </div>
      <button type="button" class="link-btn" @click="updateUser">确定</button>
    </div>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import router from "@/router";
import {createEmptyUser, fetchCurrentUser} from "@/utils/auth";

export default {
  name: "UserContactView",
  data() {
    return {
      user: createEmptyUser(),
      msg: null
    }
  },
  mounted() {
    if (sessionStorage.getItem("replace") === "1") {
      sessionStorage.setItem("replace", "0");
      location.reload();
    }
    this.openCheck();
  },
  methods: {
    async openCheck() {
      this.user = createEmptyUser()
      const {user} = await fetchCurrentUser(true)
      if (!user) {
        const currentRoute = router.currentRoute;
        if (currentRoute.path !== '/') {
          await router.push({path: '/'});
        }
        return
      }
      this.user = user;
    },
    async updateUser() {
      let condition = "phone"
      const regex = /^[1-9]\d{3,13}@qq\.com$/;
      if (regex.test(this.msg)) {
        condition = "mail"
        this.user.mail = this.msg;
      } else {
        this.user.phone = this.msg;
      }
      const formData = new FormData();

      if (this.checkFrom(this.msg, condition) === 'pass') {
        formData.append('user', JSON.stringify(this.user));
        const config = {
          headers: {
            frond: 'true',
            contact: 'true'
          }
        };
        const {data: res} = await axiosInstance.put('/user', formData, config);
        if (res.code === 10003) {
          this.$notify.info({
            title: '提示',
            message: '修改成功，请重新登录',
            type: 'success',
            duration: 1000
          });
          localStorage.clear();
          sessionStorage.clear();
          await router.push({path: '/'});
          return
        }
        this.$notify.info({
          title: '提示',
          message: res.msg,
          type: 'warning',
          duration: 1000
        });
      }
    },
    checkFrom(msg, condition) {
      if (condition === "phone") {
        if (msg === null) {
          this.$notify.info({
            title: '系统提示',
            message: '请填写手机号或邮箱',
            duration: 1000,
            showClose: false
          });
          return
        }
        const phoneRegex = /^1[3456789]\d{9}$/;
        if (!phoneRegex.test(msg)) {
          this.$notify.info({
            title: '系统提示',
            message: '手机号格式不正确',
            duration: 1000,
            showClose: false
          });
          return
        }
      }
      if (condition === "mail") {
        if (msg === null) {
          this.$notify.info({
            title: '系统提示',
            message: '请填写手机号或邮箱',
            duration: 1000,
            showClose: false
          });
          return
        }
        const mailRegex = /^[1-9]\d{3,13}@qq\.com$/;
        if (!mailRegex.test(msg)) {
          this.$notify.info({
            title: '系统提示',
            message: '邮箱格式不正确',
            duration: 1000,
            showClose: false
          });
          return
        }
      }
      if (this.user.password === null) {
        this.$notify.info({
          title: '系统提示',
          message: '请填写密码',
          duration: 1000,
          showClose: false
        });
        return
      }
      const passwordRegex = /\s/;
      if (this.user.password.length < 6 || passwordRegex.test(this.user.password)) {
        this.$notify.info({
          title: '系统提示',
          message: '密码格式不正确',
          duration: 1000,
          showClose: false
        });
        return
      }
      return "pass";
    },
  }
}
</script>

<style scoped>
.password-main {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  gap: 70px; /* 设置元素之间的间距 */
}

.input-msg-title {
  color: #6c757d;
  font-size: 2rem;
  border-bottom: 2px solid #bebebe;
  padding-bottom: 10px;
}

.input-msg {
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  gap: 20px; /* 设置元素之间的间距 */
}

.input-password {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40%;
  background-color: #cbb6b6;
}

.input-select-box {
  border: 2px solid #105147; /* 添加边框，可选 */
  padding: 20px; /* 为输入框添加内边距 */
  font-size: 1.5rem;
  width: 90%;
  height: 50px;
}

.el-icon-info {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  width: 80px;
  height: 50px;
  color: #d2d2d2;
  background-color: #105147;
}

.el-icon-lock {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  width: 80px;
  height: 50px;
  color: #d2d2d2;
  background-color: #105147;
}

.link-btn {
  width: 40%;
  margin-top: 1rem;
  padding: 1rem 3rem;
  display: inline-block;
  border: 0.1rem solid #105147;
  color: #105147;
  background: none;
  cursor: pointer;
  font-size: 1.7rem;
}

.link-btn:hover {
  background: #105147;
  color: #fff;
}
</style>
