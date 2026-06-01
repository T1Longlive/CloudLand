<template>
  <div class="LandView">
    <el-container>
      <!--查询表单-->
      <div class="select">
        <el-form :inline="true" :model="Land" class="demo-form-inline">
          <el-form-item label="用地状态">
            <el-select v-model="LandStatus" placeholder="状态" @change="conditionChange(0)" style="width: 100px">
              <el-option label="全部" :value="null"></el-option>
              <el-option label="启用" :value="1"></el-option>
              <el-option label="禁用" :value="0"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="查询条件">
            <el-select v-model="condition" placeholder="当前条件" @change="conditionChange(1)" style="width: 150px">
              <el-option label="全部" :value="0"></el-option>
              <el-option label="地区" :value="1"></el-option>
              <el-option label="用地类型" :value="2"></el-option>
              <el-option label="用地标题" :value="3"></el-option>
              <el-option label="用地-ID" :value="4"></el-option>
              <el-option label="用地出租人-ID" :value="5"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="">
            <div class="block" v-show="condition===1">
              <el-cascader
                  placeholder="试试搜索：四川"
                  :options="options"
                  v-model="beforeAddress"
                  @change="conditionChange(0)"
                  filterable></el-cascader>
            </div>
            <el-select v-model="LandType" placeholder="请选择分类" v-show="condition===2" @change="conditionChange(0)">
              <el-option label="农用地" :value="1"></el-option>
              <el-option label="建设用地" :value="2"></el-option>
              <el-option label="商业用地" :value="3"></el-option>
              <el-option label="公共管理与公共服务用地" :value="4"></el-option>
              <el-option label="水域及水利设施用地" :value="5"></el-option>
              <el-option label="其他" :value="6"></el-option>
            </el-select>
            <el-tooltip class="item" effect="dark" content="请先选择查询条件" placement="bottom-start"
                        :disabled="condition!==0">
              <el-input v-model="conditionText" placeholder="输入查询内容" :disabled="condition===0"
                        v-show="condition!==1&&condition!==2" @keyup.native.enter="conditionChange(0)"></el-input>
            </el-tooltip>

          </el-form-item>

          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="conditionChange(0)">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" plain @click="dialogVisible = true;" size="mini"
                       icon="el-icon-plus">新增
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="danger" plain @click="deleteByIds(ids)" size="mini" icon="el-icon-minus">批量删除</el-button>
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
          <!--商品信息展开部分-->
          <el-table-column type="expand">
            <template slot-scope="props">
              <el-form label-position="left" inline class="demo-table-expand">
                <el-form-item label="用地 ID ：" class="expandLabel">
                  <span v-cloak>{{ props.row.id }}</span>
                </el-form-item>
                <el-form-item label="标题 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.landName }}</span>
                </el-form-item>
                <el-form-item label="用地联系人 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.customerAUsername + "-" + props.row.customerAPhone }}</span>
                </el-form-item>
                <el-form-item label="代理用地负责联系人 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.employeeUsername + "-" + props.row.employeePhone }}</span>
                </el-form-item>
                <el-form-item label="价格(元/平方米/天) ：" class="expandLabel">
                  <span v-cloak>{{ props.row.price }}</span>
                </el-form-item>
                <el-form-item label="可用面积 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.area }}</span>
                </el-form-item>
                <el-form-item label="地址 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.address }}</span>
                </el-form-item>
                <el-form-item label="详细地址 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.detailedAddress }}</span>
                </el-form-item>
                <el-form-item label="分类 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.typeName }}</span>
                </el-form-item>
                <el-form-item label="排序权重 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.ordered }}</span>
                </el-form-item>
                <el-form-item label="用地状态 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.status }}</span>
                </el-form-item>
                <el-form-item label="用地详细资料下载 ：" class="expandLabel">
                  <a :href="filePath+props.row.landFiles.path" v-cloak>{{ "点击下载" }}</a>
                </el-form-item>
                <el-form-item label="用地简介 ：" class="expandLabel">
                  <span v-cloak>{{ props.row.description }}</span>
                </el-form-item>
              </el-form>
            </template>
          </el-table-column>
          <!--商品信息部分-->
          <el-table-column
              label="用地 ID"
              prop="id"
              width="130%"
          >
          </el-table-column>
          <el-table-column
              label="用地图集"
              prop="bookName"
              width="130%"
          >
            <template slot-scope="scope">
              <div class="demo-image__placeholder">
                <div class="block" @click="Images(scope.row.imageFiles)">
                  <el-image
                      style="width: 80px; height: 53px;"
                      :src="filePath+scope.row.imageFiles[0].path " v-if="scope.row.imageFiles.length>0"></el-image>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
              label="标题"
              prop="landName">
          </el-table-column>
          <el-table-column
              label="类型"
              prop="typeName"
              width="130%"
          >
          </el-table-column>
          <el-table-column
              label="排序权重"
              prop="ordered"
              width="130%"
          >
          </el-table-column>
          <el-table-column label="当前状态" width="130%" v-if="parentData.power===2">
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
              <el-button type="primary" @click="openUpdate(scope.row.id).then()" size="mini" icon="el-icon-edit">编辑
              </el-button>
              <el-button type="danger" @click="deleteByIds(scope.row.id)" size="mini" icon="el-icon-minus">删除
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
      <el-form :model="land" :rules=rules ref="ruleForm" label-width="100px" class="demo-ruleForm">
        <el-form-item label="标题" prop="landName">
          <el-input v-model="land.landName"></el-input>
        </el-form-item>
        <el-form-item label="价格(元/平方米/天)" prop="price">
          <el-input v-model="land.price"></el-input>
        </el-form-item>
        <el-form-item label="面积(平方米)" prop="area">
          <el-input v-model="land.area"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" :rows="2" placeholder="请输入内容" v-model="land.description"></el-input>
        </el-form-item>
        <el-form-item label="用地分类" prop="landType">
          <el-select v-model="land.landType" placeholder="请选择分类">
            <el-option label="农用地" :value="1"></el-option>
            <el-option label="建设用地" :value="2"></el-option>
            <el-option label="商业用地" :value="3"></el-option>
            <el-option label="公共管理与公共服务用地" :value="4"></el-option>
            <el-option label="水域及水利设施用地" :value="5"></el-option>
            <el-option label="其他" :value="6"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="parentData.power===2">
          <el-select v-model="land.status" placeholder="请选择状态">
            <el-option label="启用" :value="1"></el-option>
            <el-option label="禁用" :value="0"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="ordered" v-if="parentData.power===2">
          <el-input-number v-model="land.ordered" controls-position="right" :min="1"
                           :max="100"></el-input-number>
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
          <el-input v-model="land.detailedAddress"></el-input>
        </el-form-item>
        <el-form-item label="代理人">
          <el-select v-model="land.employeeId" filterable placeholder="请选择">
            <el-option
                v-for="item in employeeOptions"
                :key="item.id"
                :label="item.username"
                :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="用地预览图">
          <el-upload
              ref="imageFile"
              class="upload-demo"
              :limit=12
              :auto-upload=false
              :accept="'image/jpeg,image/png,image/gif'"
              action="https://jsonplaceholder.typicode.com/posts/"
              :file-list="imgFileList"
              :on-change="ImgHandChange"
              :on-remove="ImgHandleRemove"
              list-type="picture"
              name="imageFiles"
          >
            <el-button size="small" type="primary" icon="el-icon-upload">上传图片</el-button>
            <div slot="tip" class="el-upload__tip">只能上传jpg/png/gif文件，(最多上传12张)</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="用地信息文件">
          <el-upload
              ref="landFile"
              class="upload-demo"
              :auto-upload=false
              :accept="'image/jpeg,image/png,image/gif,.doc,.docx,text/plain,.txt,application/pdf,.pdf'"
              action="https://jsonplaceholder.typicode.com/posts/"
              :limit=5
              :on-change="handChange"
              :on-remove="handleRemove"
              :file-list="fileList"
              name="landFiles"
          >
            <el-button size="small" type="primary" icon="el-icon-upload">上传文件</el-button>
            <div slot="tip" class="el-upload__tip">最多上传5个文件</div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitFiles('ruleForm')">{{ addOrUpdate.bottomTitle }}</el-button>
          <el-button type="info" @click="dialogVisible=false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
    <!--    图片预览图集-->
    <el-dialog
        title="用地详情图集"
        :visible.sync="ImgVisible"
        width="1000px"
    >
      <el-carousel :autoplay="false" type="card" trigger="click" height="250px">
        <el-carousel-item v-for="item in dialogImages" :key="item.id">
          <el-image
              style="width: 100%; height: 100%;border-radius: 5px"
              :src="filePath+item.path"></el-image>
        </el-carousel-item>
      </el-carousel>
    </el-dialog>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import {CodeToText, regionData} from 'element-china-area-data'
import {APP_CONFIG} from "@/config/app";

export default {
  name: "Land",
  props: ['parentData'], // 接收父组件传递的数据
  //Vue数据属性
  data() {
    return {
      tableShow: true,
      //添加表单验证规则
      rules: {
        landName: [
          {required: true, message: '请输入标题', trigger: 'blur'},
        ],
        price: [
          {required: true, message: '请输入价格', trigger: 'blur'},
          {pattern: /^\d{1,2}([.]\d{1,2})?$/, message: '请输入合理的价格(0~99.99)'},
        ],
        area: [
          {required: true, message: '请输入面积', trigger: 'blur'},
        ],
        description: [
          {required: true, message: '请输入用地描述', trigger: 'blur'},
        ],
        landType: [
          {required: true, message: '请选择用地类型', trigger: 'blur'},
        ],
        status: [
          {required: true, message: '请选择用地状态', trigger: 'blur'},
        ],
        address: [
          {required: true, message: '请选择地址', trigger: 'blur'},
        ],
        detailedAddress: [
          {required: true, message: '请输入详细地址信息', trigger: 'blur'},
        ],
        imgFileList: [
          {required: true, message: '请上传用地预览图', trigger: 'blur'},
        ],
        fileList: [
          {required: true, message: '请上传用地文件', trigger: 'blur'},
        ]
      },
      condition: 0,
      conditionText: null,
      employeeOptions: [],
      options: regionData,
      // selectedOptions: ['510000','510100','510121'],
      selectedOptions: [],
      beforeAddress: [],
      imgFileList: [],
      fileList: [],
      dialogImages: [],
      ImgVisible: false,
      //分页表格数据
      tableData: [],
      //数据总条数
      total: 100,
      //文件上传
      files: [],
      //图片上传
      imageFiles: [],
      //头像图片回显
      imageUrl: `${APP_CONFIG.resourceUrls.landFile}Land_1/Images/land1.jpg`,
      filePath: APP_CONFIG.resourceUrls.landFile,
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
        status: null,
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
      LandStatus: null,
      LandType: null,
      //dialog对话框数据
      land: {
        id: null,
        landName: null,
        landType: null,
        description: null,
        imageFiles: [],
        ordered: 100,
        landFiles: [],
        price: null,
        aId: 1,
        status: null,
        area: null,
        employeeId: 1,
        detailedAddress: null,
        typeName: null,
        typeDescription: null,
        customerAUsername: null,
        customerAPhone: null,
        employeeUsername: null,
        employeePhone: null,
        address: []
      },
      notification: null,
      admin: true,
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

    //查询所有
    async selectAll(msg) {
      if (this.parentData.power===1){
        this.Land.aId=this.parentData.id
      }else{
        this.Land.aId=null;
      }
      const formData = new FormData();
      formData.append('land', JSON.stringify(this.Land));
      formData.append('pageNum', this.currentPage);
      formData.append('pageSize', this.pageSize);
      const {data: res} = await axiosInstance.post('/land/page', formData)
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
      this.tableData = res.data.records
      console.log(this.tableData)
      if (msg !== 'status' || this.LandStatus === 0 || this.LandStatus === 1) {
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
        const {data: res} = await axiosInstance.delete('/land', {data: ids});
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
      this.Land = this.$options.data().Land
      this.Land.status = this.LandStatus
      if (change === 1) {
        this.beforeAddress = null;
        this.conditionText = null;
        this.LandType = null;
        this.selectAll()
        return;
      }
      if (this.condition === 1 && this.beforeAddress === null) {
        this.selectAll()
        return;
      }
      switch (this.condition) {
        case 1:
          this.Land.address = this.beforeAddress.join(",");
          this.Land.landType = null;
          this.conditionText = null;
          break;
        case 2:
          this.Land.landType = this.LandType;
          this.beforeAddress = null;
          this.conditionText = null;
          break;
        case 3:
          this.Land.landName = this.conditionText;
          break;
        case 4:
          if (!isNaN(this.conditionText)) {
            this.Land.id = this.conditionText;
          } else {
            this.$notify({
              title: '警告',
              message: '请输入数字',
              type: 'warning'
            });
            return;
          }
          break;
        case 5:
          if (!isNaN(this.conditionText)) {
            this.Land.aId = this.conditionText;
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
      if (dialogVisible) {
        //代理人信息查询
        const {data: res} = await axiosInstance.post('/user/employee');
        this.employeeOptions = res.data;
      } else {
        //对话框关闭事件
        //关闭修改的提示信息
        if (this.notification) {
          this.notification.close(); // 调用 close 方法关闭通知
        }
        //对话框和状态重置回添加功能
        this.addOrUpdate.title = "添加用地信息";
        this.addOrUpdate.bottomTitle = "添加";
        this.addOrUpdate.update = false;
        //对话框关闭重置land数据
        this.land = this.$options.data().land;
        this.selectedOptions = [];
        this.$refs.landFile.clearFiles();
        this.$refs.imageFile.clearFiles();
      }
    },

    //监听状态开关变化
    async StateChanged(row) {
      const {data: res1} = await axiosInstance.get('/land/' + row.id);
      this.land = res1.data;
      if (this.land.status === 0) {
        this.land.status = 1;
      } else {
        this.land.status = 0;
      }
      const formData = new FormData();
      formData.append('land', JSON.stringify(this.land));
      const {data: res2} = await axiosInstance.put('/land', formData);
      if (res2.code === 10002) {
        this.land = this.$options.data().land
      }
      await this.selectAll('status')
    },

    //查询修改的内容,开启修改对话框
    async openUpdate(id) {
      this.addOrUpdate.update = true;
      //修改数据回显
      const {data: res} = await axiosInstance.get('/land/' + id);
      this.land = res.data;
      const addressString = res.data.address;
      this.selectedOptions = addressString.split(",");
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
      if (this.imgFileList.length === 0 && this.fileList.length === 0 && !this.addOrUpdate.update) {
        this.$notify({
          title: '警告',
          message: '请上传用地预览图或用地文件',
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
          formData.append("imageFiles", file.raw);
        }
      }
      //向formData添加文件数据
      for (const file of this.fileList) {
        if (file.size > maxSize) {
          this.$notify({
            title: '警告',
            message: file.name + '文件大小超过限制（最大 5MB）',
            type: 'warning'
          });
          allowUp = false; // 拒绝上传
          return; // 中断循环
        } else {
          formData.append("landFiles", file.raw);
        }
      }
      //向formData添加用地的基本信息
      this.land.address = this.selectedOptions.join(",")
      this.$refs[formName].validate(async (valid) => {
        //判断表单验证结果
        if (valid) {
          //如果文件大小审核通过，开始上传文件到后端
          if (allowUp) {
            this.land.aId=this.parentData.id
            formData.append('land', JSON.stringify(this.land));
            if (this.addOrUpdate.update) {
              const {data: res} = await axiosInstance.put('/land', formData);
              if (res.code === 10003) {
                this.dialogVisible = false
                await this.selectAll();
              }
            } else {
              //发送添加请求
              const {data: res} = await axiosInstance.post('/land', formData);
              if (res.code === 10001) {
                this.dialogVisible = false
                //重新查询，更新信息
                await this.selectAll('add');
              }
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

.el-pagination.is-background .el-pager li:not(.disabled).active{
  background-color: #105147 !important;
  color: white !important;
}
.el-pagination.is-background .btn-next:hover, .el-pagination.is-background .btn-prev:hover, .el-pagination.is-background .el-pager li:hover{
  color: #105147 !important;
  background-color: white !important;
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
.el-textarea__inner:hover{
  border-color: #105147;
}
.el-textarea__inner:focus{
  border-color: #105147;
}
.el-message-box__headerbtn .el-message-box__close{
  color: #000000;
}
.el-dialog__headerbtn .el-dialog__close {
  color: #000000;
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
