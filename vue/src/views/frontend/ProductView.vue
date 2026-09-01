<template>
  <div class="land">
    <NavBar mode="page"></NavBar>
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
import NavBar from "@/components/NavBar";
import {APP_CONFIG} from "@/config/app";

export default {
  name: "ProductView",
  components: {
    NavBar,
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

<style scoped>
.description {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.land {
  width: 100vw;
  height: 100vh;
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
  border: 2px solid var(--color-primary);
  padding: 20px;
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
  background-color: var(--color-primary);
}

#land-el-icon-search:hover {
  color: white;
  background-color: var(--color-primary);
}

.condition {
  width: 100%;
  height: 330px;
  display: flex;
  min-height: 330px;
  min-width: 1000px;
  border-radius: 3px;
}

.condition-img {
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
  border-bottom: 1px solid #777777;
}

.land-list-sort .item {
  flex: 1;
  text-align: center;
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
  background-color: var(--color-primary);
}

.land-list-msg {
  background-color: #ffffff;
  padding-top: 20px;
}

.land-list-msg .item {
  height: 200px;
  padding: 20px 20px 20px 0;
  border-bottom: 1px solid #777777;
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
  flex-direction: column;
}

.land-text > h2 {
  padding-bottom: 20px;
  color: var(--color-accent);
  font-weight: bold;
}

.land-text > h2:hover {
  color: var(--color-primary);
}

.land-text > p {
  color: var(--color-text-muted);
}

.product-price {
  flex: 1;
  display: flex;
  height: 160px;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: bolder;
  color: var(--color-accent);
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

.address-area {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.address-area p,
.address-area span {
  color: var(--color-text-muted);
  margin: 0;
}

.address-area span {
  margin: 0 5px;
}

.landOrder {
  color: white !important;
  background-color: var(--color-primary) !important;
}
</style>
