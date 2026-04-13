<template>
  <div class="land">
    <TopBer></TopBer>
    <div class="land-main">
      <div class="land-select">
        <div class="land-input-select">
          <input placeholder="请输入搜索内容" class="input-select-box" v-model="Land.landName">
          <i class="el-icon-search" id="land-el-icon-search" @click="conditionChange(Land.landName,0)"></i>
        </div>
      </div>
      <div class="condition2">
        <div class="condition2-title">
          <div class="item">区域:</div>
          <div class="item">分类:</div>
          <div class="item">（平方米 m²）面积:</div>
        </div>
        <!--土地地址-->
        <div class="condition2-list">
          <div class="item">
            <div class="sub-item"
                 @click="conditionChange(null,1)"
                 :class="{ 'landSelected': Land.address===null }"
            >全部
            </div>
            <div class="sub-item"
                 v-for="(index)  in address"
                 @click="conditionChange(index,1)"
                 :class="{ 'landSelected': index.value===Land.address }"
            >{{ index.label }}
            </div>
          </div>
          <!--土地类型-->
          <div class="item">
            <div
                class="sub-item"
                @click="conditionChange(null,2)"
                :class="{ 'landSelected': Land.landType===null }"
            >不限
            </div>
            <div
                class="sub-item"
                v-for="(item,index)  in LandType"
                @click="conditionChange(index+1,2)"
                :class="{ 'landSelected': index+1===Land.landType }"
            >{{ item }}
            </div>
          </div>
          <!--面积-->
          <div class="item">
            <div
                class="sub-item"
                @click="conditionChange(null,3)"
                :class="{ 'landSelected': Land.area===null }"
            >不限
            </div>
            <div
                class="sub-item"
                v-for="(index) in LandArea"
                @click="conditionChange(index,3)"
                :class="{ 'landSelected': index===Land.area }"
            >{{ ">" + index }}
            </div>
          </div>
        </div>
      </div>
      <div class="land-list">
        <div class="land-list-sort">
          <div class="item" @click="conditionChange(null,4)" :class="{ 'landOrder': Land.id===null }">默认排序</div>
          <div class="item"
               v-for="(item,index) in order" :key="index"
               @click="conditionChange(index-3,4)"
               :class="{ 'landOrder': index-3===Land.id }"
          >{{ item }}
          </div>
        </div>
        <div class="land-list-msg">
          <div class="item" v-for="item in tableData">
            <router-link :to="'/landInfo/'+item.id">
              <div class="land-msg-item">
                <img :src="filePath+item.imageFiles[0].path " class="land-img" alt="用地封面">
                <div class="land-msg">
                  <div class="land-text">
                    <h2>{{ item.landName }}</h2>
                    <p>用地类型：{{ item.typeName }}</p>
                    <p>用途：{{ item.typeDescription }}</p>
                    <div class="address-area">
                      <p>地址：{{ item.address }}</p>
                      <span>|</span>
                      <p>面积：{{ item.area }}平方米</p>
                    </div>
                  </div>

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
import axiosInstance from "@/request/axiosInstance";
import {regionData, CodeToText} from "element-china-area-data";
import TopBer from "@/components/Top2";
import {APP_CONFIG} from "@/config/app";

export default {
  name: "LandView",
  components: {
    TopBer,
  },
  data() {
    return {
      order: [
        '发布时间', '面积(升序)', '价格'
      ],
      selectMsg: null,
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
      //当前页
      currentPage: 1,
      address: null,
      //每页显示条数
      pageSize: 5,
      //数据总条数
      total: 100,
      //分页表格数据
      tableData: [],
      LandType: [
        "农用地", "建设用地", "商业用地", "公共管理与公共服务用地", "水域及水利设施用地", "其他"
      ],
      LandArea: [
        10, 50, 100, 500, 1000, 2000, 5000, 10000, 20000, 50000
      ],
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
    this.address = regionData[22].children[5].children
    await this.selectAll();
  },
  methods: {
    //查询所有
    async selectAll() {
      const formData = new FormData();
      formData.append('land', JSON.stringify(this.Land));
      formData.append('pageNum', this.currentPage);
      formData.append('pageSize', this.pageSize);
      const {data: res} = await axiosInstance.post('/land/page', formData)
      this.total = res.data.total
      for (let i = 0; i < res.data.records.length; i++) {
        const addressString = res.data.records[i].address;
        const addressArray = addressString.split(",");
        res.data.records[i].address = CodeToText[addressArray[0]] + "-" + CodeToText[addressArray[1]] + "-" + CodeToText[addressArray[2]]
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
            this.Land.landName = condition
          } else {
            this.Land.landName = null
          }
          break
        case 1:
          if (condition !== null) {
            this.Land.address = condition.value
          } else {
            this.Land.address = null
          }
          break
        case 2:
          this.Land.landType = condition
          break
        case 3:
          this.Land.area = condition
          break
        case 4:
          this.Land.id = condition
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
.land {
  width: 100vw;
  height: 100vh;
}

.land-top-msg > a:hover {
  color: #71918b;
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

.condition2 {
  width: 100%;
  height: 30vh;
  background-color: #e5e5e5;
  display: flex;
  min-height: 100px;
  border-radius: 3px;
}

.condition2-title {
  width: 10%;
  height: 30vh;
  display: flex;
  flex-direction: column;
  color: #312f2f;
  font-size: 1rem;
  font-weight: bolder;
  min-height: 200px;
}

.condition2-title .item {
  flex: 1; /* 让每个子盒子自动扩展以占满剩余空间 */
  /*border: 1px solid #000; !* 添加边框，可选 *!*/
  text-align: center; /* 文本居中，可选 */
  display: flex;
  /*align-items: center;*/
  justify-content: right;
  margin-top: 20px;
  margin-right: 20px;
}

.condition2-list {
  display: flex;
  flex-wrap: wrap;
  width: 90%;
  height: 30vh;
  flex-direction: column;
  min-height: 200px;
}

.condition2-list .item {
  display: flex;
  flex-wrap: wrap;
  flex: 1; /* 让每个子盒子自动扩展以占满剩余空间 */
  /*align-items: center;*/
  margin-top: 20px;
}

.condition2-list .sub-item {
  height: 20px;
  color: #919191;
  margin-right: 20px;
  text-align: center;
  font-size: 1rem;
  cursor: pointer;
}

.condition2-list .sub-item:hover {
  color: #000000;
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

.land-msg-item {
  display: flex;
  align-items: center;
  height: 160px;
}

.land-img {
  width: 213px;
  height: 160px;
}

.land-msg {
  flex: 1;
  display: flex;
  height: 160px;
  justify-content: space-between;
}

.land-text {
  display: flex;
  padding-left: 20px;
  width: 80%;
  height: 160px;
  flex-direction: column; /* 将子元素竖着排列 */
}

.land-text > h2 {
  padding-bottom: 20px;
}

.land-price {
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
  color: #000000 !important;
}

.landOrder {
  color: white !important;
  background-color: #105147 !important;
}
</style>
