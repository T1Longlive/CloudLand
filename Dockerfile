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
RUN mvn clean package -DskipTests -q

FROM crpi-jzqyelh9p5zb0dmu.cn-chengdu.personal.cr.aliyuncs.com/t1longlive/openjdk:8-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
