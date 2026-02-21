pipeline {
    agent any

    // 全局环境变量
    environment {
        // 项目名称
        PROJECT_NAME = 'leaf-springcloud'
        // Docker 镜像前缀
        DOCKER_IMAGE_PREFIX = 'leaf'
        // 需要构建的微服务模块列表
        MODULES = 'leaf-gateway,leaf-service-user,leaf-service-order'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '>>> 1. 拉取代码'
                // 注意：这里默认使用你在 Jenkins 配置任务时选择的 Git 源
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                echo '>>> 2. Maven 编译打包 (跳过测试)'
                // 执行 Windows CMD 的 maven 打包命令 (因为使用的是 Windows Native Jenkins)
                // 如果环境变量没配置 M2_HOME，可以改成绝对路径如 D:\\apache-maven-x.x.x\\bin\\mvn
                bat 'mvn clean package -am -pl %MODULES% -DskipTests'
            }
        }

        stage('Docker Image Build') {
            steps {
                echo '>>> 3. 构建微服务 Docker 镜像'
                // 分别为每一个微服务打镜像
                // 前提条件是 Jenkins 服务运行用户 (如 SYSTEM 或你的电脑账号) 可以执行 docker 命令
                bat '''
                    echo "Building leaf-gateway image..."
                    docker build -t %DOCKER_IMAGE_PREFIX%/leaf-gateway:latest -f Dockerfile --build-arg JAR_FILE=leaf-gateway/target/*.jar .
                    
                    echo "Building leaf-service-user image..."
                    docker build -t %DOCKER_IMAGE_PREFIX%/leaf-service-user:latest -f Dockerfile --build-arg JAR_FILE=leaf-service-user/target/*.jar .
                    
                    echo "Building leaf-service-order image..."
                    docker build -t %DOCKER_IMAGE_PREFIX%/leaf-service-order:latest -f Dockerfile --build-arg JAR_FILE=leaf-service-order/target/*.jar .
                '''
            }
        }

        stage('Clean Up') {
            steps {
                echo '>>> 4. 清理旧版本及冗余镜像 (可选)'
                bat '''
                    docker image prune -f
                '''
            }
        }
    }

    // 构建后的操作
    post {
        success {
            echo '🎉 构建成功！您的微服务镜像已经躺在 Docker 本地仓库了！'
        }
        failure {
            echo '❌ 构建失败！请检查编译日志或 Docker 权限。'
        }
    }
}
