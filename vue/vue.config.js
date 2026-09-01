const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  parallel: false,
  productionSourceMap: false,
  transpileDependencies: true,
  devServer: {
    // historyApiFallback: true,
    // allowedHosts: "all",
    port: 8080, // 设置端口号
    host: 'localhost', // 设置主机名
  }
})
