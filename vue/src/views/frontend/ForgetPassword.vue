<template>
  <div class="user">
    <div class="landInfo-top">
      <div class="landInfo-top-msg">
        <router-link to="/" class="logo"><img src="../../assets/images/cloud.png" class="couldLogo" alt="">
          云用地
        </router-link>
      </div>
    </div>
    <div class="user-main">
      <div class="user-msg">
        <div class="password-main">
          <div class="input-msg-title">重置密码</div>
          <div class="input-msg">
            <div class="input-password">
              <i class="el-icon-lock"></i>
              <input type="password" placeholder="请输入新密码" class="input-select-box" v-model="password1">
            </div>
            <div class="input-password">
              <i class="el-icon-lock"></i>
              <input type="password" placeholder="请再次输入新密码" class="input-select-box" v-model="password2">
            </div>
            <button type="button" class="link-btn" @click="updateUser">确定</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<script>

import axiosInstance from "@/request/axiosInstance";
import router from "@/router";

export default {
  name: "ForgetPasswordView",
  data() {
    return {
      selectedOptions: [],
      user: {
        id: null,
        username: null,
        password: null,
        phone: null,
        age: null,
        address: null,
        img: null,
        status: 1,
        detailedAddress: null,
        power: 0,
        mail: null
      },
      password1: null,
      password2: null
    }
  },
  mounted() {
    this.openCheck();
  },
  methods: {
    async openCheck() {
      const str = this.$route.params.id;
      // 使用正则表达式检查字符串是否符合指定格式
      const regex = /^\d{6}-\d+$/; // 匹配6位数字-随机位数的数字
      if (!regex.test(str)) {
        router.push({path: '/user'}).then(r => null);
        return;
      }
      // 使用 split 方法将字符串分割成数组，分隔符为 "-"
      const parts = str.split("-");
      // 将分割后的两部分分别转换成数字
      const code = parseInt(parts[0], 10);
      this.user.id = parseInt(parts[1], 10)
      const formData = new FormData();
      formData.append('user', JSON.stringify(this.user));
      formData.append('path', code);
      const {data: res} = await axiosInstance.post('/user/forgetPassword', formData)
      if (res.code === 10003) {
        this.$message.success("验证成功！请修改你的密码")
      } else {
        router.push({path: '/user'}).then(r => null);
      }
    },
    async updateUser() {
      const formData = new FormData();
      if (this.checkFrom(this.password1) === 'pass' && this.checkFrom(this.password2) === 'pass') {
        if (this.password1 !== this.password2) {
          this.$notify.info({
            title: '提示',
            message: '两次密码不一致',
            type: 'warning',
            duration: 1000
          });
        } else {
          //暂时用详细地址字段接收新密码
          this.user.password = this.password1
          formData.append('user', JSON.stringify(this.user));
          formData.append('path', null);
          const config = {
            headers: {
              'frond': 'true', // 添加自定义请求头
              'forgetPassword': 'true'//用于取消后端验证旧密码
            }
          };
          const {data: res} = await axiosInstance.post('/user/forgetPassword', formData, config);
          if (res.code === 10003) {
            this.$notify.info({
              title: '提示',
              message: '修改成功，请在首页登录！',
              type: 'success',
              duration: 2000
            });
            localStorage.clear();
            sessionStorage.clear();
            setTimeout(async () => {
              await router.push({path: '/'});
            }, 2000);
          } else {
            this.$notify.info({
              title: '提示',
              message: res.msg,
              type: 'warning',
              duration: 1000
            });
          }
        }
      }
    },
    checkFrom(password) {
      //密码验证
      if (password === null) {
        this.$notify.info({
          title: '系统提示',
          message: '请填写密码!',
          duration: 1000,
          showClose: false
        });
        return
      } else {
        const regex = /\s/;
        if (password.length < 6 || regex.test(password)) {
          this.$notify.info({
            title: '系统提示',
            message: '密码有误!',
            duration: 1000,
            showClose: false
          });
          return
        }
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
  gap: 70px;
}

.input-msg-title {
  color: #6c757d;
  font-size: 2rem;
  border-bottom: 2px solid var(--color-border);
  padding-bottom: 10px;
}

.input-msg {
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.input-password {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40%;
  background-color: #cbb6b6;
}

.input-select-box {
  border: 2px solid var(--color-primary);
  padding: 20px;
  font-size: 1.5rem;
  width: 90%;
  height: 50px;
}

.el-icon-lock {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  width: 80px;
  height: 50px;
  color: #d2d2d2;
  background-color: var(--color-primary);
}

.link-btn {
  width: 40%;
  margin-top: 1rem;
  padding: 1rem 3rem;
  display: inline-block;
  border: 0.1rem solid var(--color-primary);
  color: var(--color-primary);
  background: none;
  cursor: pointer;
  font-size: 1.7rem;
}

.link-btn:hover {
  background: var(--color-primary);
  color: #fff;
}

.landInfo-top {
  width: 100%;
  height: 13vh;
  background-color: var(--color-primary);
}

.landInfo-top-msg {
  width: 80%;
  height: 100%;
  background-color: var(--color-primary);
  margin: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 900px;
}

.logo {
  display: inline-block;
  font-size: 2.5rem;
  color: #fff;
  font-weight: bolder;
  padding-left: 1vw;
}

.user {
  width: 100vw;
  height: 100vh;
  background-color: var(--color-bg-page);
  min-height: 700px;
}

.user-main {
  width: 60%;
  height: 87vh;
  margin: auto;
  min-width: 800px;
  padding: 20px;
}

.user-msg {
  background-color: #ffffff;
  flex: 0 0 78%;
  border-radius: 2px;
  padding: 40px;
  box-shadow: var(--shadow-panel);
  min-height: 550px;
}
</style>
