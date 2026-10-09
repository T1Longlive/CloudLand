#!/usr/bin/env bash
# ======================================================================
# 把 Cloudland 需要的 4 个基础镜像转存到本项目 ACR
# ----------------------------------------------------------------------
# 【为什么需要】
#   云端构建环境不保证能访问 Docker Hub（Gitee 时代的云端构建完全访问不了；
#   原本 Dockerfile 依赖的第三方代理 docker.1panel.live 也已失效）。
#   把基础镜像转存到自己的 ACR 后，云端即可拉取 ——
#   而 ACR 的可达性是已确定的（我们本来就在往它推镜像）。
#   顺带好处：基础镜像来源固定，供应链可控。
#
# 【在哪执行】⚠️ 2026-09-24 实测：**服务器已经无法访问 Docker Hub**，
#   本脚本在服务器上会失败。pm-system 已改为在 GitHub Actions runner 里转存
#   （见其 .github/workflows/refresh-base-images.yml，runner 能正常访问 Docker Hub）。
#   Cloudland 要刷新基础镜像应照做；本脚本保留，供网络可达时手动使用。
#
# 【什么时候执行】基础镜像版本变化时重跑。日常部署不需要。
#
# 【前置】
#   docker login crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com
#   且 ACR 中已建好命名空间 t1longlive
#
# 【特别说明：openjdk:8-jre-slim】
#   该镜像来自已停止维护的 Docker Hub「openjdk」官方仓库，**很可能拉不到**。
#   但服务器本地已有它（Cloudland 正在用），所以脚本的策略是：
#     本地已有 → 直接 tag + push（不拉取）
#     本地没有 → 才尝试 docker pull
#   并且会兼容「本地只存了 1panel 代理名」的情况。
# ======================================================================
set -Eeuo pipefail

ACR="crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com"
NS="t1longlive"
FALLBACK_PREFIX="docker.1panel.live/library/"

# 目标镜像名（转存后保持同名同 tag，Dockerfile 里只是多一段仓库前缀）
IMAGES=(
  "maven:3.8.6-openjdk-8"     # 后端构建阶段
  "openjdk:8-jre-slim"        # 后端运行阶段（Docker Hub 已下线，靠本地）
  "node:16-alpine"            # 前端构建阶段
  "nginx:alpine"              # 前端运行阶段（ACR 中可能已存在）
)

echo ">> 目标仓库: ${ACR}/${NS}"
echo ">> 若下方 push 报无权限，请先执行: docker login ${ACR}"
echo

fail=0
for img in "${IMAGES[@]}"; do
  target="${ACR}/${NS}/${img}"
  src="$img"

  echo "--------------------------------------------------"
  echo ">> ${img}"

  if docker image inspect "$img" >/dev/null 2>&1; then
    echo "   本地已有，跳过 pull"
  elif docker image inspect "${FALLBACK_PREFIX}${img}" >/dev/null 2>&1; then
    src="${FALLBACK_PREFIX}${img}"
    echo "   本地命中（1panel 代理名）: ${src}"
  else
    echo "   本地没有，尝试 pull ..."
    if ! docker pull "$img"; then
      echo "   ✗ pull 失败（该镜像可能已从 Docker Hub 下线）"
      fail=1
      echo
      continue
    fi
  fi

  echo "   -> ${target}"
  docker tag "$src" "$target"
  if docker push "$target"; then
    echo "   ✓ 完成"
  else
    echo "   ✗ push 失败"
    fail=1
  fi
  echo
done

echo "=================================================="
if [ "$fail" -eq 0 ]; then
  echo "✓ 全部基础镜像已转存到 ACR"
  echo
  echo "下一步：两个 Dockerfile 的 FROM 已指向 ${ACR}/${NS}/...，"
  echo "        可以重新运行流水线了（GitHub Actions: build-deploy.yml）。"
else
  echo "✗ 有镜像处理失败，请按上面提示排查后重跑本脚本"
  exit 1
fi
