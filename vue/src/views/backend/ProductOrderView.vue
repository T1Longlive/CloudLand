<template>
  <div class="productView">
    <el-container>
      <!--查询表单-->
      <div class="select">
        <el-form :inline="true" :model="order" class="demo-form-inline">
          <el-form-item label="查询条件">
            <el-select v-model="condition" placeholder="当前条件" @change="conditionChange(false)" style="width: 150px">
              <el-option label="全部" :value="0"></el-option>
              <el-option label="订单ID" :value="1"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="">
            <el-tooltip class="item" effect="dark" content="请先选择查询条件" placement="bottom-start"
                        :disabled="condition!==0">
              <el-input v-model="conditionText" placeholder="输入查询内容" :disabled="condition===0"
                        @keyup.native.enter="conditionChange(true)"></el-input>
            </el-tooltip>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="conditionChange(true)">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" plain @click="orderDownload" size="mini"
                       icon="el-icon-download">产品订单导出
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-container>
    <div class="wrap">
      <transition name="fade" mode="out-in">
        <!--数据列表-->
        <el-table
            stripe
            v-if="tableShow"
            highlight-selection-row
            :data="tableData"
            style="width: 100%"
            @selection-change="checkChange"
            height="460px"
        >
          <!--用户信息展开部分-->
          <el-table-column type="expand">
            <template slot-scope="props">
              <el-form label-position="left" inline class="demo-table-expand">
                <el-form-item label="订单ID ：" class="expandLabel">
                  <span v-cloak>{{ props.row.id }}</span>
                </el-form-item>
                <el-form-item label="订单分类 ：" class="expandLabel">
                  <el-tag v-if="props.row.num!==-1" size="mini">普通商品</el-tag>
                  <el-tag v-if="props.row.num===-1" type="success" size="mini">用地</el-tag>
                </el-form-item>
                <el-form-item label="下单时间 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.createTime }}</span>
                </el-form-item>
                <el-form-item label="支付时间 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.payTime }}</span>
                </el-form-item>
                <el-form-item label="下单人 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.username }}</span>
                </el-form-item>
                <el-form-item label="下单人联系电话 ：" class="expandLabel">
                  <el-tag>{{ props.row.phone }}</el-tag>
                </el-form-item>
              </el-form>
            </template>
          </el-table-column>
          <el-table-column
              width="100"
          >
            <template slot-scope="scope">
              <div class="demo-image__placeholder">
                <el-image
                    style="width: 80px; height: 50px;"
                    :src="scope.row.num===-1?filePath1+scope.row.img:filePath2+scope.row.img"></el-image>
              </div>
            </template>
          </el-table-column>
          <el-table-column
              prop="id"
              label="订单号"
              min-width="100px">
          </el-table-column>
          <el-table-column
              prop="productName"
              label="商品名称"
              min-width="150px">
          </el-table-column>
          <el-table-column
              label="订单时间"
              width="200">
            <template slot-scope="scope">
              <div>{{ scope.row.createTime }}</div>
            </template>
          </el-table-column>
          <el-table-column
              label="支付时间"
              width="200">
            <template slot-scope="scope">
              <div>{{ scope.row.payTime }}</div>
            </template>
          </el-table-column>
          <el-table-column
              prop="price"
              label="定金"
              width="100">
          </el-table-column>
          <el-table-column
              label="支付状态"
              min-width="80"
          >
            <template slot-scope="scope">
              <el-tag v-if="scope.row.status===1" type="success" size="mini">已支付</el-tag>
              <el-tag v-if="scope.row.status===2" type="info" size="mini">已退单</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200px">
            <template slot-scope="scope">
              <el-button type="danger" size="mini" icon="el-icon-minus" @click="changeOrder(scope.row)" :disabled="scope.row.status===2">取消订单
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </transition>
      <!--分页-->
      <div class="block2">
        <el-pagination
            background
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="currentPage"
            :page-sizes="[5, 10, 15, 20]"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="total">
        </el-pagination>
      </div>
    </div>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import {APP_CONFIG} from "@/config/app";

export default {
  name: "ProductOrderView",
  props: ['parentData'], // 接收父组件传递的数据
  //Vue数据属性
  data() {
    return {
      tableShow: true,

      condition: 0,

      conditionText: null,

      imgFileList: [],

      //分页表格数据
      tableData: [],

      //数据总条数
      total: 100,

      filePath1: APP_CONFIG.resourceUrls.landFile,

      filePath2: APP_CONFIG.resourceUrls.productFile,

      //当前页
      currentPage: 1,

      //每页显示条数
      pageSize: 5,

      // 查询框数据
      order: {
        id: null,
        pId: null,
        num: null,
        uId: null,
        createTime: null,
        payTime: null,
        status: null,
        img: null,
        price: null,
        del: null,
        username: null,
        phone: null
      },
      ids:[],
      // 修改的订单数据
      order2: {
        id: null,
        pId: null,
        num: null,
        uId: null,
        createTime: null,
        payTime: null,
        status: null,
        img: null,
        price: null,
        del: null,
        username: null,
        phone: null
      }
    }
  },
  watch: {
    // 监听父组件传递的数据变化
    parentData: {
      immediate: true, // 立即执行，保证在 mounted 钩子中能够获取到父组件传递的数据
      once: true, // 仅执行一次
      handler(newValue, oldValue) {
        // 在 props 变化时执行相应的逻辑
        this.selectAll();
        // 这里可以执行子组件需要的逻辑
      }
    }
  },
  mounted() {
  },
  //vue方法集合
  methods: {
    async orderDownload() {
      if (this.parentData.power===1){
        this.order.uId=this.parentData.id
      }else {
        this.order.uId=null
      }
      const formData = new FormData();
      formData.append('order', JSON.stringify(this.order));
      await axiosInstance.post('/order/download', formData, {
        responseType: 'blob', // 指定响应数据的类型为二进制流
      })
          .then(response => {
            const url = window.URL.createObjectURL(new Blob([response.data])); // 创建一个Blob对象
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', '云用地_产品订单.xlsx'); // 设置下载的文件名为提取到的文件名
            document.body.appendChild(link);
            link.click();
          })
          .catch(error => {
            console.error(error);
          });
    },
    //每页显示数据改变触发的事件
    handleSizeChange(val) {
      //当前页显示条数
      this.pageSize = val
      this.conditionChange(true)
    },

    //页数改变触发的事件
    handleCurrentChange(val) {
      //当前页
      this.currentPage = val
      this.conditionChange(true)
    },

    //查询所有
    async selectAll(msg) {
      if (this.parentData.power===1){
        this.order.uId=this.parentData.id
      }else {
        this.order.uId=null
      }
      const formData = new FormData();
      formData.append('order', JSON.stringify(this.order));
      formData.append('pageNum', this.currentPage);
      formData.append('pageSize', this.pageSize);
      const {data: res} = await axiosInstance.post('/order/page', formData)
      this.total = res.data.total
      //判断查询后的总页数是否小于当前页号,小于则更新当前页号后再查询.(递归)
      let a = Math.ceil((this.total) / this.pageSize)
      if (a < this.currentPage) {
        this.currentPage = a
        await this.selectAll()
        return
      } else if (msg === 'add' && this.currentPage < a) {
        this.currentPage = a
        await this.selectAll()
        return
      }
      this.tableData = res.data.records;
      if (msg !== 'status' || this.productStatus === 0 || this.productStatus === 1) {
        this.tableShow = false;
        this.$nextTick(() => {
          this.tableShow = true;
        });
      }
      for (let i = 0; i < this.tableData.length; i++) {
        // 使用正则表达式替换所有的'T'为空格
        this.tableData[i].createTime = this.tableData[i].createTime.replace(/T/g, ' ');
        this.tableData[i].payTime = this.tableData[i].payTime.replace(/T/g, ' ');
      }
      console.log(this.tableData)
    },

    conditionChange(change) {
      this.order = this.$options.data().order
      if (!change) {
        this.conditionText = null;
        this.order.id = null;
        this.selectAll()
        return;
      }
      switch (this.condition) {
        case 1:
          if (!isNaN(this.conditionText)) {
            this.order.id = this.conditionText;
          } else {
            this.$notify({
              title: '警告',
              message: '请输入数字',
              type: 'warning'
            });
            return;
          }
          break;
        default:
          break;
      }
      this.selectAll()
    },


    async changeOrder(order) {
      this.$confirm('确定要取消该订单?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.ids[0]=order.id
        const formData = new FormData();
        formData.append('ids', this.ids);
        formData.append('status', 2);
        formData.append('time', false);
        const {data: res} = await axiosInstance.put('/order', formData)
        if (res.code === 10003) {
          await this.selectAll()
        }
      }).catch(() => {
      });
    },

    //图集轮播图赋值
    Images(imgFiles) {
      this.ImgVisible = true
      this.dialogImages = imgFiles;
    },

    //监听单选框和多选框变化和赋值ids[]
    checkChange(val) {
      this.Selection = val
      this.ids = []
      if (val.length === 0) {
        return 0;
      } else {
        for (let i = 0; i < this.Selection.length; i++) {
          this.ids[i] = this.Selection[i].id
        }
      }
    },
  }
}
</script>

<style>
section {
  padding: 2rem 2rem;
}

/*查询条件文字样式*/
.el-form-item__label {
  font-weight: bolder;
}

/*展开更多内容的样式*/
.demo-table-expand label {
  font-weight: bolder;
}

.demo-table-expand .el-form-item {
  color: #969696;
  margin-left: 15px;
  margin-bottom: 0;
  width: 100%;
}

.el-carousel__item:nth-child(2n) {
  background-color: #99a9bf;
}

.el-carousel__item:nth-child(2n+1) {
  background-color: #d3dce6;
}

/* 进入和离开动画效果 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 1s; /* 定义过渡效果的时间和属性 */
}

.fade-enter, .fade-leave-to {
  opacity: 0; /* 进入和离开时的透明度变化 */
}

.block2{
  margin-left: 20px;
  margin-top: 10px;
}

.el-pagination.is-background .el-pager li:not(.disabled).active {
  background-color: #105147 !important;
  color: white !important;
}

.el-pagination.is-background .btn-next:hover, .el-pagination.is-background .btn-prev:hover, .el-pagination.is-background .el-pager li:hover {
  color: #105147 !important;
  background-color: white !important;
}
.el-button--primary {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-button--danger {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-button--primary.is-plain {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-button--danger.is-disabled, .el-button--danger.is-disabled:active, .el-button--danger.is-disabled:focus, .el-button--danger.is-disabled:hover {
  color: #FFF;
  background-color: #c8cccc;
  border-color: #c8cccc;
}
.el-button--primary:focus, .el-button--primary:hover {
  background-color: #105147;
  border-color: #105147;
}
.el-button--primary.is-plain:focus, .el-button--primary.is-plain:hover {
  background-color: #105147;
  border-color: #105147;
}
.el-button--danger.is-plain:focus, .el-button--danger.is-plain:hover {
  background-color: #105147;
  border-color: #105147;
}
.el-button--danger:focus, .el-button--danger:hover {
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
