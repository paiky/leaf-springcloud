# 🐋 Docker 常用绝密兵器谱（速查宝典）

在我们的高可用架构演进中，Docker 和 Docker-Compose 是地基。为了避免指令遗忘，这份手册收录了你以后在排查和运维问题时最常用的绝密命令。

---

## 🛠️ 第一章：容器生命周期管理 (生死簿)

### 1. 查勘天下容器
这是每天上班第一件必须敲的命令，查看所有正在运行的服务状态：
```bash
docker ps
```
*如果你想看到连同挂掉的（停止的）也一起显示，请加 `-a`*：
```bash
docker ps -a
```

### 2. 生杀大权
启动、停止或重启某个单独的“打工人”容器：
```bash
# 停止一个正在运行的容器 (平滑退出)
docker stop 容器名字或者ID (例如: docker stop leaf-mysql-master)

# 强制杀死一个不响应的容器 (拔电源)
docker kill 容器名字或ID

# 启动和重启
docker start 容器名字或ID
docker restart 容器名字或ID
```

### 3. 抹除痕迹
当一个容器已经被 `stop` 了，但它依然占用硬盘和列表里：
```bash
docker rm 容器名或ID
# 如果你想强制删除一个正在运行的容器(危险): docker rm -f 容器名或ID
```

---

## 🔍 第二章：日志查证与微整形 (侦察兵)

### 1. 翻阅案底 (看日志)
这是排错时绝对的核心动作，比如你想知道微服务为什么启动失败了：
```bash
# 查看所有历史日志 (慎用，可能刷屏)
docker logs 容器名

# 🟢 进阶绝招：实时跟踪最新日志打印 (带上 -f)
docker logs -f 容器名

# 🟢 进阶绝招 2：由于日志太多，我只看最后 50 行
docker logs --tail 50 容器名
```

### 2. 潜入内部 (进入容器 Bash)
有时候你需要像进入虚拟机一样进入容器里头看看环境变量或者文件到底有没有挂载成功：
```bash
docker exec -it 容器名 /bin/bash
# (非常少数极其基础的镜像没有 bash，遇到报错则换成 /bin/sh 或者 sh 即可)
```
*进去看完之后想要退出来，直接敲 `exit` 或按 `Ctrl+D`。*

### 3. 隔空喊话 (执行单条命令)
不用进入容器交互界面，直接丢一条命令给它运行并返回结果给宿主机：
```bash
# 让 mysql 容器直接执行查询返回结果
docker exec leaf-mysql-master mysql -uroot -proot -e "SHOW DATABASES;"

# 获取 redis 容器里的某个键值
docker exec leaf-redis redis-cli GET order_count
```

---

## 🏘️ 第三章：Docker Compose 集群魔法 (大阵法)

当你的组件不是一个两个，而是一套体系（比如 3个RocketMQ Broker + 2个Nameserver）时，我们就不再使用单个 `docker` 命令了。

### 1. 一键拔地而起
在拥有 `docker-compose.yml` 配置文件的目录下，将里面编排好的多个容器一齐启动（且在后台运行）：
```bash
docker-compose up -d
```

### 2. 一键摧毁重塑
如果你的配置文件写错了，或者想彻底清理掉他们再重来：
```bash
docker-compose down
```
*(注意：这不仅仅是 Stop 容器，它会连同帮这批容器配置拉起来的网络环境都一起销毁，但通常不删除声明了 `Volumes` 持久化的真实硬盘数据。)*

### 3. 局部动刀
你在 compose 的文件里新加了一个叫做 `nacos` 的服务，但不想把原来的 MySQL 等重启，只想把新写的那个拉起来：
```bash
docker-compose up -d nacos
```

---

## 🧹 第四章：宿主机大扫除 (清道夫)

做久了研发，你的 Windows 硬盘空间会被各种废弃的历史镜像和退役容器吃掉几十个 G。**这一刀最解气**：

### 1. 终极一键大清洗
这个命令会移除所有：停止运行的容器、没有被容器使用的网络环境、以及悬空的 (没有 tag 名称且不被使用的) 残余系统镜像。
```bash
docker system prune
# (如果你想更绝一点，连同本地未使用过的正常拉取下载的镜像库也删除，极度节省空间，加上 -a 参数：docker system prune -a)
```

记住这份兵器谱，遇到排障卡住的时候掏出来看看，容器化的世界将为你敞开大门！
