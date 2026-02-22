# Spring Cloud Alibaba & 云原生微服务实战演进脉络图 (Task & Timeline)

本项目从零开始一步步搭建起了**企业级、高可用、云原生标准**的微服务底座架构。为了方便后续阅读和回溯，特此将整体搭建和演进路线整理为脉络图。

---

## 🚩 Phase 1: 基础设施构建 & 脚手架初始化 (Day 1)
- **目标**：搭建 Spring Cloud Alibaba 项目多模块骨架。
- **动作**：
  1. 创建顶层父 POM `leaf-springcloud`，管理所有依赖版本 (Spring Boot 3.2.x, Spring Cloud 2023.0.x, Alibaba 2023.0.x)。
  2. 建立 `leaf-common` 模块，编写统一返回标准对象 `Result<T>`。
  3. 初步搭出 `leaf-service-user` 和 `leaf-service-order` 模块。

## 🚩 Phase 2: 后端底层存储中间件集结 (Day 1)
- **目标**：以容器化思想准备数据库和缓存环境。
- **动作**：
  1. 引入 Docker Compose 单机编排能力，编写 `infra-deploy/docker-compose.yml`。
  2. 启动单机版的 `MySQL` 和 `Redis`，作为微服务的公共底座。
  3. 配置了 `host.docker.internal` 为统一内网穿透别名。

## 🚩 Phase 3: Nacos 注册配置中心 (Day 1)
- **目标**：解决“微服务不知彼此在何方”和“配置散落难配”的问题。
- **动作**：
  1. 在 `docker-compose.yml` 中塞入并启动了 `Nacos 2.3.0` 服务器。
  2. 配置 `leaf-service-user` 使其注册到 Nacos 开发空间。
  3. 将本地的 `application.yml` 彻底迁移到 Nacos Config Center。

## 🚩 Phase 4: Feign 与 Ribbon 负载均衡 (Day 1)
- **目标**：实现服务与服务之间的相互调用。
- **动作**：
  1. Order 服务引入 `OpenFeign` 和 Nacos Discovery，编写 `UserClient` 接口。
  2. 实现了从订单发起的 HTTP 层请求，并成功通过负载均衡分发至 User 节点。

## 🚩 Phase 5: RocketMQ 异步削峰 (Day 2)
- **目标**：让核心主干链路解耦，抵御突发流量洪峰。
- **动作**：
  1. `docker-compose.yml` 中新增 `RocketMQ NameServer` 和 `Broker`。
  2. 配置 `broker.conf` 以适应容器跨端路由。
  3. Order 服务创建订单后发送异步消息，User 服务通过 `RocketMQListener` 消费，实现削峰填谷。

## 🚩 Phase 6: Sentinel 流量哨兵护航 (Day 2)
- **目标**：保护脆弱微服务免遭雪崩。
- **动作**：
  1. 将 Sentinel 控制台打包入底座（后简化为直连 Nacos 持久化流控规则）。
  2. 在 Order 服务的下单接口进行速率熔断，压测超过 QPS=1 时，立刻触发流控降级，接口返回限流提示而不是系统卡死崩溃。
  3. 为 Feign 提供全局 `Fallback` 兜底实现。

## 🚩 Phase 7: Gateway 统一微服务网关 (Day 2)
- **目标**：对整个散落在背后的微服务群进行路由收敛。
- **动作**：
  1. 建立 `leaf-gateway` 网关模块，不挂载 Tomcat 而是基于 Netty 非阻塞运行。
  2. 根据 URL `/api/user/**` 和 `/api/order/**` 实现到各个子服务的流量分发与负载转发。
  3. (可选)：网关侧 Sentinel 的全局限流接入。

## 🚩 Phase 8: SkyWalking / Jaeger 分布式链路追踪探针 (Day 2)
- **目标**：顺着一条请求 ID，看穿其在所有微服务节点里的游走耗时和报错点。
- **动作**：
  1. `docker-compose.yml` 增加 `Jaeger All-In-One` 追踪监控中心。
  2. 全线接入 Micrometer & Zipkin 标准实现方法追踪 `spring.boot.actuator` 结合。

## 🚩 Phase 9: XXL-JOB 分布式任务调度中心 (Day 3)
- **目标**：彻底丢弃 `@Scheduled` 的单点故障隐患和不可控问题。
- **动作**：
  1. 从官方获取 `XXL-JOB` 数据库表，在 MySQL 初始化 `xxl_job` 库。
  2. 将大管家 `xxl-job-admin` 加入 `docker-compose`。
  3. 在 `leaf-service-order` 挂载 `XxlJobSpringExecutor` 执行器，实现跨越宿主机与容器网段的调度握手，手写第一个自动同步订单状态的调度任务（10秒一次执行成功）。

## 🚩 Phase 10: Seata 分布式事务解决跨库数据一致性 (Day 3)
- **目标**：一顿跨库的下订单、扣库存、扣余额操作，要么全部成功，要么一齐回滚。
- **动作**：
  1. 部署 `Seata Server 2.0.0` (TC) 事务协调器。
  2. 所有业务表结构所在的数据库 (MySQL) 同步初始化 `undo_log` 回滚流水表。
  3. 在 `leaf-service-order` 中加持 `@GlobalTransactional`，演示跨库扣减导致异常后，成功回退用户已经扣掉的余额数据。

## 🚩 Phase 11: 云原生时代的观测站 (ELK / PLG Stack) (Day 4)
- **目标**：对部署在各处的容器抓取 CPU、内存与标准输出日志的健康指标。
- **动作**：
  1. 部署 `Prometheus` (监控抓手) + `Grafana` (大屏控制台) + `Loki` (日志引擎) + `Promtail` (贴身日志搜集员)。
  2. 配置端点暴露出 JVM 指标、Spring Actuator、Nacos JVM，统统上报。

## 🚩 Phase 12-14: 终局之战 Kubernetes 容器集群化接管 (Day 4)
- **目标**：脱离玩具级的本地开发 IDE 启动模型，享受动态扩缩容（弹性和自愈）机制。
- **动作**：
  1. 利用 `Dockerfile` 为所有的核心组件打出云原生 Docker 镜像。
  2. 告别 `docker-compose` 和繁冗命令，手写 `Deployment` 和 `Service` YAML 图纸文件。
  3. `kubectl apply -f` 秒级完成从 1 台实例无痛扩容出 3 台高可用实例并发分流 (`NodePort: 30080`)。
  4. 解决极为致命但经典的“跨环境网络映射”问题（正确运用环境变量解耦与 K8s DNS 解析穿透）。

---
**阶段性完结留念 (2026/02)**
一入微服务深似海，但顺着以上步骤，所有晦涩难懂的技术模块都成了受您指挥的云原生军团！

---
# 🚀 Chapter 3: High Availability & Clustering (高可用与集群演进)

前面的架构虽全，但底座仍是**单点**。接下来我们将为这些核心基础设施穿上“防弹衣”，打造真正的企业级高可用架构：

## 🚩 Phase 15: Redis 高可用集群 (Redis Cluster / Sentinel)
- **目标**：解决单节点 Redis 宕机导致的缓存雪崩和单点故障。
- **动作**：
  1. [x] 理解 Redis 主从复制、哨兵模式和 Cluster 分片集群的区别与适用场景。
  2. [x] 使用 Docker Compose 搭建 Redis Cluster。
  3. [x] 修改 Spring Boot (Lettuce/Jedis) 配置，使其无缝连接并具备感知集群拓扑变化的能力。

## 🚩 Phase 16: MySQL 主从复制与读写分离 (Primary-Replica)
- **目标**：为了应对高并发下的数据库读瓶颈，将查询压力分担至多个只读节点。
- **动作**：
  1. [x] 搭建 MySQL 主从复制架构 (`Master -> Slaves`)。
  2. [x] （可选）引入 ShardingSphere-JDBC 或 MyBatis-Plus Dynamic Datasource 中间件。
  3. [x] 修改业务代码验证读写剥离（写主库，读从库）。

## 🚩 Phase 17: RocketMQ 高可用集群 (Dledger / Master-Slave)
- **目标**：保证消息零丢失和 Broker 宕机时的自动故障转移。
- **动作**：
  1. 搭建 RocketMQ 基于 Dledger 的多节点集群（或者多主多从）。
  2. 调整微服务的 Producer 和 Consumer 连接集群地址池。
  3. 拔掉一台 Broker 的网线，验证业务的消息收发是否能够实现毫秒级自动切换。

## 🚩 Phase 18: K8s 高阶运维 (Helm & GitOps)
- **目标**：以声明式代码的方式自动化管理应用的云原生全生命周期。
- **动作**：
  1. 引入 Helm Packet Manager 统筹部署几十个微服务的海量 YAML 文件。
  2. 部署 ArgoCD 搭建 GitOps 自动化流水线。
