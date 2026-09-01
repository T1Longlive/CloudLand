<template>
  <div class="user">
    <NavBar mode="page"></NavBar>
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

import NavBar from "@/components/NavBar";

export default {
  name: "UserView",
  components: {
    NavBar,
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

<style scoped>
.user {
  width: 100vw;
  height: 100vh;
  background-color: var(--color-bg-page);
  min-height: 700px;
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
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 20px;
  padding-left: 40px;
  height: 280px;
  background-color: #ffffff;
  flex: 0 0 21%;
  border-radius: 2px;
}

.user-msg {
  background-color: #ffffff;
  flex: 0 0 78%;
  border-radius: 2px;
  padding: 40px;
  box-shadow: var(--shadow-panel);
  min-height: 550px;
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
  flex-direction: column;
  gap: 10px;
  justify-content: center;
}

.select-item .item {
  color: #6c757d;
  font-size: 1.3rem;
  cursor: pointer;
}

.select-item .item:hover {
  color: var(--color-primary);
  font-weight: bolder;
}
</style>