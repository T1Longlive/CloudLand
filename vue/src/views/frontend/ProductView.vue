<template>
  <div class="land">
    <TopBer></TopBer>
    <div class="land-main">
      <div class="land-select">
        <div class="land-input-select">
          <input placeholder="请输入搜索内容" class="input-select-box" v-model="product.landName">
          <i class="el-icon-search" id="land-el-icon-search" @click="conditionChange(product.landName,0)"></i>
        </div>
      </div>
      <div class="condition">
        <el-carousel :interval="4000" type="card" height="300px" style="width: 100%">
          <el-carousel-item v-for="(item,index) in tableData" :key="index">
            <img :src="filePath+item.img" alt="" class="condition-img">
          </el-carousel-item>
        </el-carousel>
      </div>
      <div class="land-list">
        <div class="land-list-sort">
          <div class="item" @click="conditionChange(null,4)" :class="{ 'landOrder': product.id===null }">默认排序</div>
          <div class="item"
               v-for="(item,index) in order"
               @click="conditionChange(index-3,4)"
               :class="{ 'landOrder': index-3===product.id }"
          >{{ item }}
          </div>
        </div>
        <div class="land-list-msg">
          <div class="item" v-for="item in tableData">
            <router-link :to="'/productInfo/'+item.id">
              <div class="land2-msg-item">
                <img :src="filePath+item.img " class="land-img" alt="用地封面">
                <div class="land2-msg">
                  <div class="land-text">
                    <h2>{{ item.productName }}</h2>
                    <p class="description">介绍：{{ item.description }}</p>
                    <div class="address-area">
                      <p>云用地自己生产</p>
                      <span>|</span>
                      <p>余量：{{ item.num }}斤</p>
                    </div>
                  </div>
                  <div class="product-price"><p>{{ (item.price).toFixed(2) }}元/斤</p></div>
                  <div class="product-price2"><p>{{ (item.price*1.2).toFixed(2) }}元/斤</p></div>
                </div>
              </div>
            </router-link>
          </div>
          <div class="item-bottom">
            <div class="block">
              <el-pagination
                  background
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                  :current-page.sync="currentPage"
                  :page-size="5"
                  layout="total, prev, pager, next"
                  :total="total">
              </el-pagination>
            </div>
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
  name: "ProductView",
  components: {
    TopBer,
  },
  data() {
    return {
      order: [
        '发布时间', '余量(升序)', '价格'
      ],
      selectMsg: null,
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
      //当前页
      currentPage: 1,
      address: null,
      //每页显示条数
      pageSize: 5,
      //数据总条数
      total: 100,
      //分页表格数据
      tableData: [],
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
  async mounted() {
    if (sessionStorage.getItem("replace") === "1") {
      sessionStorage.setItem("replace", "0");
      location.reload();
    }
    this.address = regionData
    await this.selectAll();
  },
  methods: {
    //查询所有
    async selectAll() {
      const formData = new FormData();
      formData.append('product', JSON.stringify(this.product));
      formData.append('pageNum', this.currentPage);
      formData.append('pageSize', this.pageSize);
      console.log(this.product)
      const {data: res} = await axiosInstance.post('/product/page', formData)
      this.total = res.data.total
      for (let i = 0; i < res.data.records.length; i++) {
        if (res.data.records[i].status === 1) {
          res.data.records[i].status = "启用"
        } else {
          res.data.records[i].status = "禁用"
        }
      }
      this.tableData = res.data.records
    },
    conditionChange(condition, type) {
      switch (type) {
        case 0:
          if (condition !== null) {
            this.product.productName = condition
          } else {
            this.product.productName = null
          }
          break
        case 4:
          this.product.id = condition
          break
      }

      this.selectAll()
    },
    //每页显示数据改变触发的事件
    handleSizeChange(val) {
      //当前页显示条数
      this.pageSize = val
      this.conditionChange(0)
    },
    //页数改变触发的事件
    handleCurrentChange(val) {
      //当前页
      this.currentPage = val
      this.conditionChange(0)
    },
  }
}
</script>

<style>
.description {
  /* 设置最大宽度 */
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-user-msg {
  font-size: 1.5rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.land {
  width: 100vw;
  height: 100vh;
}

.land-top {
  width: 100%;
  height: 13vh;
  background-color: #105147;
}

.land-top-msg {
  width: 80%;
  height: 100%;
  background-color: #105147;
  margin: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 900px;
}

.land-top-msg > a:hover {
  color: #71918b;
}

.logo {
  display: inline-block; /* 可选：使链接元素变成块级元素，以便设置宽度等属性 */
  font-size: 2.5rem;
  color: #fff;
  font-weight: bolder;
  padding-left: 1vw;
}

.land-top-msg-right {
  font-weight: bold;
  font-size: 1.5rem;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.land-top-msg-right > a {
  margin-right: 30px;
  color: #fff;
}

.land-top-msg-right > a:hover {
  color: #71918b;
}

.land-main {
  width: 80%;
  height: 87vh;
  margin: auto;
  min-width: 800px;
}

.land-select {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 80px;
}

.land-input-select {
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

#land-el-icon-search {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  width: 80px;
  height: 50px;
  color: #d2d2d2;
  background-color: #105147;
}

#land-el-icon-search:hover {
  color: white;
  background-color: #105147;
}

.condition {
  width: 100%;
  height: 330px;
  display: flex;
  min-height: 330px;
  min-width: 1000px;
  border-radius: 3px;
}
.condition-img{
  width: 100%;
  height: 100%;
}

.land-list {
  padding-top: 20px;
}

.land-list-sort {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 500px;
  border-bottom: 1px solid #777777; /* 下边框 */
}

.land-list-sort .item {
  flex: 1; /* 让每个子盒子自动扩展以占满剩余空间 */
  text-align: center; /* 文本居中，可选 */
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
  font-size: 1.5rem;
  font-weight: bolder;
  color: #363636;
  cursor: pointer;
}

.land-list-sort .item:hover {
  color: white;
  background-color: #105147;
}

.land-list-msg {
  background-color: #ffffff;
  padding-top: 20px;
}

.land-list-msg .item {
  height: 200px;
  padding: 20px 20px 20px 0;
  border-bottom: 1px solid #777777; /* 下边框 */
}

.land-list-msg .item-bottom {
  margin-top: 10px;
  height: 100px;
}

.land2-msg-item {
  display: flex;
  align-items: center;
  height: 160px;
}

.land-img {
  width: 213px;
  height: 160px;
}

.land2-msg {
  flex: 1;
  display: flex;
  height: 160px;
  justify-content: space-between;
}

.land-text {
  display: flex;
  padding-left: 20px;
  width: 550px;
  height: 160px;
  flex-direction: column; /* 将子元素竖着排列 */
}

.land-text > h2 {
  padding-bottom: 20px;
}

.product-price {
  flex: 1;
  display: flex;
  height: 160px;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: bolder;
  color: #20a895;
  padding-left: 10px;
}

.product-price2 {
  flex: 1;
  display: flex;
  height: 160px;
  align-items: center;
  justify-content: center;
  font-weight: bolder;
  color: #858585;
  font-size: 1.5rem;
  padding-left: 10px;
  text-decoration: line-through !important;
}

.land-text > h2 {
  color: #20a895;
  font-weight: bold;
}

.land-text > h2:hover {
  color: #187769;
}

.land-text > p {
  color: #919191;
}

.address-area {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.address-area p,
.address-area span {
  color: #919191;
  margin: 0; /* 去除段落标签的默认外边距 */
}

.address-area span {
  margin: 0 5px; /* 可以添加一些间距，使 "|" 与文本之间有空隙 */
}

.el-pagination.is-background .el-pager li:not(.disabled).active {
  background-color: #105147 !important;
  color: white !important;
}

.el-pagination.is-background .btn-next:hover, .el-pagination.is-background .btn-prev:hover, .el-pagination.is-background .el-pager li:hover {
  color: #105147 !important;
  background-color: white !important;
}

.landSelected {
  font-weight: bold;
  color: #721c24 !important;
}

.landOrder {
  color: white !important;
  background-color: #105147 !important;
}

.el-carousel__item h3 {
  color: #475669;
  font-size: 14px;
  opacity: 0.75;
  line-height: 200px;
  margin: 0;
}

.el-carousel__item:nth-child(2n) {
  background-color: #99a9bf;
}

.el-carousel__item:nth-child(2n+1) {
  background-color: #d3dce6;
}
#uImg:hover {
  border: none;
  box-shadow: 2px 2px 5px rgba(23, 20, 20, 0.8);
}
</style>
