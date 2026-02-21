# Jenkins 离线与网络问题踩坑及解决方案笔记

在微服务 CI/CD 流水线搭建的过中，搭建 Jenkins 基础环境时经常会遇到网络相关的安装大坑，特此记录核心流程和解决方案，以备后续复盘参考。

## 方案选型变迁
由于机器资源的分配问题，起初我们选择的是 `Jenkins in Docker`：
但 Docker 环境内部是一个相对隔离的 Linux，默认不走宿主机配置的全局代理（如 Clash TUN 模式），直接导致 Jenkins 安装时死死卡在拉取外网更新包的阶段。

后期转为直接利用宿主机的 **Windows Native 离线服务版本 (war)**，它可以与宿主机共享网络网络策略（能顺利被 TUN 流量劫持），有效绕开海外长城防火墙。

---

## 避坑指南一：初始安装卡死在“加载中” / 实例离线

Jenkins 的原罪在于它启动初始化向导时，会去拉取 `updates.jenkins.io` (这个域名国内极大概率超时)。这会导致长时间卡在安装加载页甚至直接报“该 Jenkins 实例已离线”。

**解决思路：**
1. **优先跳过它**：如果安装时提示“离线”，千万不要硬装，直接点击 **“跳过插件安装(Skip Plugin Installation)”**。这样可以直接先创建一个本地管理账户，强制让你进入到 Jenkins 后台主页，再图后续。
2. **替换代理源为清华镜像站**：
   - 进入系统后的路径：`系统管理 (Manage Jenkins)` -> `插件管理 (Plugins)` -> 滚动到底部的 `高级设置 (Advanced settings)`
   - 将“Update Site” URL 替换为：`http://mirrors.tuna.tsinghua.edu.cn/jenkins/updates/update-center.json` (注意使用 HTTP 避免底层的 CA 证书异常阻断)。

## 避坑指南二：Docker 与 Native 网络 HTTPS 证书拦截

就算用了清华源，有的时候因为科学上网软件开启了 TUN 模式抓取全局流量，导致代理证书与 Java 环境内部颁发的数字凭证握手失败（报错类似：PKIX path building failed 或 CRYPT_E_REVOCATION_OFFLINE），依然会导致在面板点击 `Install` 包大片红叉。

**解决策略：全手动上传装包法**
1. 用自己畅通无阻的宿主机浏览器自行前往 Jenkins Plugins 中心: `https://updates.jenkins.io/download/plugins/` 
2. 搜索你需要插件（比如 `maven-plugin.hpi` / `git.hpi` / `workflow-aggregator.hpi`）并直接下载到本地电脑硬盘。
3. 从 `插件管理` -> `高级设置`，找到 **部署(Deploy) 或者 上传(Upload)** 功能，直接将本地扩展名为 `.hpi` 的包传进去生效。
这种方式被称为最后的物理外挂，极其稳定！

---

## 避坑指南三：Windows 本地服务版的起停管理

Windows 本地安装版的 Jenkins 一般通过服务守护，不要使用系统的 `net start`（权限控制严格极易报错 `系统错误 5 拒绝访问`）。

请直接进入安装目录（由于您的在 `E:\jenkins`），并调用它专门提供的服务包裹工具 `jenkins.exe` 执行：

*   **停止 Jenkins：** `jenkins.exe stop`
*   **启动 Jenkins：** `jenkins.exe start`
*   **重启 Jenkins：** `jenkins.exe restart`

## 避坑指南四：Jenkins 升级替换 War 包导致闪退

当下载了属于新版本的 `jenkins.war` 这个 90多 MB 的文件去强行覆盖老版本的程序试图升级时，如果点击启动发生自动闪退。需要第一时间看日志大抵是因为 **JDK 版本不匹配**（新版 Jenkins 要求至少 JDK 17 及以上）。

**解决方案：修改底层驱动器配置 `jenkins.xml`**
在 `E:\jenkins\jenkins.xml` 文件中，可以配置其后台运行所指定的 Java 环境位置及分配的运行内存：
```xml
  <!-- 分配的运行时内存建议至少给 512m 以上 -->
  <!-- 这里的 executable 就是指明用哪个 Java 来跑最新的 War 包，老版的通常是 11，要手动把它改成系统里高版本的 21 -->
  <executable>D:\Program Files\Java\jdk21\bin\java.exe</executable>
  <arguments>-Xrs -Xmx512m -Dfile.encoding=utf-8 ...</arguments>
```
修改完成后，回到命令行敲 `jenkins.exe restart` 即可热更新配置实现升级启动。

---
## 最终必装的三剑客插件推荐：
在解决好网络后，要想搞定基于 Java/Maven 和 Pipeline 流程自动化的后端项目，最起码的必备插件组合：
1. **Git / Git client**：处理 GitHub/Gitee 代码拉取
2. **Maven Integration**：自动化的 Java 编译组件
3. **Pipeline (workflow-aggregator)**：核心流水线功能引擎（可以通过写 Jenkinsfile 代码一样执行发布策略）
