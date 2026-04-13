<template>
  <div class="productInfo">
    <TopBer></TopBer>
    <div class="productInfo-main">
      <div class="productInfo-img">
        <div class="block">
          <el-carousel height="270px" :autoplay="false">
            <el-carousel-item v-for="(item,index) in 1" :key="index">
              <el-image
                  style="width: 100%; height: 100%;border-radius: 5px"
                  :src="filePath+product.img"></el-image>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="product-Card">
          <div class="product-Card-1">
            <h2>云用地-成都用地服务中心</h2>
            <div class="product-Card-1-2">了解更多</div>
          </div>
          <div class="product-Card-2">
            <div class="product-Card-2-1">
              <p>服务热线：{{ product.customerAPhone }}</p>
              <p>联系人：{{ product.customerAUsername }}</p>
              <p>支持云用地</p>
            </div>
            <div class="product-Card-2-2">分享</div>
          </div>
        </div>
      </div>
      <div class="productInfo-msg">
        <div class="productInfo-msg-1">
          <p>{{ product.productName }}</p>
        </div>
        <div class="productInfo-msg-2">
          <div class="productInfo-msg-2-1">
            <div>价格：</div>
            <div>{{ product.price }}元/斤</div>
          </div>
          <div class="productInfo-msg-2-2">
            <div>总价：</div>
            <div>{{ product.price * product.num }}元</div>
          </div>
        </div>
        <div class="productInfo-msg-3">
          <div class="productInfo-msg-3-1">
            <p>产品介绍：{{ product.description }}</p>
          </div>
          <div class="productInfo-msg-3-2">
            <p v-if="product.num>0">余量：{{ product.num }}</p>
            <p v-if="product.num<=0">余量：补货中...</p>
          </div>
        </div>
        <div class="productInfo-msg-4">
          <p>温馨提示：国家法律规定。。。。。。。。。。。。。</p>
          <div class="productInfo-msg-4-1">
            <p>云用地套餐服务</p>
            <p>云用地套餐服务1。。。。。。。。</p>
            <p>云用地套餐服务2。。。。。。。。</p>
            <p>云用地套餐服务3。。。。。。。。</p>
          </div>
          <div class="productInfo-msg-4-2">
            <button type="button" class="link-btn2" @click="addTrolley(0)">加入购物车</button>
            <button type="button" class="link-btn2" @click="addTrolley(1)">立即购买</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {CodeToText, regionData} from "element-china-area-data";
import axiosInstance from "@/request/axiosInstance";
import TopBer from "@/components/Top2";
import {APP_CONFIG} from "@/config/app";

export default {
  name: "ProductInfoView",
  components: {
    TopBer,
  },
  data() {
    return {
      filePath: APP_CONFIG.resourceUrls.productFile,
      filePath2: APP_CONFIG.resourceUrls.userFile,
      product: {
        id: null,
        productName: null,
        description: null,
        ordered: null,
        price: null,
        img: null,
        status: 1,
        num: null,
        aId: null,
        customerAUsername: null,
        customerAPhone: null
      },
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
      trolley: {
        id: null,
        pid: null,
        num: null,
        uid: null,
      }
    }

  },
  async mounted() {
    if (sessionStorage.getItem("replace") === "1") {
      sessionStorage.setItem("replace", "0");
      location.reload();
    }
    this.address = regionData
    await this.selectLand();
  },
  methods: {
    async selectLand() {
      const id = this.$route.params.id;
      const {data: res} = await axiosInstance.get('/product/' + id);
      this.product = res.data;
    },
    async addTrolley(choose) {
      this.user.id=sessionStorage.getItem('userID')
      let {data: res1} = await axiosInstance.post('/user/trolley', this.user)
      console.log(res1)
      for (let i = 0; i < res1.data.length; i++) {
        if (res1.data[i].pid === this.product.id) {
          if (choose===1){
            await this.$router.push('/user/myTrolley');
          }else{
            alert("该商品已在购物车里了哦, 请勿重复添加!")
          }
          return
        }
      }

      this.trolley.pid = this.product.id
      this.trolley.uid = sessionStorage.getItem('userID')
      this.trolley.num = 1
      let {data: res2} = await axiosInstance.post('/user/addTrolley', this.trolley)
      if (res2.code === 10001 && choose === 0) {
        this.$confirm("是否前往购物车页面?", '添加成功', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        }).then(async () => {
          await this.$router.push('/user/myTrolley');
        })
      } else if (res2.code === 10001 && choose === 1) {
        await this.$router.push('/user/myTrolley');
      } else {
        alert("网络繁忙,请重试!")
        location.reload();
      }
    },

  }
}
</script>
<style>
.link-btn2 {
  margin-top: 50px;
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


.productInfo {
  width: 100vw;
  height: 100vh;
  background-color: #ffffff;
}

.productInfo-top-msg > a:hover {
  color: #71918b;
}

.productInfo-top-msg-right > a {
  margin-right: 30px;
  color: #fff;
}

.productInfo-top-msg-right > a:hover {
  color: #71918b;
}

.productInfo-main {
  display: flex;
  width: 80%;
  height: 82vh;
  margin: auto;
  min-width: 1000px;
  justify-content: space-between;
  padding: 20px;
  border: 2px solid #bebebe;
  border-top: none;
  gap: 30px;
  min-height: 600px;
}

.productInfo-img {
  display: flex;
  flex-direction: column;
  width: 450px;
  height: 100%;
  min-width: 450px;
  gap: 50px;
}

.productInfo-msg {
  flex: 1;
  height: 100%;
}

.product-Card {
  display: flex;
  flex-direction: column;
  height: 230px;
  border: 1px solid #bebebe;
}

.el-carousel__item h3 {
  color: #475669;
  font-size: 14px;
  opacity: 0.75;
  line-height: 150px;
  margin: 0;
}

.product-Card-1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  height: 35%;
  border-bottom: 1px dashed #bebebe;
}

.product-Card-1 > h2 {
  font-size: 1.5rem;
  font-weight: bolder;
  color: #2cb7c0;
}

.product-Card-1-2 {
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
  width: 60px;
  height: 30px;
  color: #000000;
  background-color: #ffffff;
  border-radius: 2px;
  cursor: pointer; /* 鼠标小手效果 */
}

.product-Card-1-2:hover {
  color: #ffffff;
  background-color: #105147;
}

.product-Card-2 {
  font-size: 1.2rem;
  padding: 10px;
  display: flex;
  justify-content: space-between;
  height: 65%;
  font-weight: bolder;
  color: #404749;
}

.product-Card-2-2 {
  display: flex;
  align-items: flex-end;
}

.productInfo-msg {
  display: flex;
  flex-direction: column;
}

.productInfo-msg-1 {
  display: flex;
  height: 15%;
}

.productInfo-msg-2 {
  display: flex;
  height: 20%;
}

.productInfo-msg-4 {
  display: flex;
  flex-direction: column;
  padding: 20px;
  height: 55%;
  gap: 20px;
}

.productInfo-msg-4 > p {
  color: #fd0000;
}

.productInfo-msg-1 {
  display: flex;
  padding: 20px;
  color: #2cb7c0;
  font-weight: bolder;
  font-size: 2rem;
  align-items: flex-end;
  border-bottom: 1px solid #bebebe;
}

.productInfo-msg-2 {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-left: 20px;
  font-size: 1.2rem;
  font-weight: bolder;
  color: #404749;
  border-bottom: 1px solid #bebebe;
}

.productInfo-msg-2-1 {
  display: flex;
  align-items: center;
  height: 50%;
  gap: 20px;
}


.productInfo-msg-2-2 {
  display: flex;
  height: 50%;
  align-items: center;
  gap: 20px;
}

.productInfo-msg-2-2 > p {
  width: 70px;
  background-color: #721c24;
}

.productInfo-msg-3 {
  display: flex;
  padding: 20px;
  gap: 20%;
  border-bottom: 1px solid #bebebe;
  height: 30%;
  font-size: 1.1rem;
  font-weight: bolder;
  color: #6c757d;
  max-height: 120px;
}

.productInfo-msg-3-1 {
  width: 80%;
}

.productInfo-msg-3-2 {
  width: 20%;
  min-width: 50px;
}

.productInfo-msg-4-1 {
  display: flex;
  flex-direction: column;
  font-size: 1.2rem;
  font-weight: bolder;
  color: #404749;
}

.productInfo-msg-4-2 {
  display: flex;
  gap: 20px;
}
.el-button--primary {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-button--primary.is-plain {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-button--primary:focus, .el-button--primary:hover {
  background-color: #105147;
  border-color: #105147;
}
.el-button--primary.is-plain:focus, .el-button--primary.is-plain:hover {
  background-color: #105147;
  border-color: #105147;
}


.el-button:hover{
  background: #105147;
  border-color: #105147;
  color: #FFF;
}
.el-message-box__headerbtn .el-message-box__close:hover{
  font-size: larger;
  color: #000000;
}
</style>
