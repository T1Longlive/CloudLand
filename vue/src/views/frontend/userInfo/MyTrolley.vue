<template>
  <div class="password-main">
    <div class="input-msg-title">我的购物车</div>
    <div>
      <el-table
          ref="multipleTable"
          :data="tableData"
          tooltip-effect="dark"
          style="width: 99%"
          max-height="300px"
          empty-text="你的购物车还没有商品"
          @selection-change="handleSelectionChange">
        <el-table-column
            type="selection"
            width="55"
            :selectable="customSelectable">
        </el-table-column>
        <el-table-column
            width="120">
          <template slot-scope="scope">
            <div class="demo-image__placeholder">
              <el-image
                  style="width: 80px; height: 50px;"
                  :src="scope.row.num!==-1?filePath1+scope.row.img:filePath2+scope.row.img"></el-image>
            </div>
          </template>
        </el-table-column>
        <el-table-column
            prop="productName"
            label="商品名称"
            width="100">
        </el-table-column>
        <el-table-column
            prop="price"
            label="价格/元"
            width="100">
        </el-table-column>
        <el-table-column
            label="数量"
            width="170">
          <template slot-scope="scope">
            <el-input-number size="mini" v-model="scope.row.num" :min="1" :max="scope.row.productNum"
                             v-if="scope.row.num!==-1 && scope.row.productNum>=1 && scope.row.status===1"
                             @change="changeNum([tableData[scope.$index]])"></el-input-number>
            <el-tooltip class="item" effect="dark" content="此项为用地无需选择数量!" placement="top">
              <el-input-number size="mini" v-model="landNum" :min="1" :max="1"
                               v-if="scope.row.num===-1&&scope.row.status===1"></el-input-number>
            </el-tooltip>
            <div v-if="(scope.row.num!==-1 && scope.row.productNum<=0) || (scope.row.num===-1 && scope.row.status===0)">
              补货中...
            </div>
            <div v-if="scope.row.productNum > 0 && scope.row.status===0">
              商品已下架...
            </div>
          </template>
        </el-table-column>
        <el-table-column
            label="商品类型"
            min-width="100"
        >
          <template slot-scope="scope">
            <el-tag v-if="scope.row.num!==-1" size="mini">普通商品</el-tag>
            <el-tag v-if="scope.row.num===-1" type="success" size="mini">用地</el-tag>
          </template>
        </el-table-column>
        <el-table-column
            min-width="150"
            show-overflow-tooltip>
          <template slot-scope="scope">
            <button type="button" class="link-btn2" @click="deleteTrolley(scope.row.id)">删除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="input-msg">
      <button type="button" class="link-btn3">总金额: {{ totalPrice }}元</button>
      <button type="button" class="link-btn" @click="addOrder">去结算</button>
    </div>

  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import router from "@/router";
import {APP_CONFIG} from "@/config/app";
import {createEmptyUser, fetchCurrentUser} from "@/utils/auth";

export default {
  name: "MyTrolley",
  data() {
    return {
      dialogVisible: false,
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
      totalPrice: 0,
      landNum: 1,
      filePath1: APP_CONFIG.resourceUrls.productFile,
      filePath2: APP_CONFIG.resourceUrls.landFile,
      user: createEmptyUser(),
      msg: null,
      tableData: null,
      multipleSelection: [],
      order: {
        id: null,
        pid: null,
        num: null,
        uid: null,
        createTime: null,
        payTime: null,
        status: 0,
        del: 0,
        username: null,
        phone: null
      }
    }
  },
  async mounted() {
    if (sessionStorage.getItem("replace") === "1") {
      sessionStorage.setItem("replace", "0");
      location.reload();
    }
    const isValid = await this.openCheck();
    if (isValid) {
      await this.trolley();
    }
  },
  methods: {
    changeNum(rows) {
      const selectedRows = this.$refs.multipleTable.selection;
      rows.forEach(row => {
        const isSelected = selectedRows.some(selectedRow => selectedRow.id === row.id);
        if (!isSelected) {
          this.$refs.multipleTable.toggleRowSelection(row, true);
        } else {
          this.handleSelectionChange(this.multipleSelection)
        }
      });
    },
    customSelectable(row) {
      return row.num !== -1 && row.status === 1 ? row.productNum >= 1 : row.status !== 0;
    },

    handleSelectionChange(val) {
      this.totalPrice = 0;
      this.multipleSelection = val;
      for (let i = 0; i < this.multipleSelection.length; i++) {
        if (this.multipleSelection[i].num === -1) {
          this.totalPrice = this.multipleSelection[i].price + this.totalPrice;
        } else {
          this.totalPrice = this.multipleSelection[i].num * this.multipleSelection[i].price + this.totalPrice;
        }
      }
    },

    async openCheck() {
      this.user = createEmptyUser()
      const {user} = await fetchCurrentUser(true)
      if (!user) {
        const currentRoute = router.currentRoute;
        if (currentRoute.path !== '/') {
          await router.push({path: '/'});
        }
        return false
      }
      this.user = user;
      return true
    },

    async trolley() {
      let {data: res} = await axiosInstance.post('/user/trolley', this.user)
      this.tableData = res.data
    },

    deleteTrolley(id) {
      this.$confirm('确定从购物车移除该商品？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        const {data: res} = await axiosInstance.delete('/user/trolley/' + id);
        if (res.code === 10002) {
          await this.trolley();
        } else {
          this.$message({
            type: 'warning',
            message: res.msg
          });
        }
      })
    },
    async addOrder() {
      if (this.multipleSelection.length === 0) {
        alert("未选择商品")
        return
      }
      const loading = this.$loading({
        lock: true,
        text: '订单生成中...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      });
      let errArray = [];
      let index = 0
      for (let i = 0; i < this.multipleSelection.length; i++) {
        this.order.uid = this.multipleSelection[i].uid
        this.order.pid = this.multipleSelection[i].pid
        this.order.num = this.multipleSelection[i].num
        let {data: res} = await axiosInstance.post('/order', this.order)
        if (res.code === 10001) {
          await axiosInstance.delete('/user/trolley/' + this.multipleSelection[i].id);
        } else if (res.code === 10005) {
          errArray[index] = i
          index++
        }
      }
      setTimeout(() => {
        loading.close();
        if (errArray.length > 0) {
          for (let i = 0; i < errArray.length; i++) {
            alert("ID [" + this.multipleSelection[errArray[i]].id + "] 名称 [" + this.multipleSelection[errArray[i]].productName + "] 的订单生成失败，请重试")
          }
          location.reload()
        } else {
          sessionStorage.setItem("order", "")
        }
      }, 2000);
      setTimeout(() => {
        loading.close();
        setTimeout(() => {
          this.openFullScreen();
        }, 1000)
      }, 2000);
    },
    openFullScreen() {
      const loading = this.$loading({
        lock: true,
        text: '正在跳转到订单页面...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      });
      setTimeout(async () => {
        loading.close();
        await this.$router.push('/user/myOrder');
      }, 2000);
    },
  }
}
</script>

<style>
.password-main {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  gap: 30px; /* 设置元素之间的间距 */
}

.input-msg-title {
  color: #6c757d;
  font-size: 2rem;
  border-bottom: 2px solid #bebebe;
  padding-bottom: 10px;
}

.input-msg {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between; /* 使两个子盒子分别靠左和靠右 */
}

.input-password {
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

.el-icon-unlock {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  width: 80px;
  height: 50px;
  color: #d2d2d2;
  background-color: #105147;
}

.el-icon-lock {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  width: 80px;
  height: 50px;
  color: #d2d2d2;
  background-color: #105147;
}

.link-btn {
  width: 20%;
  margin-top: 1rem;
  padding: 1rem 3rem;
  display: inline-block;
  border: 0.1rem solid #105147;
  color: #105147;
  background: none;
  cursor: pointer;
  font-size: 1.4rem;
}

.link-btn:hover {
  background: #105147;
  color: #fff;
}

.link-btn2 {
  width: 30%;
  border: 0.1rem solid #105147;
  color: #105147;
  background: none;
  cursor: pointer;
  font-size: 1rem;
}

.link-btn2:hover {
  border: 0.1rem solid #b92626;
  background: #b92626;
  color: #fff;
}

.link-btn3 {
  width: 30%;
  margin-top: 1rem;
  display: inline-block;
  border: none;
  color: #105147;
  background: none;
  font-size: 1.5rem;
}

.el-checkbox__inner:hover {
  border-color: #105147 !important;
}

.el-checkbox__input.is-focus .el-checkbox__inner {
  border-color: #105147 !important;
}

.el-checkbox__input.is-checked .el-checkbox__inner,
.el-checkbox__input.is-indeterminate .el-checkbox__inner {
  border-color: #105147 !important;
  background-color: #105147 !important;
}
.el-loading-spinner .el-loading-text {
  color: #fdfdfd;
}
.el-loading-spinner i {
  color: #fdfdfd;
}
.el-input--mini .el-input__inner{
  border-color: #bbbbbb !important;
}
.el-input--mini .el-input__inner:hover{
  border-color: #bbbbbb !important;
}
.el-icon-plus{
  color: #105147;
}
.el-icon-plus:hover{
  color: #105147;
}
.el-icon-plus:focus{
  color: #105147;
}
.el-icon-minus{
  color: #105147;
}
.el-icon-minus:hover{
  color: #105147;
}
.el-icon-minus:focus{
  color: #105147;
}

</style>
