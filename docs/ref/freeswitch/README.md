# FreeSWITCH mod_xml_curl 配置说明

本项目使用 mod_xml_curl 实现动态分机配置，FreeSWITCH 会通过 HTTP 请求从 Spring Boot 服务获取分机信息。

## 配置步骤

### 1. 创建 xml_curl 配置文件

在 FreeSWITCH 配置目录中创建或编辑 `xml_curl.conf.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration name="xml_curl.conf" description="XML Curl Configuration">
  <settings>
    <param name="timeout" value="10"/>
    <param name="connect-timeout" value="5"/>
    <param name="enable- Preserve-backslashes" value="false"/>
  </settings>
  <bindings>
    <binding name="directory">
      <param name="gateway-url" value="http://springboot:8081/api/fs/directory"/>
      <param name="method" value="POST"/>
      <param name="timeout" value="10"/>
    </binding>
    <binding name="dialplan">
      <param name="gateway-url" value="http://springboot:8081/api/fs/dialplan"/>
      <param name="method" value="POST"/>
      <param name="timeout" value="10"/>
    </binding>
    <binding name="configuration">
      <param name="gateway-url" value="http://springboot:8081/api/fs/configuration"/>
      <param name="method" value="POST"/>
      <param name="timeout" value="10"/>
    </binding>
  </bindings>
</configuration>
```

### 2. 修改 sofia 配置启用 XML curl

在 `sofia.conf.xml` 中确保以下配置存在：

```xml
<configuration name="sofia.conf" description="Sofia SIP Stack">
  <global_settings>
    <param name="log-level" value="0"/>
    <param name="debug-presence" value="0"/>
  </global_settings>
  <profiles>
    <profile name="internal">
      <settings>
        <param name="sip-config" value="enabled"/>
        <param name="force-register-domain" value="$${domain}"/>
        <param name="force-subscription-domain" value="$${domain}"/>
        <param name="force-register-db-domain" value="$${domain}"/>
        <!-- 启用 XML curl 获取用户目录 -->
        <param name="odbc-dsn" value=""/>
      </settings>
    </profile>
  </profiles>
</configuration>
```

### 3. 启用 mod_xml_curl 模块

在 `autoload_configs/modules.conf.xml` 中确保以下行存在：

```xml
<load module="mod_xml_curl"/>
```

### 4. Docker 网络配置

确保 FreeSWITCH 和 Spring Boot 在同一 Docker 网络中，docker-compose 配置示例：

```yaml
version: "3.8"

services:
  freeswitch:
    image: registry.cn-beijing.aliyuncs.com/wenning/freeswitch:1.10.12
    container_name: freeswitch
    network_mode: host
    volumes:
      - ./conf:/etc/freeswitch
    restart: unless-stopped

  springboot:
    build: ./springboot
    container_name: springboot
    network_mode: host
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    restart: unless-stopped
```

如果使用自定义网络：

```yaml
version: "3.8"

services:
  freeswitch:
    image: registry.cn-beijing.aliyuncs.com/wenning/freeswitch:1.10.12
    container_name: freeswitch
    networks:
      - voip-network
    volumes:
      - ./conf:/etc/freeswitch
    restart: unless-stopped

  springboot:
    build: ./springboot
    container_name: springboot
    networks:
      - voip-network
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    restart: unless-stopped

networks:
  voip-network:
    driver: bridge
```

### 5. 验证配置

重启 FreeSWITCH 容器后，检查日志确认 mod_xml_curl 正常工作：

```bash
docker logs freeswitch 2>&1 | grep xml_curl
```

正常启动日志应包含：

```
[INFO] mod_xml_curl.c:462 XML Curl interface (directory) enabled.
[INFO] mod_xml_curl.c:462 XML Curl interface (dialplan) enabled.
[INFO] mod_xml_curl.c:462 XML Curl interface (configuration) enabled.
```

## Spring Boot 接口说明

### 分机目录接口

- **路径**：POST /api/fs/directory
- **Content-Type**：application/x-www-form-urlencoded
- **Accept**：application/xml

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user | String | 是 | 分机号 |
| domain | String | 否 | 域名，默认为 default |
| action | String | 否 | 请求类型，如 sip_auth |

**响应示例**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<document type="freeswitch/xml">
  <section name="directory">
    <domain name="default">
      <user id="1001">
        <params>
          <param name="password" value="1234"/>
        </params>
        <variables>
          <variable name="user_context" value="default"/>
          <variable name="effective_caller_id_name" value="张三"/>
          <variable name="effective_caller_id_number" value="1001"/>
        </variables>
      </user>
    </domain>
  </section>
</document>
```

### 分机管理 REST API

所有接口基础路径：`/api`

#### 1. 创建分机

**接口**：POST /api/extensions

**请求参数**（JSON）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| extensionNumber | String | 是 | 分机号，3-10位数字 |
| password | String | 是 | 密码，4-20位 |
| displayName | String | 是 | 显示名称，最大50字符 |
| context | String | 是 | 上下文，最大50字符 |

**请求示例**：

```bash
curl -X POST http://localhost:8081/api/extensions \
  -H "Content-Type: application/json" \
  -d '{
    "extensionNumber": "1002",
    "password": "1234",
    "displayName": "李四",
    "context": "default"
  }'
```

**响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 2,
    "extensionNumber": "1002",
    "displayName": "李四",
    "status": "OFFLINE",
    "context": "default",
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-01-15T10:30:00"
  }
}
```

#### 2. 查询分机列表

**接口**：GET /api/extensions

**响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "extensionNumber": "1001",
      "displayName": "张三",
      "status": "ONLINE",
      "context": "default",
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T10:00:00"
    },
    {
      "id": 2,
      "extensionNumber": "1002",
      "displayName": "李四",
      "status": "OFFLINE",
      "context": "default",
      "createdAt": "2026-01-15T10:30:00",
      "updatedAt": "2026-01-15T10:30:00"
    }
  ]
}
```

#### 3. 查询单个分机

**接口**：GET /api/extensions/{id}

#### 4. 更新分机

**接口**：PUT /api/extensions/{id}

请求参数同创建分机。

#### 5. 删除分机

**接口**：DELETE /api/extensions/{id}

#### 6. 查询分机状态

**接口**：GET /api/extensions/{id}/status

**响应示例**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "extensionNumber": "1001",
    "displayName": "张三",
    "status": "ONLINE",
    "context": "default"
  }
}
```

#### 7. 分机发起外呼

**接口**：POST /api/extensions/{id}/dial

**请求参数**（JSON）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| destination | String | 是 | 目标号码 |
| callerIdNumber | String | 否 | 主叫号码，默认使用分机号 |
| callerIdName | String | 否 | 主叫名称，默认使用分机显示名 |

**请求示例**：

```bash
curl -X POST http://localhost:8081/api/extensions/1/dial \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "1002"
  }'
```

## 故障排查

### 问题：分机无法注册

**检查项**：

1. 确认 Spring Boot 服务正常运行且能访问
2. 检查防火墙是否阻止了 8081 端口
3. 查看 FreeSWITCH 日志中的详细错误信息

```bash
docker logs freeswitch 2>&1 | tail -100
```

### 问题：XML 解析错误

**检查项**：

1. 确认返回的 XML 格式正确
2. 检查 XML 声明和特殊字符是否正确转义
3. 确认 Content-Type 响应头为 application/xml

### 问题：超时错误

**检查项**：

1. 检查 Spring Boot 服务响应时间
2. 调整 xml_curl.conf.xml 中的 timeout 参数
3. 检查数据库查询性能

## 相关文档

- [分机管理任务](../tasks/003-分机管理.md)
- [FreeSWITCH ESL 连接配置](../tasks/003-FreeSWITCH-ESL连接配置.md)
