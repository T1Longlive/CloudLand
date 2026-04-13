<template>
  <div class="landInfo">
    <TopBer></TopBer>
    <div class="landInfo-main">
      <div class="landInfo-img">
        <div class="block">
          <el-carousel height="270px" :autoplay="false">
            <el-carousel-item v-for="(item,index) in Land.imageFiles" :key="index">
              <el-image
                  style="width: 100%; height: 100%;border-radius: 5px"
                  :src="filePath+item.path"></el-image>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="land-Card">
          <div class="land-Card-1">
            <h2>云用地-绵阳用地服务中心</h2>
            <div class="land-Card-1-2">了解更多</div>
          </div>
          <div class="land-Card-2">
            <div class="land-Card-2-1">
              <p>服务热线：{{ Land.employeePhone }}</p>
              <p>联系人：{{ Land.employeeUsername }}</p>
              <el-tag type="success">  <p>支持云用地</p></el-tag>
            </div>
            <div class="land-Card-2-2">分享</div>
          </div>
        </div>
      </div>
      <div class="landInfo-msg">
        <div class="landInfo-msg-1">
          <p>{{ Land.landName }}</p>
        </div>
        <div class="landInfo-msg-2">
          <div class="landInfo-msg-2-1">
            <div>价格：</div>
            <div>{{ Land.price }}元/平方米/天</div>
          </div>
          <div class="landInfo-msg-2-2">
            <div>总价：</div>
            <div>{{ Land.price * Land.area }}元/平方米/天</div>
          </div>
        </div>
        <div class="landInfo-msg-3">
          <div class="landInfo-msg-3-1">
            <p>地址：{{ Land.address }}</p>
            <p>详细地址：{{ Land.detailedAddress }}</p>
            <p>用地类型：{{ Land.typeName }}</p>
            <p>用地类型描述：{{ Land.typeDescription }}</p>
          </div>
          <div class="landInfo-msg-3-2">
            <p>面积：{{ Land.area }}</p>
            <p>用地描述：{{ Land.description }}</p>
            <p>用地资料： <a :href="filePath+Land.landFiles.path" v-cloak>{{ "点击下载" }}</a></p>
          </div>
        </div>
        <div class="landInfo-msg-4">
          <p>温馨提示：国家法律规定。。。。。。。。。。。。。</p>
          <div class="landInfo-msg-4-1">
            <p>云用地套餐服务</p>
            <p>1、看地服务全程包接送</p>
            <p>2、土地管理提供技术顾问和技术支持</p>
            <p>3、可提交给平台的土地代理人员进行土地管理</p>
          </div>
          <div class="productInfo-msg-4-2">
            <button type="button" class="link-btn2" @click="addTrolley(0)">加入预约</button>
            <button type="button" class="link-btn2" @click="addTrolley(1)">立即租用</button>
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
  name: "LandInfoView",
  components: {
    TopBer,
  },
  data() {
    return {
      filePath: APP_CONFIG.resourceUrls.landFile,
      filePath2: APP_CONFIG.resourceUrls.userFile,
      Land: {
        id: null,
        landName: null,
        landType: null,
        description: 0,
        imageFiles: [],
        ordered: null,
        landFiles: [],
        price: 10,
        aId: null,
        status: 1,
        area: null,
        employeeId: null,
        detailedAddress: null,
        typeName: null,
        typeDescription: null,
        customerAUsername: null,
        customerAPhone: null,
        employeeUsername: null,
        employeePhone: null,
        province: null,
        city: null,
        county: null,
        address: null
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
        pId: null,
        num: null,
        uId: null,
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
      const {data: res} = await axiosInstance.get('/land/' + id);
      const addressString = res.data.address;
      const addressArray = addressString.split(",");
      res.data.address = CodeToText[addressArray[0]] + "-" + CodeToText[addressArray[1]] + "-" + CodeToText[addressArray[2]]
      this.Land = res.data;
      console.log(this.Land)
    },
    async addTrolley(choose) {
      let {data: res1} = await axiosInstance.post('/user/trolley', this.user)
      for (let i = 0; i < res1.data.length; i++) {
        if (res1.data[i].pid === this.Land.id) {
          if (choose === 1) {
            await this.$router.push('/user/myTrolley');
          } else {
            alert("该商品已在购物车里了哦, 请勿重复添加!")
          }
          return
        }
      }

      this.trolley.pid = this.Land.id
      this.trolley.uid = sessionStorage.getItem('userID')
      this.trolley.num = -1
      let {data: res} = await axiosInstance.post('/user/addTrolley', this.trolley)
      if (res.code === 10001 && choose === 0) {
        this.$confirm("是否前往购物车页面?", '添加成功', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        }).then(async () => {
          await this.$router.push('/user/myTrolley');
        })
      } else if (res.code === 10001 && choose === 1) {
        await this.$router.push('/user/myTrolley');
      } else {
        alert("网络繁忙,请重试!")
        location.reload();
      }
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

.productInfo-msg-4-2 {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
}

.user-user-msg {
  font-size: 1.5rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}


.landInfo {
  width: 100vw;
  height: 100vh;
  background-color: #ffffff;
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

.landInfo-main {
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

.landInfo-img {
  display: flex;
  flex-direction: column;
  width: 450px;
  height: 100%;
  min-width: 450px;
  gap: 50px;
}

.landInfo-msg {
  flex: 1;
  height: 100%;
}

.land-Card {
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

.land-Card-1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  height: 35%;
  border-bottom: 1px dashed #bebebe;
}

.land-Card-1 > h2 {
  font-size: 1.5rem;
  font-weight: bolder;
  color: #2cb7c0;
}

.land-Card-1-2 {
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

.land-Card-1-2:hover {
  color: #ffffff;
  background-color: #105147;
}

.land-Card-2 {
  font-size: 1.2rem;
  padding: 10px;
  display: flex;
  justify-content: space-between;
  height: 65%;
  font-weight: bolder;
  color: #404749;
}

.land-Card-2-2 {
  display: flex;
  align-items: flex-end;
}

.landInfo-msg {
  display: flex;
  flex-direction: column;
}

.landInfo-msg-1 {
  display: flex;
  height: 15%;
}

.landInfo-msg-2 {
  display: flex;
  height: 20%;
}

.landInfo-msg-4 {
  display: flex;
  flex-direction: column;
  padding: 20px;
  height: 55%;
  gap: 10px;
  border-bottom: 1px solid #bebebe;
}

.landInfo-msg-4 > p {
  color: #fd0000;
}

.landInfo-msg-1 {
  display: flex;
  padding: 20px;
  color: #2cb7c0;
  font-weight: bolder;
  font-size: 2rem;
  align-items: flex-end;
  border-bottom: 1px solid #bebebe;
}

.landInfo-msg-2 {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-left: 20px;
  font-size: 1.2rem;
  font-weight: bolder;
  color: #404749;
  border-bottom: 1px solid #bebebe;
}

.landInfo-msg-2-1 {
  display: flex;
  align-items: center;
  height: 50%;
  gap: 20px;
}


.landInfo-msg-2-2 {
  display: flex;
  height: 50%;
  align-items: center;
  gap: 20px;
}

.landInfo-msg-2-2 > p {
  width: 70px;
  background-color: #721c24;
}

.landInfo-msg-3 {
  display: flex;
  padding: 20px;
  gap: 20%;
  border-bottom: 1px solid #bebebe;
  height: 30%;
  font-size: 1.1rem;
  font-weight: bolder;
  color: #6c757d;
}

.landInfo-msg-3-1 {
  width: 50%;
}

.landInfo-msg-3-2 {
  width: 50%;
}

.landInfo-msg-4-1 {
  display: flex;
  flex-direction: column;
  font-size: 1.2rem;
  font-weight: bolder;
  color: #404749;
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
