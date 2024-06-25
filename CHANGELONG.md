# 自动部署插件更新日期

## 1.x版本

### 1.0

第一个发行的正式版本，可以实现普通程序包的上传与部署，使用如下

🌽通过配置，可以在mvn package时自动将打好的包上传至指定服务器，并在上传前后执行一些脚本命令，基于此实现自动上传部署

目前只支持普通程序包（比如jar和war）的上传与部署



🍏使用方法

比如有个Maven工程`parent`，parent工程有两个子工程A和B，希望上传A工程package出来的程序包

首先在A工程所属的pom.xml文件中引入插件

```xml
<build>
    <plugins>
        <!--引入插件坐标-->
        <plugin>
            <groupId>org.az.yangtong</groupId>
            <artifactId>auto-deploy-maven-plugin</artifactId>
            <version>1.0</version>
        </plugin>
	</plugins>
</build>
```



每个插件都有一个执行目标，配置该插件的执行目标

```xml
<plugin>
    <!--引入插件坐标-->
    <groupId>org.az.yangtong</groupId>
    <artifactId>auto-deploy-maven-plugin</artifactId>
    <version>1.0</version>
    <!--插件执行目标-->
    <executions>
        <execution>
            <!--目标id，这个随便写-->
            <id>auto-deploy</id>
            <!--目标，目前只有一个目标auto-deploy，该目标绑定的Maven生命周期阶段是package-->
            <!--也就是说，运行mvn package时就会自动运行该插件的auto-deploy-->
            <goals>
                <goal>auto-deploy</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```



要上传package出来的包，总得让插件知道服务器地址，以及登录的账号密码吧

```xml
<plugin>
    <!--引入插件坐标-->
    <groupId>org.az.yangtong</groupId>
    <artifactId>auto-deploy-maven-plugin</artifactId>
    <version>1.0</version>
    <!--插件执行目标-->
    <executions>
        <execution>
            <id>auto-deploy</id>
            <goals>
                <goal>auto-deploy</goal>
            </goals>
        </execution>
    </executions>
    <!--插件参数-->
    <configuration>
        <!--服务器ip，必填-->
        <host>192.168.1.220</host>
        <!--ssh端口，默认22，选填-->
        <port>22</port>
        <!--登录账号，必填-->
        <user>root</user>
        <!--登录密码，必填-->
        <password>Dev@62628816</password>
        <!--上传到服务器的哪个文件夹下，必填-->
        <remotePath>/home/test</remotePath>
        <!--前置命令，在程序包上传之前执行的命令，选填，比如杀死进程什么的-->
        <beforeCommands>
            <command>ps -ef|grep redis</command>
            <command>./run.sh stop</command>
        </beforeCommands>
        <!--后置命令，在程序包上传之后执行的命令，选填，比如运行启动脚本什么的-->
        <afterCommands>
            <command>ls -l</command>
            <command>./run.sh start</command>
        </afterCommands>
    </configuration>
</plugin>
```



🍊运行`mvn clean package`命令，会自动运行插件，步骤如下

1. 依次运行前置命令，所有命令都在`remotePath`目录下执行，如果有一个命令运行失败，package结束
2. 定位到当前工程下package出来的包，使用`sftp`上传到指定服务器的指定目录下
3. 一次运行后置命令，所有命令都在`remotePath`目录下执行，如果有一个命令运行失败，package结束
4. package成功结束



注意：

执行命令前会加载环境变量，确保你的环境变量是配置在以下的几个文件中

```shell
source /etc/environment; source /etc/profile; source ~/.bashrc; source ~/.profile
```



### 1.1

增加如下功能：

* 优化文件上传时的进度条显示，以及增加上传速率
* command命令间可以增加分号`;`，插件会将以分号分隔的命令解析成多条命令依次执行

这里介绍下command标签里的命令如何使用分号分隔，使用如下

```xml
<beforeCommands>
	ps -ef|grep redis;
	./run.sh stop;
</beforeCommands>
```

这等价于之前的

```xml
<beforeCommands>
	<command>ps -ef|grep redis</command>
	<command>./run.sh stop</command>
</beforeCommands>
```



### 1.2

增加如下功能：

* 只有在特定环境下运行`mvn package`命令才能触发插件的自动部署行为

之前一旦引入插件，package时就会自动触发自动部署行为，对于有些不希望自动部署的情况必须移除该插件才能正常package，这里对此进行了优化

package时，只有当profiles包含了`auto-deploy`，才能触发插件行为，比如

```shell
# 有3个profiles，其中包含了auto-deploy，可以触发插件行为
mvn package -P prod,dev,auto-deploy
```

对应工程的pom.xml如下

```xml
<profiles>
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
    </profile>
    <profile>
        <id>auto-deploy</id>
    </profile>
    <profile>
        <id>prod</id>
    </profile>
</profiles>
```



### 1.3

对于某些服务器，不允许使用root账号登录，而是使用普通账号登录，然后使用`sudo`来执行命令

增加如下功能

* 可以选择前置命令和后置命令是否以`sudo`模式执行

当满足下面3个条件时，会以sudo模式执行命令

1. 登录账号不为root
2. 当前执行的命令不是`cd`（因为cd命令不需要配合sudo）
3. 命令以`sudo `开头，或者插件配置的`isSudo`标签设置为了true

比如下面这个配置

```xml
<configuration>
    <host>192.168.1.123</host>
    <user>az</user>
    <password>az123</password>
    <remotePath>/home/az</remotePath>
    <!--是否已sudo模式执行前置、后置命令，选填，默认false-->
    <isSudo>true</isSudo>
    <beforeCommands>
        ps -ef|grep redis
    </beforeCommands>
    <afterCommands>
        cd /home;
        sudo /home/httech/sso/customer-run.sh restart
    </afterCommands>
</configuration>
```



如果插件进行了如上的配置，执行流程如下所示

1. 先以sudo模式执行前置命令，并以`password`标签的值作为sudo密码

   ```shell
   #真正执行的linux命令如下，以"az123"作为命令的密码
   sudo -S -p '' ps -ef|grep redis
   ```

   并且会打印：`执行命令(sudo模式)：ps -ef|grep redis`

2. 上传package出来的程序包

3. 最后以sudo模式执行后置命令

   由于`cd`命令不需要配合sudo，所以当插件发现命令是`cd`，不会做额外处理

   ```shell
   #真正执行的linux命令如下
   cd /home
   ```

   然后在执行下一个命令，发现下一个命令以`sudo `开头，所以会以sudo模式执行（就算isSudo设为false也会以sudo模式执行）

   ```shell
   #真正执行的linux命令如下，将"sudo "替换成了"sudo -S -p '' "
   sudo -S -p '' /home/httech/sso/customer-run.sh restart
   ```

   

   
