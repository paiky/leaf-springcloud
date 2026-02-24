# 🧠 Leaf SpringCloud 项目上下文交接文档

> **目的**：本文档是跨对话窗口的"记忆接力棒"。当你在新对话窗口开始工作时，
> 请先完整阅读本文档，它包含了项目的当前状态、已验证的结论、踩过的坑和下一步指令。

---

## 1. 项目概述

**项目名称**: Leaf SpringCloud  
**仓库地址**: `https://github.com/paiky/leaf-springcloud`  
**本地路径**: `e:\workspace\projects\leaf-springcloud`  
**技术栈**: Java 21 + Spring Boot 3.2.11 + Spring Cloud 2023.0.1 + Spring Cloud Alibaba 2023.0.1.3  

这是一个从零到一构建的**企业级微服务架构学习项目**，已经完成了从单体到云原生的完整演进。

---

## 2. Current State: 项目物理结构

### 2.1 Maven 模块结构

```
leaf-springcloud/                   # 顶层父 POM
├── leaf-common/                    # 公共依赖模块 (Result<T> 统一响应体)
├── leaf-auth/                      # 🔐 OAuth2 认证中心 (port: 8083)
│   └── config/
│       ├── SecurityConfig.java     # 授权服务器配置 + RegisteredClient
│       └── KeyPairConfig.java      # RSA 2048 密钥对动态生成
├── leaf-gateway/                   # 🔐 API 网关 (port: 8080, K8s NodePort: 30080)
│   └── config/
│       └── SecurityConfig.java     # JWT 校验过滤器 (WebFlux Security)
├── leaf-service-user/              # 用户微服务 (port: 8081, K8s NodePort: 30081)
│   └── config/
│       └── ResourceServerConfig.java  # OAuth2 Resource Server (验证 JWT)
├── leaf-service-order/             # 订单微服务 (port: 8082)
│   └── config/
│       └── FeignTokenRelayConfig.java # Feign 拦截器 (JWT 透传)
├── infra-deploy/
│   ├── docker-compose.yml          # 本地基础设施 (Nacos, MySQL, Redis, RocketMQ, Prometheus...)
│   ├── k8s/                        # K8s Deployment + Service YAML
│   │   ├── leaf-auth.yaml          # Auth服务 K8s (NodePort: 30083)
│   │   ├── leaf-gateway.yaml       # 网关 K8s (NodePort: 30080)
│   │   ├── leaf-service-user.yaml  # 用户服务 K8s (NodePort: 30081)
│   │   └── leaf-service-order.yaml # 订单服务 K8s
│   ├── redis-cluster/              # Redis 6节点集群 (6371-6376)
│   ├── mysql-cluster/              # MySQL 主从 (master:3307, slave:3308)
│   ├── rocketmq-cluster/           # RocketMQ DLedger 集群
│   └── helm/                       # Helm Chart 定义
└── docs/
    └── task.md                     # 📋 完整的演进脉络图 (所有 Phase 状态)
```

### 2.2 关键配置文件位置

| 文件 | 作用 |
|---|---|
| `leaf-gateway/src/main/resources/application.yml` | 网关路由规则 + JWK URI |
| `leaf-auth/src/main/resources/application.yml` | 认证中心端口 + Nacos + Redis |
| `leaf-service-user/src/main/resources/application.yml` | 用户服务 + JWK URI |
| `leaf-service-order/src/main/resources/application.yml` | 订单服务配置 |
| `infra-deploy/k8s/*.yaml` | K8s 部署清单 (含环境变量覆盖) |

### 2.3 当前已部署的 K8s 服务 (Docker Desktop K8s)

| 服务 | K8s Deployment | NodePort |
|---|---|---|
| leaf-auth | leaf-auth-deployment | 30083 |
| leaf-gateway | leaf-gateway-deployment | 30080 |
| leaf-service-user | leaf-service-user-deployment | 30081 |
| leaf-service-order | leaf-service-order-deployment | - |

> **注意**: K8s 集群可能未运行（Docker Desktop 未启动时）。
> 本地中间件通过 `docker compose up -d` 启动（在 `infra-deploy/` 目录下）。

---

## 3. Completed Phases: 已完成的所有阶段

详细清单见 `docs/task.md`，以下是摘要：

| Chapter | Phases | 状态 |
|---|---|---|
| **Chapter 1**: 基础架构 | Phase 1-7 (骨架→Nacos→Feign→MQ→Sentinel→Gateway→Tracing) | ✅ 全部完成 |
| **Chapter 2**: 进阶运维 | Phase 8-13 (Loki→Jaeger→Seata→XXL-JOB→K8s) | ✅ 全部完成 |
| **Chapter 3**: 高可用集群 | Phase 14-18 (Redis Cluster→MySQL主从→RocketMQ DLedger→Helm/GitOps) | ✅ 全部完成 |
| **Chapter 4**: 安全完全体 | Phase 19 (OAuth2+JWT认证中心) | ✅ 已完成 |
| **Chapter 4**: 安全完全体 | Phase 20 (Elasticsearch+Canal) | ⬜ 下一步 |
| **Chapter 4**: 安全完全体 | Phase 21 (Istio Service Mesh) | ⬜ 未来 |

---

## 4. Success Criteria: Phase 19 已验证的成功标准

以下测试在 K8s 环境中已全部通过 (2026-02-23)：

```bash
# ✅ TEST 1: 获取 JWT Token
curl -X POST -u "leaf-gateway:leaf-secret" \
  http://localhost:30080/api/auth/oauth2/token \
  -d "grant_type=client_credentials"
# 返回: {"access_token":"eyJ...","token_type":"Bearer","expires_in":299}

# ✅ TEST 2: 无 Token 访问 → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:30080/api/user/3
# 返回: 401

# ✅ TEST 3: 无 Token 直连 User 服务 → 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:30081/user/3
# 返回: 401

# ✅ TEST 4: 带 Token 经网关访问 → 200
curl -H "Authorization: Bearer <token>" http://localhost:30080/api/user/3
# 返回: {"code":200,"data":{"id":3,"username":"admin",...}}

# ✅ TEST 5: 带 Token 创建订单 (全链路: Gateway → Order → Feign JWT 透传 → User)
curl -H "Authorization: Bearer <token>" http://localhost:30080/api/order/create/3
# 返回: {"code":200,"data":"Order created successfully. TX Commited. OrderID: 32"}
```

---

## 5. Rejected Paths: 踩过的坑（严禁重蹈覆辙）

### 5.1 ⚠️ Gateway 路由 StripPrefix 数值
**问题**: `/api/auth/oauth2/token` 路由到 `leaf-auth`，如果 `StripPrefix=1` 只会剥除 `/api`，
实际转发的是 `/auth/oauth2/token`（404），而不是正确的 `/oauth2/token`。  
**正确做法**: **`StripPrefix=2`**，同时剥掉 `/api` 和 `/auth` 两段。
```yaml
# leaf-gateway application.yml 路由配置
- id: leaf-auth-route
  uri: http://leaf-auth-svc:8083
  predicates:
    - Path=/api/auth/**
  filters:
    - StripPrefix=2    # ← 必须是 2，不是 1！
```

### 5.2 ⚠️ Nacos 命名空间必须一致
**问题**: `leaf-auth` 曾经在 `bootstrap.yml` 中配了 `namespace: leaf-dev`，
导致它注册到了错误的 Nacos 命名空间，Gateway 的 LoadBalancer 找不到它（503）。  
**正确做法**: 所有微服务都应注册在**默认命名空间 (public)**，不要单独给某个服务加 namespace。

### 5.3 ⚠️ Feign 拦截器 Bean 重复注册
**问题**: `FeignTokenRelayInterceptor.java` 类同时 `implements RequestInterceptor`（自身被 Spring 作为 Bean 扫描）
又用 `@Bean` 方法返回了 `new FeignTokenRelayInterceptor()`，导致同名 Bean 冲突，Pod 启动失败。  
**正确做法**: 使用 `@Configuration` 类 + `@Bean` 方法返回匿名 `RequestInterceptor` 实现，
文件名必须是 `FeignTokenRelayConfig.java`（类名和文件名一致）。

### 5.4 ⚠️ Gateway SecurityConfig 必须禁用 httpBasic 和 formLogin
**问题**: 不禁用的话，请求会被 302 重定向到 `/login` 页面（Spring Security 默认行为）。  
**正确做法**:
```java
.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
```

### 5.5 ⚠️ Maven 编译 leaf-service-order 必须加 -am
**问题**: `mvn clean package -pl leaf-service-order -DskipTests` 不加 `-am` 会因为缺少
`leaf-common` 的传递依赖（如 `xxl-job-core`）而编译失败。  
**正确做法**: 始终使用 `mvn clean package -pl <module> -am -DskipTests`。

### 5.6 ⚠️ OrderController 的 createOrder 是 @GetMapping 不是 @PostMapping
`/order/create/{userId}` 使用的是 `@GetMapping`，用 POST 请求会返回 405。

### 5.7 ⚠️ K8s 容器内访问宿主机中间件
容器内不能用 `127.0.0.1`，必须用 `host.docker.internal` 访问宿主机上的 MySQL/Nacos/Redis 等。
但同一 K8s 集群内的服务之间，使用 K8s Service DNS（如 `leaf-auth-svc:8083`）。

---

## 6. Environment: 本地开发环境关键信息

| 组件 | 地址 | 备注 |
|---|---|---|
| **Nacos** | `127.0.0.1:8848` | admin/nacos |
| **MySQL Master** | `127.0.0.1:3307` | root/root123 |
| **MySQL Slave** | `127.0.0.1:3308` | root/root123 |
| **Redis Cluster** | `127.0.0.1:6371-6376` | 6节点分片集群 |
| **RocketMQ NameServer** | `127.0.0.1:9876;127.0.0.1:9877` | DLedger 双节点 |
| **Prometheus** | `127.0.0.1:9090` | |
| **Grafana** | `127.0.0.1:3000` | admin/admin |
| **Jaeger** | `127.0.0.1:16686` | |
| **XXL-JOB Admin** | `127.0.0.1:8088/xxl-job-admin` | admin/123456 |
| **Seata TC** | `127.0.0.1:8091` | |
| **OAuth2 Client** | `leaf-gateway` / `leaf-secret` | client_credentials 授权 |

**操作系统**: Windows  
**Shell**: PowerShell (`tail` 命令不可用，用 `Select-Object -Last N`)  
**Docker**: Docker Desktop (K8s 集群内置)

---

## 7. Next Steps: Phase 20 原子操作指令

### Phase 20: Elasticsearch + Canal 实时数据管道

**目标**: 将 MySQL `leaf_user` 库的数据通过 Canal 监听 Binlog 实时同步到 Elasticsearch，
然后在 `leaf-service-user` 中提供全文搜索 API。

**建议步骤**:

1. **Step 1**: 在 `infra-deploy/docker-compose.yml` 中添加 Elasticsearch 8.x 和 Kibana 服务。
2. **Step 2**: 确保 MySQL Master (`3307`) 的 Binlog 已开启（`log-bin=mysql-bin`, `binlog-format=ROW`）。
3. **Step 3**: 在 `infra-deploy/docker-compose.yml` 中添加 Alibaba Canal Server，配置伪装为 MySQL Slave 连接 Master。
4. **Step 4**: 配置 Canal 的 Destination 监听 `leaf_user` 数据库的 `t_user` 表变更。
5. **Step 5**: 在 `leaf-service-user` 中引入 `spring-boot-starter-data-elasticsearch` 依赖。
6. **Step 6**: 创建 `UserDocument` 实体和 `UserSearchRepository` 接口。
7. **Step 7**: 实现 Canal 消费者（或使用 Canal Client Adapter 直连 ES），将变更同步到 ES。
8. **Step 8**: 在 `UserController` 中添加 `/user/search?keyword=xxx` 搜索接口。
9. **Step 9**: E2E 验证：修改 MySQL 数据 → Canal 自动同步 → ES 中可搜索到最新数据。

---

## 8. 构建与部署常用命令速查

```bash
# 编译单个模块 (必须带 -am)
mvn clean package -pl leaf-service-user -am -DskipTests

# Docker 打镜像
docker build -t leaf/leaf-service-user:latest -f Dockerfile leaf-service-user/

# K8s 部署 / 重启
kubectl apply -f infra-deploy/k8s/leaf-service-user.yaml
kubectl rollout restart deployment/leaf-service-user-deployment

# 查看 Pod 日志
kubectl logs deployment/leaf-service-user-deployment --since=5m

# 获取 JWT Token (K8s 环境)
$token = (curl.exe -s -X POST -u "leaf-gateway:leaf-secret" "http://localhost:30080/api/auth/oauth2/token" -d "grant_type=client_credentials" | ConvertFrom-Json).access_token

# 带 Token 访问
curl.exe -s -H "Authorization: Bearer $token" http://localhost:30080/api/user/3
```

---

*最后更新: 2026-02-24 22:26 | Phase 19 完成后*
