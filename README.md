# Leaf Microservice Framework 🌱

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.11-brightgreen.svg)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue.svg)
![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.3-orange.svg)
![JDK](https://img.shields.io/badge/JDK-21-red.svg)

> **Leaf SpringCloud** 是一套从零到一构建的现代化企业级微服务架构学习和实践项目。
> 
> 本项目遵循高内聚、低耦合的设计原则，通过 Spring Cloud Alibaba 生态实现了服务发现、流量治理、远程调用、消息解耦以及完善的可观测性体系，并打通了本地到云端的 DevOps (Jenkins) 全自动化流水线部署方案。

## 🎯 项目核心亮点

- ⚡️ **前沿架构选型**：全面拥抱 **JDK 21**，底层基于 `Spring Boot 3.2.x` + `Spring Cloud Alibaba 2023`，体验最新虚拟线程与生态特性。
- 🛡️ **微服务治理闭环**：以 Nacos 为双子星（配置与注册中心），OpenFeign 解决服务间痛点调用，Sentinel 作为流量防卫兵实现接口熔断与降级。
- 📦 **中间件火力全开**：集成 MySQL8、Redis 以及 RocketMQ 5 消息队列完成异步解耦和错峰流控。
- 📊 **世界级可观测大屏**：通过 Spring Boot Actuator 暴漏微服务心跳，借由 Prometheus 进行时序拉取，最后由 Grafana 点亮极客级的监控大盘。
- 🛠️ **DevOps & CI/CD**：内置定制的 `Dockerfile` 与 `Jenkinsfile` 自动化构建脚本，以及本地化的 Docker-compose 一键式环境部署底座。

## 🏗️ 系统架构拓扑

```mermaid
graph TD
    A[User Request] --> B(Leaf-Gateway)
    B -->|Route & Filter| C{Nacos Service Discovery}
    C --> D[Leaf-Service-User]
    C --> E[Leaf-Service-Order]
    
    D <-.-> |OpenFeign & Sentinel| E
    
    D --> F[(MySQL)]
    D --> G[(Redis)]
    
    E --> H((RocketMQ))
    H --> |Consume Async| D
    
    I[Prometheus] --> |Scrape Metrics| B
    I --> |Scrape Metrics| D
    I --> |Scrape Metrics| E
    J[Grafana] --> |Visualize| I
```

## 📂 模块指南及结构

整个微服务划分为如下几个基础骨架：

| 核心模块名 | 端口 | 模块职责 |
|---|---|---|
| **[leaf-common](./leaf-common)** | - | 基础依赖包、统一常量、Result 通用响应体包装器 |
| **[leaf-gateway](./leaf-gateway)** | `8080` | 集群的统一流量网关，处理请求路由转发与鉴权 |
| **[leaf-service-user](./leaf-service-user)** | `8081` | 用户域微服务 (包含 MyBatis-Plus 与 Redis 缓存整合实践) |
| **[leaf-service-order](./leaf-service-order)** | `8082` | 订单域微服务 (验证 OpenFeign RPC 及 RocketMQ 异步投递) |

## 🚀 极速起航 (Quick Start)

### 1. 基础环境搭建
系统根目录下的 `infra-deploy` 目录为您准备了一套完整的 Docker Compose 配置文件：
```bash
cd infra-deploy
# 在本地直接拉起 Nacos、MySQL、Redis、RocketMQ、Prometheus 与 Grafana
docker compose up -d
```
> *注：MySQL 脚本已经自动挂载并导入了必要的初始化建库语句设置。*

### 2. 本地项目启动开发
由于引入了 JDK21，请确保您的 IDE (如 IntelliJ IDEA) 设置 JDK 版本为 21。
按以下次序启动核心主程序：
1. `GatewayApplication`
2. `UserApplication`
3. `OrderApplication`

访问测试接口：
```text
网关直接访问订单层出单 (自动 RPC 调用的验证)
GET http://localhost:8080/api/order/create/1
```

### 3. 可视化监控探活
- **Nacos 控制台**: [http://127.0.0.1:8848/nacos](http://127.0.0.1:8848/nacos) *(nacos/nacos)*
- **Sentinel Dashboard**: [http://127.0.0.1:8080](http://127.0.0.1:8080) (如果已单独起服务)
- **Grafana JVM 大盘**: [http://127.0.0.1:3000](http://127.0.0.1:3000) *(admin/admin)*

## 💡 技术栈清单 (Tech Stack)

* **语言**: Java >= 21
* **核心框架**: Spring Boot 3.2.11 / Spring Cloud 2023.0.1
* **注册/配置中心**: Alibaba Nacos
* **远程过程调用**: Spring Cloud OpenFeign
* **服务容错降级**: Alibaba Sentinel
* **统一流控网关**: Spring Cloud Gateway
* **持久层与连接池**: MySQL 8 / MyBatis-Plus / HikariCP
* **缓存层**: Redis (Spring Data Redis)
* **消息队列中间件**: Apache RocketMQ 5.1.4
* **系统监控大盘**: Micrometer / Prometheus / Grafana
* **容器化与流水线**: Docker / Jenkins (Native Pipeline)

## 📌 进阶笔记与避坑指南
对于 CI/CD 构建部分以及网络抓取排坑史，我们在开发历程中特意总结了一篇专属小册子：
> 详见 [Jenkins 本地/离线环境与踩坑指北 (jenkins_troubleshooting.md)](./docs/jenkins_troubleshooting.md)

---
*Created by [Paiky](https://github.com/paiky) together with Antigravity AI Code Assistant.*
