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
  1. [x] 搭建 RocketMQ 基于 Dledger 的多节点集群（或者多主多从）。
  2. [x] 调整微服务的 Producer 和 Consumer 连接集群地址池。
  3. [x] 拔掉一台 Broker 的网线，验证业务的消息收发是否能够实现毫秒级自动切换。

## 🚩 Phase 18: K8s 高阶运维 (Helm & GitOps)
- **目标**：以声明式代码的方式自动化管理应用的云原生全生命周期。
- **动作**：
  1. [x] 引入 Helm Packet Manager 统筹部署几十个微服务的海量 YAML 文件。
  2. [x] 阐述与部署基于 GitOps 的最核心自动化流水线 (ArgoCD)。

---
# 🔐 Chapter 4: Microservice Advanced Security (微服务安全完全体)

## 🚩 Phase 19: 统一认证与鉴权中心 (OAuth2.0 + JWT) ✅
- **目标**：构建企业级统一安全防线，对整条微服务调用链路进行身份校验闭环。
- **动作**：
  1. [x] 搭建独立的 `leaf-auth` 认证中心，集成 Spring Authorization Server，动态生成 RSA 2048 密钥对并签发 JWT。
  2. [x] 在 `leaf-gateway` 实现全局 JWT Token 校验（`SecurityWebFilterChain`）：无 Token → `401`，有效 Token → 放行路由。修复 `StripPrefix=2` 路由 Bug。
  3. [x] 在 `leaf-service-order` 添加 `FeignTokenRelayConfig` Feign 拦截器，自动从当前请求上下文提取并透传 `Authorization` Header。
  4. [x] 在 `leaf-service-user` 接入 OAuth2 Resource Server（`ResourceServerConfig`），验证内部 RPC 的 JWT 合法性。
  5. [x] 全链路 E2E 验证：无 Token → `401`，携带 Token → 订单创建成功（跨越 Gateway → Order → Feign → User 完整链路）。

## 🚩 Phase 20: 搜索引擎与实时数据管道 (Elasticsearch + Canal) ⬜
- **目标**：亿级数据毫秒级全文检索，MySQL 数据库变更实时同步到 ES。
- **动作**：
  1. [ ] 部署 Elasticsearch 搜索引擎。
  2. [ ] 引入 Alibaba Canal，伪装成 MySQL 从节点监听 Binlog，变更零延迟同步至 ES。
  3. [ ] 改造核心微服务接口，体验全文检索与高亮词查找。

## 🚩 Phase 21: Service Mesh 服务网格终极形态 (Istio) ⬜
- **目标**：将限流熔断和复杂路由从业务代码中彻底剥离，由基础架构层接管。
- **动作**：
  1. [ ] 在 K8s 集群中安装 Istio 并启用 Sidecar Envoy 流量代理无感注入。
  2. [ ] 实操体验金丝雀发布（灰度发布）和基于 Header 的高级流量截断机制。

---
# 🧪 Chapter 5: Engineering Quality & Testing (工程质量与测试体系)

## 🚩 Phase 22: 单元测试与集成测试体系 ⬜
- **目标**：建立可持续维护的自动化测试基础设施，保障代码质量与回归安全。
- **动作**：
  1. [ ] 引入 JUnit 5 + Mockito，为核心业务逻辑 (User/Order Service) 编写单元测试。
  2. [ ] 引入 Testcontainers，使用真实 MySQL/Redis 容器进行集成测试，告别 Mock 数据库的假象。
  3. [ ] 集成 JaCoCo 代码覆盖率报告，在 Maven 构建时自动生成覆盖率统计。
  4. [ ] 建立测试分层规范：Unit → Integration → E2E，明确各层职责边界。

## 🚩 Phase 23: 契约测试 (Contract Testing) ⬜
- **目标**：确保 Feign 接口消费方与提供方的契约一致性，防止接口变更导致隐性故障。
- **动作**：
  1. [ ] 引入 Spring Cloud Contract 或 Pact 框架。
  2. [ ] 为 `UserClient` Feign 接口编写 Provider/Consumer 双向契约测试。
  3. [ ] 在 CI 流水线中集成契约验证，接口不兼容变更时自动阻断构建。

---
# 🚀 Chapter 6: CI/CD & DevOps Pipeline (持续集成与交付流水线)

## 🚩 Phase 24: GitHub Actions CI 流水线 ⬜
- **目标**：实现代码提交到自动构建、测试、镜像打包的全自动化闭环。
- **动作**：
  1. [ ] 编写 `.github/workflows/ci.yml`，实现多模块自动构建与单元测试。
  2. [ ] PR 触发自动测试，通过后才允许合并。
  3. [ ] 构建成功后自动打包 Docker 镜像并推送至 GitHub Container Registry (ghcr.io) 或 Docker Hub。
  4. [ ] 添加构建状态徽章到 README.md。

## 🚩 Phase 25: ArgoCD GitOps 实战落地 ⬜
- **目标**：从 Phase 18 的理论介绍推进到完整实操，实现 Git 驱动的自动化部署。
- **动作**：
  1. [ ] 在 K8s 集群中安装 ArgoCD 并配置 Web UI 访问。
  2. [ ] 关联 GitHub 仓库，配置 Application 自动同步 `infra-deploy/k8s/` 目录。
  3. [ ] 演练完整流程：Git Push → ArgoCD 检测变更 → 自动部署 → 健康检查。
  4. [ ] 实操灰度回滚：模拟故障版本发布，通过 ArgoCD 一键回滚到上一个健康版本。

---
# 📊 Chapter 7: Deep Observability (可观测性深化)

## 🚩 Phase 26: Grafana 全栈运维大屏 ⬜
- **目标**：构建生产级监控大屏，一屏掌控全局健康状态，异常秒级告警。
- **动作**：
  1. [ ] 设计并导入 Grafana Dashboard：JVM 监控面板（堆内存/GC/线程数）。
  2. [ ] 构建 API 性能面板（QPS、P99 延迟、错误率）。
  3. [ ] 添加中间件健康面板：RocketMQ 消费进度、MySQL 主从延迟、Redis Cluster 节点状态。
  4. [ ] 配置 Prometheus AlertManager 告警规则（CPU > 80%、接口 P99 > 2s、MQ 消费堆积等），对接通知渠道。

## 🚩 Phase 27: OpenTelemetry 统一观测标准 ⬜
- **目标**：从 Micrometer+Zipkin 升级到 OpenTelemetry，统一 Traces/Metrics/Logs 三大观测信号。
- **动作**：
  1. [ ] 引入 OpenTelemetry Java Agent 或 SDK，替换现有 Micrometer Tracing。
  2. [ ] 配置 OTel Collector 作为统一数据管道，分发至 Jaeger (Traces) + Prometheus (Metrics) + Loki (Logs)。
  3. [ ] 实现 Trace-Log 关联：在日志中自动注入 TraceID，从 Grafana 一键跳转到对应链路。

---
# 🛡️ Chapter 8: Production-Grade Security (生产级安全加固)

## 🚩 Phase 28: RBAC 细粒度权限控制 ⬜
- **目标**：扩展 OAuth2 认证中心，从 client_credentials 升级为完整的用户认证 + 角色权限体系。
- **动作**：
  1. [ ] 设计并创建 RBAC 数据库模型：`t_user_account` / `t_role` / `t_permission` / `t_user_role` / `t_role_permission`。
  2. [ ] 扩展 `leaf-auth`，支持 `authorization_code` 和 `password` 授权模式，JWT Payload 中携带 `authorities` 声明。
  3. [ ] 在 Resource Server 端实现接口级权限校验：`@PreAuthorize("hasAuthority('user:read')")`。
  4. [ ] E2E 验证：不同角色用户访问同一接口，返回不同的权限结果（200 / 403）。

## 🚩 Phase 29: API 安全加固 ⬜
- **目标**：为生产环境 API 穿上多层防弹衣，抵御常见攻击向量。
- **动作**：
  1. [ ] 接口幂等性设计：基于 Redis Token 实现防重放机制，避免重复提交。
  2. [ ] 请求签名验签：对关键接口的请求参数进行 HMAC-SHA256 签名校验。
  3. [ ] 敏感字段加密：手机号、身份证等字段落库前 AES 加密，查询时解密。
  4. [ ] 全局安全审计：SQL 注入防护（MyBatis 参数化）、XSS 过滤、CORS 精细化策略。

---
# 🏗️ Chapter 9: Domain-Driven Design & Architecture Evolution (领域驱动与架构演进)

## 🚩 Phase 30: DDD 领域驱动设计重构 ⬜
- **目标**：选取 Order 服务作为试点，从贫血模型演进为 DDD 充血模型，体验战术设计精髓。
- **动作**：
  1. [ ] 重构 Order 服务目录结构为 DDD 四层架构：`interfaces` → `application` → `domain` → `infrastructure`。
  2. [ ] 引入聚合根 (Aggregate Root)、值对象 (Value Object)、领域事件 (Domain Event) 概念。
  3. [ ] 实现仓储模式 (Repository Pattern)，将数据访问逻辑从业务逻辑中解耦。
  4. [ ] 编写领域事件发布机制（基于 Spring ApplicationEvent 或 RocketMQ），实现跨聚合的最终一致性。

## 🚩 Phase 31: CQRS + Event Sourcing ⬜
- **目标**：实现读写分离的架构升级，写操作走 Command 侧，读操作走 Query 侧，通过事件总线实现最终一致性。
- **动作**：
  1. [ ] 设计 CQRS 架构：Command 端负责业务写入（MySQL），Query 端负责高性能查询（Elasticsearch）。
  2. [ ] 通过 RocketMQ 领域事件将写操作产生的变更异步同步到 Query 端。
  3. [ ] 实现 Event Sourcing：核心聚合的状态由事件流重建，支持完整的操作审计与时间旅行查询。

---
# ☁️ Chapter 10: Cloud-Native Evolution (云原生进阶)

## 🚩 Phase 32: GraalVM Native Image ⬜
- **目标**：利用 Spring Boot 3 的 AOT 编译能力，将微服务编译为 GraalVM 原生镜像，极致性能优化。
- **动作**：
  1. [ ] 配置 GraalVM 环境，在 `pom.xml` 中启用 `native-maven-plugin`。
  2. [ ] 解决 AOT 编译中的反射注册、动态代理等 Native Image 兼容性问题。
  3. [ ] 对比验证：JVM 模式 vs Native 模式的启动时间（秒 → 毫秒）和内存占用（降低 ~80%）。
  4. [ ] 将 Native Image 打包为超轻量 Docker 镜像并部署至 K8s。

## 🚩 Phase 33: 多环境管理 (dev / staging / prod) ⬜
- **目标**：建立完整的多环境配置体系，实现环境隔离与安全发布。
- **动作**：
  1. [ ] 在 Nacos 中创建 `dev` / `staging` / `prod` 命名空间，按环境隔离配置。
  2. [ ] 使用 K8s ConfigMap + Secret 管理敏感配置（数据库密码、密钥等），结合 Spring Cloud Kubernetes 自动注入。
  3. [ ] 建立发布规范：dev 自由部署 → staging 冒烟测试 → prod 审批发布，配合 ArgoCD 多环境同步策略。

## 🚩 Phase 34: 云厂商部署实战 ⬜
- **目标**：将项目从本地 Docker Desktop K8s 部署到真实云环境，体验生产级云原生全流程。
- **动作**：
  1. [ ] 选择云厂商（阿里云 ACK / AWS EKS / 腾讯云 TKE），创建托管 K8s 集群。
  2. [ ] 配置 Ingress Controller + 域名解析 + HTTPS 证书（Let's Encrypt / 云厂商证书管理）。
  3. [ ] 配置 HPA (Horizontal Pod Autoscaler) 弹性伸缩策略，基于 CPU/内存/自定义指标自动扩缩容。
  4. [ ] 全链路压测：模拟真实流量洪峰，验证自动扩容 → 流量分发 → 缩容回收的完整弹性能力。
