<template>
  <div class="landInfo">
    <NavBar mode="page"></NavBar>
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
import NavBar from "@/components/NavBar";
import {APP_CONFIG} from "@/config/app";

export default {
  name: "LandInfoView",
  components: {
    NavBar,
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
            this.$message.warning("该商品已在购物车里了哦, 请勿重复添加!")
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
        this.$message.error("网络繁忙,请重试!")
        location.reload();
      }
    }
  }
}
</script>

<style scoped>
.link-btn2 {
  width: 180px;
  padding: 1rem 3rem;
  display: inline-block;
  border: 0.1rem solid var(--color-primary);
  color: var(--color-primary);
  background: none;
  cursor: pointer;
  font-size: 1.7rem;
}

.link-btn2:hover {
  background: var(--color-primary);
  color: #fff;
}

/* 操作按钮容器（原依赖 ProductInfoView 的全局样式，现收敛为本页自有） */
.productInfo-msg-4-2 {
  display: flex;
  gap: 20px;
}

.landInfo {
  width: 100vw;
  height: 100vh;
  background-color: #ffffff;
}

.landInfo-main {
  display: flex;
  width: 80%;
  height: 82vh;
  margin: auto;
  min-width: 1000px;
  justify-content: space-between;
  padding: 20px;
  border: 2px solid var(--color-border);
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

.land-Card {
  display: flex;
  flex-direction: column;
  height: 230px;
  border: 1px solid var(--color-border);
}

.land-Card-1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  height: 35%;
  border-bottom: 1px dashed var(--color-border);
}

.land-Card-1 > h2 {
  font-size: 1.5rem;
  font-weight: bolder;
  color: var(--color-accent);
}

.land-Card-1-2 {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 60px;
  height: 30px;
  color: #000000;
  background-color: #ffffff;
  border-radius: 2px;
  cursor: pointer;
}

.land-Card-1-2:hover {
  color: #ffffff;
  background-color: var(--color-primary);
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
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.landInfo-msg-1 {
  display: flex;
  padding: 20px;
  color: var(--color-accent);
  font-weight: bolder;
  font-size: 2rem;
  align-items: flex-end;
  border-bottom: 1px solid var(--color-border);
}

.landInfo-msg-2 {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-left: 20px;
  font-size: 1.2rem;
  font-weight: bolder;
  color: #404749;
  border-bottom: 1px solid var(--color-border);
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
  border-bottom: 1px solid var(--color-border);
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

.landInfo-msg-4 {
  display: flex;
  flex-direction: column;
  padding: 20px;
  height: 55%;
  gap: 10px;
  border-bottom: 1px solid var(--color-border);
}

.landInfo-msg-4 > p {
  color: #fd0000;
}

.landInfo-msg-4-1 {
  display: flex;
  flex-direction: column;
  font-size: 1.2rem;
  font-weight: bolder;
  color: #404749;
}
</style>
