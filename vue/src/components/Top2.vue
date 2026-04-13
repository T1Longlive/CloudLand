<template>
  <div class="landInfo-top">
    <div class="landInfo-top-msg">
      <router-link to="/" class="logo"><img src="../assets/images/cloud.png" class="couldLogo" alt="">
        云用地
      </router-link>
      <div class="landInfo-top-msg-right">
        <router-link to="/">首页</router-link>
        <router-link to="/user">
          <div class="user-user-msg" v-show="user.username!==null">
            <img
                style="width: 40px; height: 40px;background-color: white;border-radius: 5px;"
                v-if="user.img!==null"
                :src="filePath2+user.img"
                id="uImg">

          </div>
          <div class="user-user-msg" v-show="user.username===null">未登录</div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
import {CodeToText} from "element-china-area-data";
import {APP_CONFIG} from "@/config/app";
import {createEmptyUser, fetchCurrentUser} from "@/utils/auth";

export default {
  name: "Top2",
  data() {
    return {
      filePath2: APP_CONFIG.resourceUrls.userFile,
      user: createEmptyUser(),
    }

  },
  async mounted() {
    if (sessionStorage.getItem("replace") === "1") {
      sessionStorage.setItem("replace", "0");
      location.reload();
    }
    await this.openCheck();
  },
  methods: {
    async openCheck() {
      this.user = createEmptyUser()
      const {user} = await fetchCurrentUser(true)
      if (!user) {
        return
      }
      const addressString = user.address;
      if (addressString) {
        const addressArray = addressString.split(",");
        user.address = CodeToText[addressArray[0]] + "-" + CodeToText[addressArray[1]] + "-" + CodeToText[addressArray[2]]
      }
      this.user = user
      sessionStorage.setItem('userID', this.user.id)
    },
  }
}
</script>

<style scoped>
.user-user-msg {
  font-size: 1.5rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
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
  display: inline-block; /* 可选：使链接元素变成块级元素，便于设置宽度等属性 */
  font-size: 2.5rem;
  color: #fff;
  font-weight: bolder;
  padding-left: 1vw;
}

.landInfo-top-msg-right {
  font-weight: bold;
  font-size: 1.5rem;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.landInfo-top-msg-right > a {
  margin-right: 30px;
  color: #fff;
}

.landInfo-top-msg-right > a:hover {
  color: #71918b;
}

#uImg:hover {
  border: none;
  box-shadow: 2px 2px 5px rgba(23, 20, 20, 0.8);
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

</style>
