# spring-boot-health-sample

运行状况检查提供了一种简单的机制，用于确定应用程序是否在正常运行。Spring Boot 的健康检查端口通过 HTTP 调用，并使用标准返回码来指示 `UP` 或 `DOWN` 状态。云环境（比如 Kubernetes 或 Mesos）可以定期轮询健康检查端口，以确定服务实例是否已经正常启动，并且已经准备好接受流量。

Spring Boot 使用 `starter-actuator` 提供健康检查端口，端口位置在 `/actuator/health`. 在程序中引入 `starter-actuator`:

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

在 `application.properties` 中开启健康检查端口，并开启详细信息：

```
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=ALWAYS
```

启动后能够查看到最基本的健康状态：

```
$ curl -i http://localhost:8080/actuator/health
HTTP/1.1 200
Content-Type: application/vnd.spring-boot.actuator.v2+json;charset=UTF-8

{
    "status": "UP",
    "details": {
        "diskSpace": {
            "status": "UP",
            "details": {
                "total": 250790436864,
                "free": 99883393024,
                "threshold": 10485760
            }
        }
    }
}
```

基本健康检查项包括 `DiskSpaceHealthIndicator`，检查主机的磁盘空间。

实际程序运行的时候要依赖很多外部资源，使用 Spring Boot Starter 引入这些依赖，会在健康检查端口自动集成相应的检查项。Spring Boot 可以自动配置以下健康检查项：

- CassandraHealthIndicator
- DiskSpaceHealthIndicator
- DataSourceHealthIndicator
- ElasticsearchHealthIndicator
- JmsHealthIndicator
- MailHealthIndicator
- MongoHealthIndicator
- RabbitHealthIndicator
- RedisHealthIndicator
- SolrHealthIndicator

有时候也需要根据需求自定义健康检查项，示例程序中提供了自定义健康检查的例子，`MyHealthIndicator`:

```java
public Health health() {
	Builder builder = null;

	try {
		Process process = Runtime.getRuntime().exec(command);
		process.waitFor();
		
		List<String> stdout = readLines(process.getInputStream());
		List<String> stderr = readLines(process.getErrorStream());
		int exitValue = process.exitValue();
		LOG.debug("exitValue: {}", exitValue);

		if (exitValue == 0) {
			builder = Health.up();
		} else {
			builder = Health.down();
		}

		builder.withDetail("exitValue", exitValue).withDetail("stdout", stdout).withDetail("stderr", stderr);
	} catch (Exception e) {
		builder = Health.down(e);
	}

	builder.withDetail("command", command);
	return builder.build();
}
```

`MyHealthIndicator` 运行一个命令行判断程序的健康状态，命令行在配置文件中定义：

```
my.health.indicator.command=curl -If http://www.baidu.com
```

> `curl` 用来做健康检查时要加 `-f` 参数，`-f`(`--fail`) 把 HTTP 错误作为错误码返回到父进程。如果没有 `-f` 参数，`curl` 永远返回 `0`.

> 按照 UNIX 惯例，正常结束的程序返回 `0`，错误结束的程序返回某个错误代码。
> https://www.gnu.org/software/libc/manual/html_node/Exit-Status.html

运行程序，探测一下监控检查端口：

```
$ curl -i http://localhost:8080/actuator/health
HTTP/1.1 200
Content-Type: application/vnd.spring-boot.actuator.v2+json;charset=UTF-8
Transfer-Encoding: chunked

{
    "status": "UP",
    "details":
    {
        "my":
        {
            "status": "UP",
            "details":
            {
                "exitValue": 0,
                "stdout":
                [
                    "HTTP/1.1 200 OK",
                    "Accept-Ranges: bytes",
                    "Content-Length: 277",
                    "Content-Type: text/html",
                    ""
                ],
                "stderr":
                [
                    "  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current",
                    "                                 Dload  Upload   Total   Spent    Left  Speed",
                    "",
                    "  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0",
                    "  0   277    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0"
                ],
                "command": "curl -If http://www.baidu.com"
            }
        },
        "diskSpace":
        {
            "status": "UP",
            "details":
            {
                "total": 250790436864,
                "free": 98924347392,
                "threshold": 10485760
            }
        }
    }
}
```

一切正常，健康检查端口返回 HTTP 状态 `200`，服务是 `UP` 状态。

现在我们故意制造一个错误，比如把网络断开，然后再探测一下健康检查端口：

```
$ curl -i http://localhost:8080/actuator/health
HTTP/1.1 503
Content-Type: application/vnd.spring-boot.actuator.v2+json;charset=UTF-8
Transfer-Encoding: chunked

{
    "status": "DOWN",
    "details":
    {
        "my":
        {
            "status": "DOWN",
            "details":
            {
                "exitValue": 6,
                "stdout":
                [
                ],
                "stderr":
                [
                    "  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current",
                    "                                 Dload  Upload   Total   Spent    Left  Speed",
                    "",
                    "  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0",
                    "curl: (6) Could not resolve host: www.baidu.com"
                ],
                "command": "curl -If http://www.baidu.com"
            }
        },
        "diskSpace":
        {
            "status": "UP",
            "details":
            {
                "total": 250790436864,
                "free": 98903023616,
                "threshold": 10485760
            }
        }
    }
}
```

健康检查失败，端口返回 HTTP 状态 `503`，服务是 `DOWN` 状态。
