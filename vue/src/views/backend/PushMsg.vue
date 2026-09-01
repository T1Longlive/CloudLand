<template>
  <div class="productView">
    <el-container>
      <!--查询表单-->
      <div class="select">
        <el-form :inline="true" :model="Msg" class="demo-form-inline">
          <el-form-item label="查询条件">
            <el-select v-model="condition" placeholder="当前条件" @change="conditionChange(false)" style="width: 150px">
              <el-option label="全部" :value="0"></el-option>
              <el-option label="留言编号" :value="1"></el-option>
              <el-option label="姓名" :value="2"></el-option>
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
            <el-button type="primary" icon="el-icon-message" @click="dialogVisible=true">推送信息</el-button>
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
          <!--信息展开部分-->
          <el-table-column type="expand">
            <template slot-scope="props">
              <el-form label-position="left" inline class="demo-table-expand">
                <el-form-item label="留言编号 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.id }}</span>
                </el-form-item>
                <el-form-item label="姓名 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.name }}</span>
                </el-form-item>
                <el-form-item label="留言信息 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.msg }}</span>
                </el-form-item>
                <el-form-item label="联系电话 ：" class="expandLabel">
                  <el-tag>{{ props.row.phone }}</el-tag>
                </el-form-item>
                <el-form-item label="邮箱 ：" class="expandLabel">
                  <el-tag>{{ props.row.mail }}</el-tag>
                </el-form-item>
                <el-form-item label="留言时间 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.sendTime }}</span>
                </el-form-item>
              </el-form>
            </template>
          </el-table-column>
          <el-table-column
              prop="id"
              label="留言编号"
              width="100px">
          </el-table-column>
          <el-table-column
              prop="msg"
              label="留言信息">
          </el-table-column>
          <el-table-column
              prop="name"
              label="姓名">
          </el-table-column>
          <el-table-column
              prop="mail"
              label="邮箱">
          </el-table-column>
          <el-table-column
              prop="phone"
              label="手机号">
          </el-table-column>
          <el-table-column
              label="留言时间"
              width="200">
            <template slot-scope="scope">
              <div>{{ scope.row.sendTime }}</div>
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
    <el-dialog
        :title="'云用地推送信息编辑'"
        :visible.sync="dialogVisible"
        width="50%"
        @open="pushMsg=null"
        @close="pushMsg=null">
      <el-form label-width="150px" class="demo-ruleForm">
        <el-form-item label="请填写推送信息" prop="description">
          <el-input type="textarea" :rows="2" placeholder="请输入内容" v-model="pushMsg"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="push()">推送</el-button>
          <el-button type="info" @click="dialogVisible=false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import {APP_CONFIG} from "@/config/app";

export default {
  name: "OrderView",
  props: ['parentData'], // 接收父组件传递的数据
  //Vue数据属性
  data() {
    return {
      tableShow: true,

      dialogVisible: false,

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
      Msg: {
        id: null,
        name: null,
        mail: null,
        phone: null,
        msg: null,
        sendTime: null,
      },
      pushMsg: null
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
    async push() {
      // 打开全屏加载状态
      this.openFullScreen();

      const formData = new FormData();
      formData.append('pushMsg', JSON.stringify(this.pushMsg));
      let pushRes
      try {
        const {data: res} = await axiosInstance.post('/msg/push', formData);
        pushRes=res
        this.dialogVisible = false;
      }finally {
        // 无论请求成功或失败都要关闭加载状态
        this.closeFullScreen(pushRes);
      }
    },

    openFullScreen() {
      this.loading = this.$loading({
        lock: true,
        text: '正在发送...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      });
    },

    closeFullScreen(res) {
      // 如果 loading 存在，则关闭加载状态
      if (this.loading) {
        this.loading.close();
      }
      if (res.code === 30001) {
        this.$notify.info({
          title: '系统提示',
          message: res.msg + '!',
          duration: 2000,
          showClose: false
        });
      }
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
      const formData = new FormData();
      formData.append('msg', JSON.stringify(this.Msg));
      formData.append('pageNum', this.currentPage);
      formData.append('pageSize', this.pageSize);
      const {data: res} = await axiosInstance.post('/msg/page', formData)
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
        this.tableData[i].sendTime = this.tableData[i].sendTime.replace(/T/g, ' ');
      }
      console.log(this.tableData)
    },

    conditionChange(change) {
      this.Msg = this.$options.data().Msg
      if (!change) {
        this.conditionText = null;
        this.Msg.id = null;
        this.selectAll()
        return;
      }
      switch (this.condition) {
        case 1:
          if (!isNaN(this.conditionText)) {
            this.Msg.id = this.conditionText;
          } else {
            this.$notify({
              title: '警告',
              message: '请输入数字',
              type: 'warning'
            });
            return;
          }
          break;
        case 2:
          this.Msg.name = this.conditionText;
          break;
        default:
          break;
      }
      this.selectAll()
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

