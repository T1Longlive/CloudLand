<template>
  <div class="bg-img">
    <div class="text-overlay">
      <p>您好，欢迎使用云用地后台系统！</p>
    </div>
  </div>
</template>

<script>
export default {
  name: "home"
}
</script>


<style>
.bg-img {
  position: relative; /* 使伪元素相对于此容器定位 */
  width: 100%; /* 宽度占满父容器 */
  height: 100%; /* 高度占满父容器 */
  background-image: url("../../assets/images/home-bg.jpg");
  background-size: 100% 100%; /* 让背景图片自适应填满整个 <div> */
  background-repeat: no-repeat; /* 防止图片平铺 */
  background-position: center center; /* 图片居中对齐 */
  overflow: hidden; /* 隐藏超出容器的部分 */
}

.bg-img::before {
  content: ""; /* 必须设置内容为空，以激活伪元素 */
  position: absolute; /* 相对于 .background-div 绝对定位 */
  top: 0;
  left: 0;
  width: 100%; /* 宽度占满 .background-div */
  height: 100%; /* 高度占满 .background-div */
  background-color: rgba(0, 0, 0, 0.65); /* 半透明的黑色背景 */
  z-index: 1; /* 确保遮罩层在图片之上 */
  pointer-events: none; /* 允许点击透过遮罩层 */
}

.text-overlay {
  position: absolute; /* 相对于 .background-div 绝对定位 */
  top: 0;
  left: 0;
  width: 100%; /* 宽度占满 .background-div */
  height: 100%; /* 高度占满 .background-div */
  z-index: 2; /* 确保文本在遮罩层之上 */
  pointer-events: none; /* 防止文本影响点击事件 */
  display: flex;
  justify-content: center;
  align-items: center;
  color: white; /* 文本颜色 */
  text-align: center; /* 文本水平居中 */
  background-color: transparent; /* 透明背景，不受遮罩影响 */
  font-weight: bolder;
  font-size: 40px; /* 初始字体大小为 0 */
  animation-name: fadeIn; /* 使用动画名称 */
  animation-duration: 0.5s; /* 设置动画持续时间 */
  animation-timing-function: linear; /* 使用线性时间函数 */
  animation-fill-mode: forwards; /* 动画结束后保持最终状态 */
}

@keyframes fadeIn {
  0% {
    font-size: 0; /* 初始字体大小为 0 */
  }
  100% {
    font-size: 4rem; /* 最终字体大小，根据需要调整 */
  }
}

.el-button--primary {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-button--primary.is-plain {
  background-color: #ffffff;
  border-color: #000000;
  color: black;
}

.el-button--primary:focus, .el-button--primary:hover {
  background-color: #105147;
  border-color: #105147;
}
.el-button--primary.is-plain:focus, .el-button--primary.is-plain:hover {
  background-color: #105147;
  border-color: #105147;
}


.el-button:hover{
  background: #105147;
  border-color: #105147;
  color: #FFF;
}
.el-message-box__headerbtn .el-message-box__close:hover{
  color: #000000;
}
.el-message-box__headerbtn .el-message-box__close{
  color: #000000;
}
</style>