<template>
  <div class="userView">
    <el-container>
      <!--查询表单-->
      <div class="select">
        <el-form :inline="true" :model="user" class="demo-form-inline">
          <el-form-item label="账号状态">
            <el-select v-model="userStatus" placeholder="状态" @change="conditionChange(true)" style="width: 100px">
              <el-option label="全部" :value="null"></el-option>
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="查询条件">
            <el-select v-model="condition" placeholder="当前条件" @change="conditionChange(false)" style="width: 150px">
              <el-option label="全部" :value="0"></el-option>
              <el-option label="用户ID" :value="1"></el-option>
              <el-option label="用户名" :value="2"></el-option>
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
            <el-button type="primary" plain size="mini"
                       icon="el-icon-plus" @click="dialogVisible=true">新增
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="danger" plain size="mini" icon="el-icon-minus" @click="deleteByIds(ids)">批量删除</el-button>
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
          <!--   @select="checkChange"                 -->
          <!-- height="560"-->
          <!--选项框部分-->
          <el-table-column
              type="selection"
              width="55"
          >
          </el-table-column>
          <!--用户信息展开部分-->
          <el-table-column type="expand">
            <template slot-scope="props">
              <el-form label-position="left" inline class="demo-table-expand">
                <el-form-item label="用户 ID ：" class="expandLabel">
                  <span v-cloak>{{ props.row.id }}</span>
                </el-form-item>
                <el-form-item label="用户名 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.username }}</span>
                </el-form-item>
                <el-form-item label="年龄 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.age }}</span>
                </el-form-item>
                <el-form-item label="地址 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.address }}</span>
                </el-form-item>
                <el-form-item label="手机号 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.phone }}</span>
                </el-form-item>
                <el-form-item label="邮箱" class="expandLabel">
                  <span v-cloak>{{ props.row.phone }}</span>
                </el-form-item>
                <el-form-item label="余额 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.debt }} 元</span>
                </el-form-item>
                <el-form-item label="详细地址 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.detailedAddress }}</span>
                </el-form-item>
                <el-form-item label="账号状态 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.status }}</span>
                </el-form-item>
              </el-form>
            </template>
          </el-table-column>
          <!--商品信息部分-->
          <el-table-column
              label="用户 ID"
              prop="id"
              width="90%"
          >
          </el-table-column>
          <el-table-column
              label="头像"
              prop="bookName"
              width="100%"
          >
            <template slot-scope="scope">
              <div class="demo-image__placeholder">
                <el-image
                    style="width: 53px; height: 53px;border-radius: 5px"
                    :src="filePath+scope.row.img"></el-image>
              </div>
            </template>
          </el-table-column>
          <el-table-column
              label="用户名"
              prop="username"
          >
          </el-table-column>
          <el-table-column
              label="年龄"
              prop="age"
              width="70%"
          >
          </el-table-column>
          <el-table-column
              label="手机号"
              prop="phone"
              width="120%"
          >
          </el-table-column>
          <el-table-column
              label="地址"
              prop="address"
          >
          </el-table-column>
          <el-table-column label="账号状态" width="100%">
            <template slot-scope="scope">
              <el-tooltip :content="'状态为: ' + scope.row.status" placement="top">
                <el-switch
                    v-model="scope.row.status"
                    active-value="启用"
                    inactive-value="禁用"
                    active-color="#186e61"
                    @change="StateChanged(scope.row)"
                >
                </el-switch>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="176px">
            <template slot-scope="scope">
              <el-button type="primary" size="mini" icon="el-icon-edit" @click="openUpdate(scope.row.id).then()">编辑
              </el-button>
              <el-button type="danger" size="mini" icon="el-icon-minus" @click="deleteByIds(scope.row.id)">删除
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
    <!--    添加和修改两用对话框-->
    <el-dialog
        :title="addOrUpdate.title"
        :visible.sync="dialogVisible"
        width="50%"
        @open="resetForm(dialogVisible)"
        @close="resetForm(dialogVisible)">
      <el-form :model="Dialog_user" :rules="rules" ref="ruleForm" label-width="100px" class="demo-ruleForm">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="Dialog_user.username"></el-input>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input v-model="Dialog_user.age"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="Dialog_user.password"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="Dialog_user.phone"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="mail">
          <el-input v-model="Dialog_user.mail"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="Dialog_user.status" placeholder="请选择状态">
            <el-option label="启用" :value="1"></el-option>
            <el-option label="禁用" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="权限" prop="status" v-show="addOrUpdate.update">
          <el-select v-model="Dialog_user.power" placeholder="请选择权限">
            <el-option label="普通用户" :value="0"></el-option>
            <el-option label="加盟用户" :value="1"></el-option>
            <el-option label="平台人员" :value="2"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <div class="block">
            <el-cascader
                placeholder="试试搜索：四川"
                :options="options"
                v-model="selectedOptions"
                filterable></el-cascader>
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailedAddress">
          <el-input v-model="Dialog_user.detailedAddress"></el-input>
        </el-form-item>
        <el-form-item label="余额" prop="balance">
          <el-input v-model="Dialog_user.debt"></el-input>
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
              ref="img"
              class="upload-demo"
              :limit=1
              :auto-upload=false
              :accept="'image/jpeg,image/png,image/gif'"
              action="https://jsonplaceholder.typicode.com/posts/"
              :file-list="imgFileList"
              :on-change="ImgHandChange"
              :on-remove="ImgHandleRemove"
              list-type="picture"
              name="img"
          >
            <el-button size="small" type="primary" icon="el-icon-upload">上传图片</el-button>
            <div slot="tip" class="el-upload__tip">只能上传jpg/png/gif文件</div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitFiles('ruleForm')">{{ addOrUpdate.bottomTitle }}</el-button>
          <el-button type="info" @click="dialogVisible=false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import {CodeToText, regionData} from 'element-china-area-data'
import {APP_CONFIG} from "@/config/app";

export default {
  //Vue数据属性
  data() {
    return {
      tableShow: true,
      //添加表单验证规则
      rules: {
        username: [
          {required: true, message: '请输入用户名', trigger: 'blur'},
        ],
        age: [
          {required: true, message: '请输入年龄', trigger: 'blur'},
          {pattern: /^\d{1,2}([.]\d{1,2})?$/, message: '请输入合理的价格(0~120)'},
        ],
        password: [
          {required: true, message: '请输入密码', trigger: 'blur'},
        ],
        phone: [
          {required: true, message: '请输入手机号', trigger: 'blur'},
        ],
        mail: [
          {required: true, message: '请输入邮箱', trigger: 'blur'},
        ],
        status: [
          {required: true, message: '请选择用户状态', trigger: 'blur'},
        ],
        address: [
          {required: true, message: '请选择地址', trigger: 'blur'},
        ],
        detailedAddress: [
          {required: true, message: '请输入详细地址信息', trigger: 'blur'},
        ],
        img: [
          {required: true, message: '请上传头像', trigger: 'blur'},
        ],
      },

      condition: 0,

      conditionText: null,

      options: regionData,

      selectedOptions: [],

      imgFileList: [],

      //分页表格数据
      tableData: [],

      //数据总条数
      total: 100,

      filePath: APP_CONFIG.resourceUrls.userFile,
      // filePath: 'http://192.168.43.206/:9090/resource/userFile/',

      //当前页
      currentPage: 1,

      //每页显示条数
      pageSize: 5,

      //用于修改dialog的状态信息,设置按钮为编辑提交功能还是添加功能
      addOrUpdate: {
        title: '添加用户信息',
        update: false,
        bottomTitle: '添加'
      },

      //存储删除的id数组
      ids: [],

      //对话框显隐
      dialogVisible: false,
      // 查询框数据
      user: {
        id: null,
        username: null,
        password: null,
        phone: null,
        age: null,
        address: null,
        img: null,
        status: null,
        detailedAddress: null,
        power: 1,
        debt: null,
        mail: null
      },
      userStatus: null,
      //dialog对话框数据
      Dialog_user: {
        id: null,
        username: null,
        password: null,
        phone: null,
        age: null,
        address: null,
        img: null,
        status: null,
        detailedAddress: null,
        power: 1,
        debt: 0,
        mail: null
      },
    }
  },
  mounted() {
    this.selectAll();
  },
  //vue方法集合
  methods: {
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
      formData.append('user', JSON.stringify(this.user));
      formData.append('pageNum', this.currentPage);
      formData.append('pageSize', this.pageSize);
      const {data: res} = await axiosInstance.post('/user/page', formData)
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
      this.tableData = res.data.records;
      if (msg !== 'status' || this.userStatus === 0 || this.userStatus === 1) {
        this.tableShow = false;
        this.$nextTick(() => {
          this.tableShow = true;
        });
      }
    },

    //删除功能+批量删除
    deleteByIds(id) {
      if (id.length === 0) {
        this.$notify({
          title: '提示',
          message: '请选择要删除的数据！',
          type: 'warning',
          duration: 2000
        });
        return
      }
      this.$confirm('此操作将永久删除该数据, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        //用户确认删除,开始执行删除
        let ids = []
        //判断传过来的参数是否是数组,true表示批量删除,false表示单个删除
        if (Array.isArray(id)) {
          ids = id
        } else {
          ids[0] = id
        }
        //delete请求携带的url和config,要带数据需要用config里面的data属性
        const {data: res} = await axiosInstance.delete('/user', {data: ids});
        if (res.code === 10002) {
          this.$notify({
            title: '提示',
            message: '删除成功',
            type: 'success',
            duration: 1000
          });
          //使用分页监听事件方法重新渲染页面
          await this.selectAll();
        } else {
          this.$message.error('删除失败!');
        }
      }).catch(() => {
      });
    },

    conditionChange(change) {
      this.user = this.$options.data().user
      this.user.status = this.userStatus
      if (!change) {
        this.conditionText = null;
        this.selectAll()
        return;
      }
      switch (this.condition) {
        case 1:
          if (!isNaN(this.conditionText)) {
            this.user.id = this.conditionText;
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
          this.user.username = this.conditionText
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

    //对话框开启关闭触发的事件
    async resetForm(dialogVisible) {
      if (!dialogVisible) {
        //对话框和状态重置回添加功能
        this.addOrUpdate.title = "添加用户信息";
        this.addOrUpdate.bottomTitle = "添加";
        this.addOrUpdate.update = false;
        //对话框关闭重置user数据
        this.Dialog_user = this.$options.data().Dialog_user;
        this.selectedOptions = [];
        this.$refs.img.clearFiles();
      }
    },

    //监听状态开关变化
    async StateChanged(row) {
      const {data: res} = await axiosInstance.post('/user/id', JSON.stringify(row.id), {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      if (res.data.status === 0) {
        res.data.status = 1;
      } else {
        res.data.status = 0;
      }
      const formData = new FormData();
      formData.append('user', JSON.stringify(res.data));
      await axiosInstance.put('/user', formData);
      await this.selectAll('status')
    },

    //查询修改的内容,开启修改对话框
    async openUpdate(id) {
      this.addOrUpdate.update = true;
      //修改数据回显
      const {data: res} = await axiosInstance.post('/user/id', JSON.stringify(id), {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      this.Dialog_user = res.data;
      const addressString = res.data.address;
      this.selectedOptions = addressString.split(",");
      //更改添加对话框为编辑对话框
      this.addOrUpdate.title = "编辑用户信息"
      this.addOrUpdate.bottomTitle = "提交"
      //显示对话框
      this.dialogVisible = true;
    },

    //提交信息
    async submitFiles(formName) {
      let allowUp = true;
      const maxSize = 5 * 1024 * 1024; // 5MB 的大小限制
      const formData = new FormData();
      if (this.imgFileList.length === 0 && !this.addOrUpdate.update) {
        this.$notify({
          title: '警告',
          message: '请上传头像',
          type: 'warning'
        });
        return
      }
      //向formData添加图片文件数据
      for (const file of this.imgFileList) {
        if (file.size > maxSize) {
          this.$notify({
            title: '警告',
            message: file.name + '文件大小超过限制（最大 5MB）',
            type: 'warning'
          });
          allowUp = false; // 拒绝上传
          return; // 中断循环
        } else {
          formData.append("userIcon", file.raw);
        }
      }
      //向formData添加用户的基本信息
      this.Dialog_user.address = this.selectedOptions.join(",")

      if (this.addOrUpdate.update) {
        delete this.rules.password; // 从规则中移除密码字段的验证规则
      }

      this.$refs[formName].validate(async (valid) => {
        //判断表单验证结果
        if (valid) {
          //如果文件大小审核通过，开始上传文件到后端
          if (allowUp) {
            formData.append('user', JSON.stringify(this.Dialog_user));
            if (this.addOrUpdate.update) {
              const {data: res} = await axiosInstance.put('/user', formData);
              if (res.code === 10003) {
                this.dialogVisible = false
              } else {
                this.$notify.info({
                  title: '修改提示',
                  message: res.msg + '!',
                  duration: 1000,
                  showClose: false,
                });
                return
              }
              //重新查询，更新信息
              await this.selectAll();
            } else {
              //发送添加请求
              const {data: res} = await axiosInstance.post('/user/register', formData);
              if (res.code === 20006) {
                this.dialogVisible = false
              } else {
                this.$notify.info({
                  title: '系统提示',
                  message: res.msg + '!',
                  duration: 1000,
                  showClose: false,
                });
                return
              }
              //重新查询，更新信息
              await this.selectAll('add');
            }
          }
        } else {
          return false;
        }
      });
    },

    //图片文件上传后赋值
    ImgHandChange(file, fileList) {
      this.imgFileList = fileList
    },
    //图片文件移除后赋值
    ImgHandleRemove(file, fileList) {
      this.imgFileList = fileList
    },
    //文件上传后赋值
    handChange(file, fileList) {
      this.fileList = fileList
    },
    //文件移除后赋值
    handleRemove(file, fileList) {
      this.fileList = fileList
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

/*选择框颜色*/
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
.el-pagination.is-background .el-pager li:not(.disabled).active{
  background-color: #105147 !important;
  color: white !important;
}
.el-pagination.is-background .btn-next:hover, .el-pagination.is-background .btn-prev:hover, .el-pagination.is-background .el-pager li:hover{
  color: #105147 !important;
  background-color: white !important;
}

.el-input__inner {
  font-size: 1.3rem !important;
  font-weight: bolder !important;
}

.el-cascader-node.in-active-path, .el-cascader-node.is-active, .el-cascader-node.is-selectable.in-checked-path {
  color: #0c5460 !important;
}
.el-select-dropdown__item.selected {
  color: #0c5460 !important;
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

.el-button--danger.is-plain {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-range-editor.is-active, .el-range-editor.is-active:hover, .el-select .el-input.is-focus .el-input__inner {
  border-color: #105147;
}
.el-select .el-input__inner:focus {
  border-color: #105147;
}
.el-input.is-active .el-input__inner, .el-input__inner:focus {
  border-color: #105147;
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
.el-dialog__headerbtn .el-dialog__close:hover {
  font-size: larger;
  color: #000000;
}
.el-cascader .el-input .el-input__inner:focus, .el-cascader .el-input.is-focus .el-input__inner {
  border-color: #105147;
}
.block2{
  margin-left: 20px;
  margin-top: 10px;
}
</style>
