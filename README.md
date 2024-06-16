# Maven的自动部署插件

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