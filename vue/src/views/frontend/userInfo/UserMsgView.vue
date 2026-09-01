<template>
  <div class="user-main-w">
    <div class="user-msg-title">我的资料</div>
    <div class="user-msg-w">
      <div class="msg-list">
        <div class="msg-list-item">
          <div class="user-front1">用户名:</div>
          <input type="text" class="user-front2" v-model="user.username">
        </div>
        <div class="msg-list-item">
          <div class="user-front1">年龄:</div>
          <input type="text" class="user-front2" v-model="user.age">
        </div>
        <div class="msg-list-item">
          <div class="user-front1">地址:</div>
          <el-cascader
              placeholder="请选择地址,可搜索"
              :options="options"
              v-model="selectedOptions"
              filterable class="box"></el-cascader>
        </div>
        <div class="msg-list-item">
          <div class="user-front1">详细地址:</div>
          <input type="text" class="user-front2" v-model="user.detailedAddress">
        </div>
        <div class="msg-list-item">
          <div class="user-front1">手机:</div>
          <div class="user-front2">{{ user.phone }}</div>
        </div>
        <div class="msg-list-item">
          <div class="user-front1">邮箱:</div>
          <div class="user-front2">{{ user.mail }}</div>
        </div>
      </div>
      <div class="msg-icon-w">
        <div class="msg-icon">
          <div class="msg-icon-title">头像:</div>
          <input type="file" ref="fileInput" style="display: none" @change="uploadImage">
          <img :src="imgPath" alt="" @click="selectImage"></div>
        <button type="button" class="link-btn2" @click="updateUser">保存</button>
      </div>
    </div>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";
import {CodeToText, regionData} from 'element-china-area-data'
import {APP_CONFIG} from "@/config/app";
import {createEmptyUser, fetchCurrentUser} from "@/utils/auth";

export default {
  name: "userMsgView",
  data() {
    return {
      userIcon: null,
      selectedOptions: [],
      options: regionData,
      filePath: APP_CONFIG.resourceUrls.userFile,
      imgPath: null,
      user: createEmptyUser()
    }
  },
  mounted() {
    this.openCheck();
  },
  methods: {
    async openCheck() {
      this.user = createEmptyUser()
      const {user} = await fetchCurrentUser(true)
      if (!user) {
        return
      }
      const addressString = user.address;
      if (addressString) {
        const addressArray = addressString.split(",");
        user.address = CodeToText[addressArray[0]] + "-" + CodeToText[addressArray[1]] + "-" + CodeToText[addressArray[2]]
        this.selectedOptions = addressString.split(",");
      }
      this.user = user
      this.imgPath = this.user.img ? this.filePath + this.user.img : null
    },
    async updateUser() {
      const formData = new FormData();
      const maxSize = 5 * 1024 * 1024;
      if (this.userIcon !== null && this.userIcon.size > maxSize) {
        this.$notify({
          title: '提示',
          message: `${this.userIcon.name} 文件大小不能超过 5MB`,
          type: 'warning'
        });
        this.imgPath = this.user.img ? this.filePath + this.user.img : null
        return
      }
      formData.append('userIcon', this.userIcon);

      if (this.checkFrom() === 'pass') {
        this.user.address = this.selectedOptions.join(",")
        formData.append('user', JSON.stringify(this.user));
        const config = {
          headers: {
            frond: null,
          }
        };
        const {data: res} = await axiosInstance.put('/user', formData, config);
        if (res.code === 10003) {
          this.$notify.info({
            title: '提示',
            message: '修改成功',
            type: 'success',
            duration: 1000
          });
          setTimeout(() => {
            location.reload()
          }, 1000);
        }
      }
    },
    selectImage() {
      this.$refs.fileInput.click();
    },
    uploadImage(e) {
      this.userIcon = e.target.files[0];
      this.imgPath = URL.createObjectURL(this.userIcon);
    },
    checkFrom() {
      if (this.user.username === null) {
        this.$notify.info({
          title: '系统提示',
          message: '请填写用户名',
          duration: 1000,
          showClose: false
        });
        return
      }
      const usernameRegex = /^\S{2,10}$/;
      if (!usernameRegex.test(this.user.username)) {
        this.$notify.info({
          title: '系统提示',
          message: '用户名格式不正确',
          duration: 1000,
          showClose: false
        });
        return
      }
      if (this.user.age === null) {
        this.$notify.info({
          title: '系统提示',
          message: '请填写年龄',
          duration: 1000,
          showClose: false
        });
        return
      }
      const ageRegex = /^(1[89]|[2-9][0-9])$/;
      if (!ageRegex.test(this.user.age)) {
        this.$notify.info({
          title: '系统提示',
          message: '年龄格式不正确',
          duration: 1000,
          showClose: false
        });
        return
      }
      if (this.selectedOptions.length === 0) {
        this.$notify.info({
          title: '系统提示',
          message: '请选择地址',
          duration: 1000,
          showClose: false
        });
        return
      }
      if (this.user.detailedAddress === null) {
        this.$notify.info({
          title: '系统提示',
          message: '请填写详细地址',
          duration: 1000,
          showClose: false
        });
        return
      }
      const addressRegex = /^\S{5,}$/;
      if (!addressRegex.test(this.user.detailedAddress)) {
        this.$notify.info({
          title: '系统提示',
          message: '详细地址至少 5 个字符',
          duration: 1000,
          showClose: false
        });
        return
      }
      return "pass";
    },
  }
}
</script>

<style scoped>
.user-main-w {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  gap: 30px;
}

.user-msg-title {
  color: #6c757d;
  font-size: 2rem;
  border-bottom: 2px solid var(--color-border);
  padding-bottom: 10px;
}

.user-msg-w {
  display: flex;
  gap: 100px;
}

.msg-list {
  display: flex;
  padding-top: 30px;
  flex-direction: column;
  height: 400px;
  width: 400px;
  justify-content: space-between;
  color: #6c757d;
  font-size: 1.3rem;
  font-weight: bolder;
  flex: 0 0 auto;
}

.msg-list-item {
  width: 100%;
  padding-left: 20px;
  padding-right: 20px;
  display: flex;
  height: 50px;
  border: 1px solid #bbbbbb;
  align-items: center;
}

.user-front1 {
  width: 80px;
}

.user-front2 {
  color: #6c757d;
  font-size: 1.3rem;
  font-weight: bolder;
  flex-grow: 1;
  width: 80px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.msg-icon {
  padding-bottom: 10px;
  display: flex;
  margin-top: 30px;
  width: 180px;
  height: 100px;
  gap: 50px;
  border-bottom: 2px solid var(--color-border);
}

.msg-list-item:hover {
  border: none;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.3);
}

.msg-icon-title {
  color: #6c757d;
  font-size: 1.3rem;
  font-weight: bolder;
  width: 50px;
}

.msg-icon > img {
  width: 80px;
  height: 80px;
  border-radius: 5px;
}

.msg-icon > img:hover {
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.3);
}

.link-btn2 {
  margin-top: 50px;
  width: 180px;
  padding: 1rem 3rem;
  display: inline-block;
  border: 0.1rem solid var(--color-primary);
  color: var(--color-primary);
  background: none;
  cursor: pointer;
  font-size: 1.7rem;
}

.link-btn2:hover {
  background: var(--color-primary);
  color: #fff;
}

/* 信息行内的级联选择器：边框由 .msg-list-item 提供，内层输入框去边框对齐 */
.msg-list-item ::v-deep .el-input__inner {
  margin-left: -14px;
  border: none !important;
}
</style>
