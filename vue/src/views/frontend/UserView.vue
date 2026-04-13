<template>
  <div class="user">
    <TopBer></TopBer>
    <div class="user-main">
      <div class="user-select">
        <router-link to="/user" class="page-name">个人中心</router-link>
        <div class="select-item">
          <router-link to="/user" class="item" exact active-class="active-link">个人信息</router-link>
          <router-link to="/user/myTrolley" class="item" exact active-class="active-link">我的购物车</router-link>
          <router-link to="/user/myOrder" class="item" exact active-class="active-link">我的订单</router-link>
          <router-link to="/user/password" class="item" exact active-class="active-link">密码修改</router-link>
          <router-link to="/user/contact" class= "item" exact active-class="active-link">账号绑定</router-link>
          <div class="item" @click="exitLogin">退出登录</div>
        </div>
      </div>
      <div class="user-msg">
        <keep-alive>
          <router-view/>
        </keep-alive>
      </div>
    </div>
  </div>
</template>


<script>

import TopBer from "@/components/Top2";

export default {
  name: "UserView",
  components: {
    TopBer,
  },
  data(){
    return{
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
      }
    }
  },
  mounted() {
    if (sessionStorage.getItem("replace") === "1") {
      sessionStorage.setItem("replace", "0");
      location.reload();
    }
  },
  methods:{
    exitLogin(){
      this.$confirm("是否退出登录?", '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        localStorage.setItem("token",null)
        sessionStorage.setItem("token",null)
        location.reload()
      })
    }
  }
}
</script>

<style>

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
  display: flex;
  width: 80%;
  height: 87vh;
  margin: auto;
  min-width: 800px;
  justify-content: space-between;
  padding: 20px;
}

.user-select {
  background-color: #ffffff;
  flex: 0 0 21%; /* 或者使用 calc(50% - 5%) 来实现 10% 留白 */
  border-radius: 2px;
}

.user-msg {
  background-color: #ffffff;
  flex: 0 0 78%; /* 或者使用 calc(50% - 5%) 来实现 10% 留白 */
  border-radius: 2px;
  padding: 40px;
  box-shadow: 5px 5px 10px rgba(0, 0, 0, 0.5);
  min-height: 550px;
}

.user-select {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 20px;
  padding-left: 40px;
  height: 280px;
  /*box-shadow: 5px 5px 10px rgba(0, 0, 0, 0.5);*/
}

.page-name {
  font-size: 2rem;
  font-weight: bolder;
  color: #7a858f;
  cursor: pointer;
}

.select-item {
  display: flex;
  height: 180px;
  flex-direction: column; /* 竖着排列 */
  gap: 10px; /* 设置元素之间的间距 */
  justify-content: center;
}

.select-item .item:hover {
  color: #105147;
  font-weight: bolder;
}
.select-item .item{
  color: #6c757d;
  font-size: 1.3rem;
  cursor: pointer;
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
.el-button:hover{
  background: #105147;
  border-color: #105147;
  color: #FFF;
}
.el-message-box__headerbtn .el-message-box__close:hover{
  color: #000000;
}
</style>