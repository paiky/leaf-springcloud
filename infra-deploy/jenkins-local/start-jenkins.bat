@echo off
echo =========================================
echo    Starting Jenkins Local Server
echo =========================================
REM 设置当前脚本所在目录的 data 文件夹作为 Jenkins 工作目录
set JENKINS_HOME=%~dp0data

REM 使用系统环境变量中的 java 运行 Jenkins，并指定使用 8083 端口
java -Xmx1024m -Xms512m -jar jenkins.war --httpPort=8083

pause
