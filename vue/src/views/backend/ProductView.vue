<template>
  <div class="productView">
    <el-container>
      <!--查询表单-->
      <div class="select">
        <el-form :inline="true" :model="product" class="demo-form-inline">
          <el-form-item label="产品状态">
            <el-select v-model="productStatus" placeholder="状态" @change="conditionChange(true)" style="width: 100px">
              <el-option label="全部" :value="null"></el-option>
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="查询条件">
            <el-select v-model="condition" placeholder="当前条件" @change="conditionChange(false)" style="width: 150px">
              <el-option label="全部" :value="0"></el-option>
              <el-option label="产品ID" :value="1"></el-option>
              <el-option label="产品名称" :value="2"></el-option>
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
                <el-form-item label="产品 ID ：" class="expandLabel">
                  <span v-cloak>{{ props.row.id }}</span>
                </el-form-item>
                <el-form-item label="产品名称 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.productName }}</span>
                </el-form-item>
                <el-form-item label="产品介绍 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.description }}</span>
                </el-form-item>
                <el-form-item label="价格 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.price }}</span>
                </el-form-item>
                <el-form-item label="产品状态 ：" class="expandLabel" >
                  <span v-cloak>{{ props.row.status }}</span>
                </el-form-item>
                <el-form-item label="余量 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.num }}</span>
                </el-form-item>
                <el-form-item label="产品直售联系人 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.customerAUsername }}</span>
                </el-form-item>
                <el-form-item label="产品直售联系人电话 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.customerAPhone }}</span>
                </el-form-item>
                <el-form-item label="排序权重 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.ordered }}</span>
                </el-form-item>
              </el-form>
            </template>
          </el-table-column>
          <!--商品信息部分-->
          <el-table-column
              label="产品 ID"
              prop="id"
              width="90%"
          >
          </el-table-column>
          <el-table-column
              label="产品封面"
              prop="bookName"
              width="100%"
          >
            <template slot-scope="scope">
              <div class="demo-image__placeholder">
                <el-image
                    style="width: 80px; height: 50px;"
                    :src="filePath+scope.row.img"></el-image>
              </div>
            </template>
          </el-table-column>
          <el-table-column
              label="产品名称"
              prop="productName"
          >
          </el-table-column>
          <el-table-column
              label="产品介绍"
              prop="description"
          >
          </el-table-column>
          <el-table-column
              label="价格"
              prop="price"
              width="120%"
          >
          </el-table-column>
          <el-table-column
              label="排序权重"
              prop="ordered"
          >
          </el-table-column>
          <el-table-column label="产品状态" width="100%" v-if="parentData.power===2">
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
          <el-table-column
              label="产品状态"
              prop="status"
              v-if="parentData.power===1"
          >
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
      <el-form :model="Dialog_product" :rules="rules" ref="ruleForm" label-width="100px" class="demo-ruleForm">
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="Dialog_product.productName"></el-input>
        </el-form-item>
        <el-form-item label="介绍" prop="age">
          <el-input v-model="Dialog_product.description"></el-input>
        </el-form-item>
        <el-form-item label="价格" prop="phone">
          <el-input v-model="Dialog_product.price"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="parentData.power===2">
          <el-select v-model="Dialog_product.status" placeholder="请选择状态">
            <el-option label="启用" :value="1"></el-option>
            <el-option label="禁用" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="ordered" v-if="parentData.power===2">
          <el-input-number v-model="Dialog_product.ordered" controls-position="right" :min="1"
                           :max="100"></el-input-number>
        </el-form-item>
        <el-form-item label="余量" prop="balance">
          <el-input v-model="Dialog_product.num"></el-input>
        </el-form-item>
        <el-form-item label="产品封面">
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
import {APP_CONFIG} from "@/config/app";

export default {
  props: ['parentData'], // 接收父组件传递的数据
  //Vue数据属性
  data() {
    return {
      tableShow: true,
      //添加表单验证规则
      rules: {
        productName: [
          {required: true, message: '请输入用户名', trigger: 'blur'},
        ],
        price: [
          {required: true, message: '请输入年龄', trigger: 'blur'},
          {pattern: /^\d{1,2}([.]\d{1,2})?$/, message: '请输入合理的价格(0~120)'},
        ],
        description: [
          {required: true, message: '请输入手机号', trigger: 'blur'},
        ],
        status: [
          {required: true, message: '请选择用地状态', trigger: 'blur'},
        ],
        img: [
          {required: true, message: '请上传头像', trigger: 'blur'},
        ],
      },

      condition: 0,

      conditionText: null,

      imgFileList: [],

      //分页表格数据
      tableData: [],

      //数据总条数
      total: 100,

      filePath: APP_CONFIG.resourceUrls.productFile,

      //当前页
      currentPage: 1,

      //每页显示条数
      pageSize: 5,

      //用于修改dialog的状态信息,设置按钮为编辑提交功能还是添加功能
      addOrUpdate: {
        title: '添加用地信息',
        update: false,
        bottomTitle: '添加'
      },

      //存储删除的id数组
      ids: [],

      //对话框显隐
      dialogVisible: false,
      // 查询框数据
      product: {
        id: null,
        productName: null,
        description: null,
        ordered: null,
        price: null,
        img: null,
        status: null,
        num: null,
        aId: null,
        customerAUsername: null,
        customerAPhone: null,
      },
      productStatus: null,
      //dialog对话框数据
      Dialog_product: {
        id: null,
        productName: null,
        description: null,
        ordered: 100,
        price: null,
        img: null,
        status: 0,
        num: null,
        aId: null
      },
    }
  },
  mounted() {
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
      if (this.parentData.power===1){
        this.product.aId=this.parentData.id
      }else{
        this.product.aId=null;
      }
      const formData = new FormData();
      formData.append('product', JSON.stringify(this.product));
      formData.append('pageNum', this.currentPage);
      formData.append('pageSize', this.pageSize);
      const {data: res} = await axiosInstance.post('/product/page', formData)
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
        if (res.data.records[i].status === 1) {
          res.data.records[i].status = "启用"
        } else {
          res.data.records[i].status = "禁用"
        }
      }
      this.tableData = res.data.records;
      if (msg !== 'status' || this.productStatus === 0 || this.productStatus === 1) {
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
        const {data: res} = await axiosInstance.delete('/product', {data: ids});
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
      this.product = this.$options.data().product
      this.product.status = this.productStatus
      if (!change) {
        this.conditionText = null;
        this.selectAll()
        return;
      }
      switch (this.condition) {
        case 1:
          if (!isNaN(this.conditionText)) {
            this.product.id = this.conditionText;
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
          this.product.productname = this.conditionText
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
        this.addOrUpdate.title = "添加产品信息";
        this.addOrUpdate.bottomTitle = "添加";
        this.addOrUpdate.update = false;
        //对话框关闭重置product数据
        this.Dialog_product = this.$options.data().Dialog_product;
        this.selectedOptions = [];
        this.$refs.img.clearFiles();
      }
    },

    //监听状态开关变化
    async StateChanged(row) {
      const {data: res} = await axiosInstance.get('/product/'+row.id);
      if (res.data.status === 0) {
        res.data.status = 1;
      } else {
        res.data.status = 0;
      }
      const formData = new FormData();
      formData.append('product', JSON.stringify(res.data));
      console.log(res.data)
      await axiosInstance.put('/product', formData);
      await this.selectAll('status')
    },

    //查询修改的内容,开启修改对话框
    async openUpdate(id) {
      this.addOrUpdate.update = true;
      //修改数据回显
      const {data: res} = await axiosInstance.get('/product/'+id);
      this.Dialog_product = res.data;
      //更改添加对话框为编辑对话框
      this.addOrUpdate.title = "编辑用地信息"
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
          message: '请上传产品图片',
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
          formData.append("productImg", file.raw);
        }
      }
      //向formData添加用地的基本信息
      this.$refs[formName].validate(async (valid) => {
        //判断表单验证结果
        if (valid) {
          //如果文件大小审核通过，开始上传文件到后端
          if (allowUp) {
            this.Dialog_product.aId=this.parentData.id
            formData.append('product', JSON.stringify(this.Dialog_product));
            if (this.addOrUpdate.update) {
              const {data: res} = await axiosInstance.put('/product', formData);
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
              const {data: res} = await axiosInstance.post('/product', formData);
              if (res.code === 10001) {
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

