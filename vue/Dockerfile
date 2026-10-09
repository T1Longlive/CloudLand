# ======================================================================
# Cloudland 前端镜像（多阶段构建）
# ----------------------------------------------------------------------
# 🔴 基础镜像已转存到本项目 ACR —— 云端构建不保证能访问 Docker Hub，
#    第三方镜像代理也不可靠。见 scripts/mirror-base-images.sh
#
# 🔴 COPY 路径为什么带 vue/ 前缀：
#    构建上下文固定为「代码库根目录」（CI 里是 `docker build -f vue/Dockerfile .`）。
#    因此 COPY 路径必须相对仓库根书写。
#    注意：这样写之后，本地 `docker build vue/` 的方式不再适用 ——
#    本地构建请用 `docker build -f vue/Dockerfile .`（上下文为仓库根）。
# ======================================================================

FROM crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com/t1longlive/node:16-alpine AS build
WORKDIR /app
COPY vue/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY vue/ ./
RUN npm run build

FROM crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com/t1longlive/nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
