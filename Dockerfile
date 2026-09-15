# ======================================================================
# Cloudland 后端镜像（多阶段构建）
# ----------------------------------------------------------------------
# 🔴 基础镜像为什么不用 docker.1panel.live / docker.io：
#    Gitee 云端构建（build@docker / buildkit）**无法访问 Docker Hub**，
#    也不能保证能访问第三方镜像代理。
#    这些基础镜像已转存到本项目自己的 ACR，既保证云端可拉取，
#    也把基础镜像来源固定下来（供应链可控）。
#    转存命令见 scripts/mirror-base-images.sh
# ======================================================================

FROM crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com/t1longlive/maven:3.8.6-openjdk-8 AS build
WORKDIR /app
COPY pom.xml .
RUN mkdir -p /root/.m2 && echo '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>*</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml \
    && mvn dependency:go-offline -q
COPY src ./src
# 编译打包并**运行单元测试**：
#   测试失败 → 本层失败 → 镜像构建失败 → 部署被流水线的 dependsOn 挡住。
#   也就是说"测试门禁"直接复用已有的构建门禁，不需要新增流水线步骤。
#   （原命令带 -DskipTests，而 pom 里连测试依赖都没有 —— 门禁是完全关着的。）
#
# 🔴 前提：测试必须**不依赖 Spring 上下文 / 真实 MySQL / Redis**，
#    否则云端构建机会失败。当前测试集合（均无 Spring、无数据库）：
#      · JwtUtilsTest —— JWT 签发/过期/防篡改/防伪造
#      · CodeTest     —— 业务状态码分类的不变量（防止加了常量忘了归类）
#      · ResultTest   —— 成功/失败语义与 Code 的一致性
#
# 注意去掉了 -q：它会连 `Tests run:` 一起压掉（-q 只留 WARN/ERROR），
# 那样"测试到底跑没跑"就看不出来了。构建日志长一点，换可见性值得。
RUN mvn clean package

FROM crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com/t1longlive/openjdk:8-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
