<template>
  <div class="password-main">
    <div class="input-msg-title">我的订单</div>
    <p style="color: red">注意：未支付的订单会在次日0点自动取消</p>
    <div>
      <el-table
          ref="multipleTable"
          :data="tableData"
          tooltip-effect="dark"
          style="width: 99%"
          max-height="300px"
          empty-text="没有查询到你的订单"
          @selection-change="handleSelectionChange">
        <el-table-column
            type="selection"
            width="55"
            :selectable="customSelectable">
        </el-table-column>
        <el-table-column
            width="100"
        >
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
            width="110">
        </el-table-column>
        <el-table-column
            label="数量"
            width="60">
          <template slot-scope="scope">
            <div v-if="scope.row.num===-1">1</div>
            <div v-if="scope.row.num!==-1">{{ scope.row.num }}</div>
          </template>
        </el-table-column>
        <el-table-column
            label="总价"
            width="70">
          <template slot-scope="scope">
            <div>{{ scope.row.price }}</div>
          </template>
        </el-table-column>
        <el-table-column
            label="订单时间"
            width="110">
          <template slot-scope="scope">
            <div>{{ scope.row.createTime }}</div>
          </template>
        </el-table-column>
        <el-table-column
            label="商品类型"
            min-width="80"
        >
          <template slot-scope="scope">
            <el-tag v-if="scope.row.num!==-1" size="mini">普通商品</el-tag>
            <el-tag v-if="scope.row.num===-1" type="success" size="mini">用地</el-tag>
          </template>
        </el-table-column>
        <el-table-column
            label="支付状态"
            min-width="80"
        >
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status===0" type="warning" size="mini">未支付</el-tag>
            <el-tag v-if="scope.row.status===1" type="success" size="mini">已支付</el-tag>
            <el-tag v-if="scope.row.status===2" size="mini">已退款</el-tag>
          </template>
        </el-table-column>
        <el-table-column
            min-width="150"
            show-overflow-tooltip>
          <template slot-scope="scope">
            <button type="button" class="link-btn2" v-if="scope.row.status===0" @click="deleteOrder(scope.row.id,0)">
              取消订单
            </button>
            <button type="button" class="link-btn2" v-if="scope.row.status!==0 " @click="deleteOrder(scope.row.id,1)">
              删除订单
            </button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="input-msg">
      <button type="button" class="link-btn3">总金额: {{ totalPrice }}元</button>
      <button type="button" class="link-btn" @click="openPay()">去支付</button>
    </div>
    <el-dialog
        title="云用地支付"
        :visible.sync="dialogVisible"
        width="30%">
      <div style="display: flex;flex-direction: column;align-items: center; justify-content: center;gap: 10px">
        <img src="../../../../payImg/pay.png" alt="微信支付" style="width: 200px; height: 270px">
        <span>微信支付：{{ totalPrice }}元</span>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="payOrder(ids)">模拟支付</el-button>
        <el-button type="warning" @click="alipayOrder()">支付宝支付</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import router from "@/router";
import {APP_CONFIG} from "@/config/app";
import {createEmptyUser, fetchCurrentUser} from "@/utils/auth";

export default {
  name: "MyOrder",
  data() {
    return {
      dialogVisible: false,
      totalPrice: 0,
      landNum: 1,
      filePath1: APP_CONFIG.resourceUrls.productFile,
      filePath2: APP_CONFIG.resourceUrls.landFile,
      user: createEmptyUser(),
      msg: null,
      tableData: null,
      multipleSelection: [],
      ids: []
    }
  },
  async mounted() {
    if (sessionStorage.getItem("replace") === "1") {
      sessionStorage.setItem("replace", "0");
      location.reload();
    }
    const isValid = await this.openCheck();
    if (isValid) {
      await this.order();
    }
  },
  methods: {
    openPay() {
      if (this.multipleSelection.length===0){
        alert('未选择订单');
      }else {
        this.dialogVisible = true;
      }
    },
    customSelectable(row) {
      return row.status === 0;
    },
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
    handleSelectionChange(val) {
      this.totalPrice = 0;
      this.multipleSelection = val;
      this.ids = []
      if (val.length === 0) {
        return 0;
      }
      for (let i = 0; i < this.multipleSelection.length; i++) {
        this.ids[i] = this.multipleSelection[i].id
      }

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
    async order() {
      let {data: res} = await axiosInstance.post('/order/order', this.user)
      for (let i = 0; i < res.data.length; i++) {
        let dateTimeString = res.data[i].createTime;
        res.data[i].createTime = dateTimeString.replace(/T/g, ' ');
      }
      this.$set(this, 'tableData', res.data)
    },
    deleteOrder(id, status) {
      this.$confirm(status === 0 ? '确定取消该订单？' : '确定删除该订单？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        const {data: res} = await axiosInstance.delete('/order/' + id);
        if (res.code === 10002) {
          await this.order();
        } else {
          this.$message({
            type: 'warning',
            message: res.msg
          });
        }
      })
    },
    async alipayOrder() {
      const formData = new FormData();
      formData.append('orderIds', this.ids);
      formData.append('totalAmount', this.totalPrice.toFixed(2));
      formData.append('userId', this.user.id);
      const {data: res} = await axiosInstance.post('/alipay/pay', formData);
      if (res.code === 10001) {
        const win = window.open('', '_blank');
        win.document.write(res.data);
        win.document.forms[0].submit();
        this.dialogVisible = false;
      }
    },
    async payOrder(ids) {
      const formData = new FormData();
      formData.append('ids', ids);
      formData.append('status', 1);
      formData.append('time', true);
      const {data: res} = await axiosInstance.put('/order', formData)
      if (res.code===10003){
        this.dialogVisible=false
        await this.order();
      }
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
  min-width: 60px;
}

.link-btn2:hover {
  border: 0.1rem solid #105147;
  background: #105147;
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
.el-message-box__headerbtn .el-message-box__close:hover{
  font-size: larger;
  color: #000000;
}
</style>
