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
        alert("验证成功！请修改你的密码")
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

<style>
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

.landInfo-top {
  width: 100%;
  height: 13vh;
  background-color: #105147;
}

.landInfo-top-msg {
  width: 80%;
  height: 100%;
  background-color: #105147;
  margin: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 900px;
}

.landInfo-top-msg > a:hover {
  color: #71918b;
}

.logo {
  display: inline-block; /* 可选：使链接元素变成块级元素，以便设置宽度等属性 */
  font-size: 2.5rem;
  color: #fff;
  font-weight: bolder;
  padding-left: 1vw;
}


.landInfo-top-msg-right > a {
  margin-right: 30px;
  color: #fff;
}

.landInfo-top-msg-right > a:hover {
  color: #71918b;
}

.el-carousel__item h3 {
  color: #475669;
  font-size: 14px;
  opacity: 0.75;
  line-height: 150px;
  margin: 0;
}

.land-Card-1 > h2 {
  font-size: 1.5rem;
  font-weight: bolder;
  color: #2cb7c0;
}

.landInfo-msg-4 > p {
  color: #fd0000;
}

.landInfo-msg-2-2 > p {
  width: 70px;
  background-color: #721c24;
}

.user {
  width: 100vw;
  height: 100vh;
  background-color: #e5e5e5;
  min-height: 700px;
}

.user-top-msg > a:hover {
  color: #71918b;
}

.user-top-msg-right > a {
  margin-right: 30px;
  color: #fff;
}

.user-top-msg-right > a:hover {
  color: #71918b;
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
  flex: 0 0 78%; /* 或者使用 calc(50% - 5%) 来实现 10% 留白 */
  border-radius: 2px;
  padding: 40px;
  box-shadow: 5px 5px 10px rgba(0, 0, 0, 0.5);
  min-height: 550px;
}

.el-button--primary {
  color: #FFF;
  background-color: #105147;
  border-color: #105147;
}

.el-button--primary:focus, .el-button--primary:hover {
  background: #105147;
  border-color: #105147;
  color: #FFF;
}

.el-button:hover {
  background: #105147;
  border-color: #105147;
  color: #FFF;
}

.el-message-box__headerbtn .el-message-box__close:hover {
  color: #000000;
}
</style>
