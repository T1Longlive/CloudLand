<template>
  <div>
    <TopBer></TopBer>
    <!-- home section starts  -->
    <section class="home" id="home">
      <div class="container">
        <a href="/#home">
          <div class="bigModel">回到顶部</div>
        </a>

        <div class="row align-items-center text-center text-md-left min-vh-100">
          <div class="col-md-6">
            <span>Enhance the value of the world</span>
            <h3>云用地，让每片土地都有归属</h3>
            <a href="#menu" class="link-btn">开始使用</a>
          </div>
        </div>
      </div>
    </section>

    <!-- home section ends -->
    <!-- about section starts  -->
    <section class="about" id="about">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-md-6">
            <img src="../../assets/images/0.jpeg" class="w-100" alt="">
          </div>
          <div class="col-md-6">
            <span>为什么选择我们?</span>
            <h3 class="title">安全的、便捷的、人性化的用地管理</h3>
            <p>我们有众多分类的用地、丰富的用地使用套餐、灵活的用地租赁、还有绿色直售的农产品。此外我们还有当地有可靠农民加盟和平台租地代理人，让你足不出户就能使用土地。</p>
            <a href="#menu" class="link-btn">了解更多</a>
            <div class="icons-container">
              <div class="icons">
                <i class="fas fa-coffee"></i>
                <h3>线上用地</h3>
              </div>
              <div class="icons">
                <i class="fas fa-shipping-fast"></i>
                <h3>平台包邮</h3>
              </div>
              <div class="icons">
                <i class="fas fa-headset"></i>
                <h3>24小时客服</h3>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
    <!-- about section ends -->
    <!-- menu section starts  -->
    <section class="menu" id="menu">
      <h1 class="heading"> 云用地可以干什么？ </h1>
      <div class="container box-container">
        <div class="box">
          <img src="../../assets/images/出租用地.png" alt="">
          <h3>出租用地</h3>
          <p>通过平台发布出租闲置的用地信息，让用地不再荒废</p>
          <router-link to="/backend/land" class="link-btn">开始使用</router-link>
        </div>
        <div class="box">
          <img src="../../assets/images/买卖绿色直售农产品.png" alt="">
          <h3>买卖绿色直售农产品</h3>
          <p>平台的农产品是当地加盟的农民和平台人员自己种出来的安全可靠，现买现摘</p>
          <router-link to="/product" class="link-btn">开始使用</router-link>
        </div>
        <div class="box">
          <img src="../../assets/images/加盟我们管理用地.png" alt="">
          <h3>加盟我们管理用地</h3>
          <p>根据用地位置选择当地人加盟帮助云客户和平台管理土地</p>
          <router-link to="/backend/land" class="link-btn">开始使用</router-link>
        </div>
        <div class="box">
          <img src="../../assets/images/云租地用地.png" alt="">
          <h3>云租地用地</h3>
          <p>选择自己的用地和套餐，平台帮你匹配当地人进行使用和管理用地。实现足不出户就能使用土地。</p>
          <router-link to="/land" class="link-btn">开始使用</router-link>
        </div>
        <div class="box">
          <img src="../../assets/images/普通租地用地.png" alt="">
          <h3>普通租地用地</h3>
          <p>选择要租用的土地进行线下联系考查直接和出租的用户进行对接，自己线下管理使用土地</p>
          <router-link to="/land" class="link-btn">开始使用</router-link>
        </div>
        <div class="box">
          <img src="../../assets/images/加盟平台用地代理.png" alt="">
          <h3>加盟平台用地代理</h3>
          <p>成为平台地区级的用地代理联系人，帮助云客户和代理管理用地的当地人进行交接沟通</p>
          <a href="#contact" class="link-btn">开始使用</a>
        </div>
      </div>
    </section>

    <section class="contact" id="contact">
      <h1 class="heading"> 联系我们 </h1>
      <div class="container">
        <div class="contact-info-container">
          <div class="box">
            <i class="fas fa-phone"></i>
            <h3>手机</h3>
            <p>+123-456-7890</p>
          </div>
          <div class="box">
            <i class="fas fa-envelope"></i>
            <h3>邮箱</h3>
            <p>676104035@qq.com</p>
          </div>
          <div class="box">
            <i class="fas fa-map"></i>
            <h3>地址</h3>
            <p>四川省-绵阳市-安州区-绵阳城市学院</p>
          </div>
        </div>
        <div class="row align-items-center">
          <div class="col-md-6 mb-5 mb-md-0">
            <iframe class="map w-100" src="https://lbsyun.baidu.com/jsdemo/demo/k0_8.htm?lng=104.741723&lat=31.457341"
                    width="600" height="450" style="border:0;" allowfullscreen="" loading="lazy"></iframe>
          </div>
          <form action="" class="col-md-6">
            <h3>给我们留言</h3>
            <input type="text" placeholder="你的称呼" class="box" v-model="msg.name">
            <input type="email" placeholder="邮箱" class="box" v-model="msg.mail">
            <input type="number" placeholder="手机" class="box" v-model="msg.phone">
            <textarea name="" placeholder="留言信息" class="box" id="" cols="30" rows="10" v-model="msg.msg"></textarea>
            <button type="button" class="link-btn2" @click="sendMsg">发送信息</button>
          </form>
        </div>
      </div>
    </section>
    <!-- contact section ends -->
    <!-- newsletter section starts  -->
    <section class="newsletter" id="newsletter">
      <div class="container">
        <h3>订阅信息</h3>
        <p>获取我们平台最新用地信息和新功能</p>
        <form action="">
          <input type="email" name="" placeholder="请填写你的邮箱" id="3" class="email" v-model="msgSend.mail">
          <button type="button" class="link-btn2" @click="subscribe">提交</button>
        </form>
      </div>
    </section>
    <!-- newsletter section ends -->
  </div>
</template>
<script>
import TopBer from "@/components/Top";
import axiosInstance from "@/request/axiosInstance";

export default {
  components: {
    TopBer,
  },
  data() {
    return {
      msg: {
        id: null,
        name: null,
        phone: null,
        mail: null,
        msg: null,
        sendTime: null
      },
      msgSend: {
        id: null,
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
  methods: {
    async sendMsg() {
      if (this.checkMsg(0)){
        let {data: res} = await axiosInstance.post('/msg', this.msg)
        if(res.code===10001){
          this.msg = this.$options.data().msg
        }
        this.$notify.info({
          title: '系统提示',
          message: res.msg,
          duration: 2000,
          showClose: false
        })
      }
    },
    async subscribe() {
      if (this.checkMsg(1)) {
        let {data: res} = await axiosInstance.post('/msg/mail', this.msgSend)
        this.msgSend = this.$options.data().msgSend
        this.$notify.info({
          title: '系统提示',
          message: res.msg,
          duration: 2000,
          showClose: false
        })
      }
    },
    checkMsg(a) {
      if (a===0) {
        //用户名验证
        if (this.msg.name === null) {
          this.$notify.info({
            title: '系统提示',
            message: '请填写用户名',
            duration: 1000,
            showClose: false
          });
          return
        } else {
          const regex = /^\S{2,10}$/;
          if (!regex.test(this.msg.name)) {
            this.$notify.info({
              title: '系统提示',
              message: '用户名填写有误!',
              duration: 1000,
              showClose: false
            });
            return
          }
        }
        //手机号验证
        if (this.msg.phone === null) {
          this.$notify.info({
            title: '系统提示',
            message: '请填写手机号!',
            duration: 1000,
            showClose: false
          });
          return
        } else {
          const regex = /^1[3456789]\d{9}$/;
          if (!regex.test(this.msg.phone)) {
            this.$notify.info({
              title: '系统提示',
              message: '手机号格式有误!',
              duration: 1000,
              showClose: false
            });
            return
          }
        }
        //内容验证
        if (this.msg.msg === null) {
          this.$notify.info({
            title: '系统提示',
            message: '未填写内容!',
            duration: 1000,
            showClose: false
          });
          return
        } else {
          const regex = /^\S{5,100}$/;
          if (!regex.test(this.msg.msg)) {
            this.$notify.info({
              title: '系统提示',
              message: '字数要求在5-100之间!',
              duration: 1000,
              showClose: false
            });
            return
          }
        }
        //邮箱验证
        if (this.msg.mail === null) {
          this.$notify.info({
            title: '系统提示',
            message: '请填写邮箱',
            duration: 1000,
            showClose: false
          });
          return
        } else {
          const regex = /^[1-9]\d{3,13}@qq\.com$/;
          if (!regex.test(this.msg.mail)) {
            this.$notify.info({
              title: '系统提示',
              message: '邮箱填写有误!',
              duration: 1000,
              showClose: false
            });
            return
          }
        }
      }
      if (a===1){
        //邮箱验证
        if (this.msgSend.mail === null) {
          this.$notify.info({
            title: '系统提示',
            message: '请填写邮箱',
            duration: 1000,
            showClose: false
          });
          return
        } else {
          const regex = /^[1-9]\d{3,13}@qq\.com$/;
          if (!regex.test(this.msgSend.mail)) {
            this.$notify.info({
              title: '系统提示',
              message: '邮箱填写有误!',
              duration: 1000,
              showClose: false
            });
            return
          }
        }
      }
      return true;
    }
  }
}
</script>
<style>
.link-btn2 {
  width: 180px;
  padding: 1rem 3rem;
  display: inline-block;
  border: 0.1rem solid #105147;
  color: #105147;
  background: none;
  cursor: pointer;
  font-size: 1.7rem;
}

.link-btn2:hover {
  background: #105147;
  color: #fff;
}
.bigModel{
  background-color: rgba(210, 222, 214, 0.45);;
  width: 80px;
  height: 40px;
  position: fixed;
  top: 200px;
  right: 0;
  z-index: 99;
  border-radius: 10px 0 0 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center; /* 水平居中 */
  vertical-align: middle; /* 垂直居中 */
  font-size: 1.3rem;
  font-weight: bolder;
  cursor: pointer;
  color: #252424 !important;
}
.bigModel:hover{
  background-color:  rgba(16, 81, 71, 0.8);
  color: white !important;
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
</style>