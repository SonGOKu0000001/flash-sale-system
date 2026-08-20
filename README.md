# 限时抢购系统（Seckill System）

高并发秒杀场景的演示项目。后端采用 **Spring Boot 3 + MyBatis-Plus + MySQL + Redis/Redisson + RabbitMQ** 构建核心抢购链路，附带 **JMeter 压测脚本** 和 **Vue3 接口测试前端**。

> 完整的开发文档见 [限时抢购系统开发文档.md](./限时抢购系统开发文档.md)，包含环境搭建、数据库设计与建表 SQL、接口设计、启动与测试说明。

## 目录结构

```
限时抢购系统/
├─ demo_flash_sale/          # 后端 Spring Boot 服务
│  ├─ src/main/java/com/kami/demo/
│  │  ├─ config/             # RabbitMQ / 属性读取 / Web 配置
│  │  ├─ controller/         # 秒杀、用户接口
│  │  ├─ service/            # 核心业务（限流/锁/库存/下单/消息）
│  │  ├─ mapper/             # MyBatis-Plus Mapper
│  │  ├─ interceptor/        # IP 限流拦截器
│  │  ├─ entities/ vo/ dto/  # 实体 / 视图对象 / 传输对象
│  │  ├─ common/ exception/  # 统一响应与异常
│  └─ src/main/resources/
│     ├─ application.yml          # 本地配置（已 gitignore，不自带）
│     └─ application.yml.example  # 配置模板（需复制为 application.yml 后填写）
├─ 前端简易demo/             # Vue3 接口测试前端（仅用于测试接口，非业务系统）
├─ jmeter/                   # JMeter 压测脚本与参数数据
│  ├─ seckill_test.jmx       # 压测计划（3 个线程组）
│  └─ user.csv               # 参数化用户 ID（1000 个）
└─ 限时抢购系统开发文档.md    # 详细开发文档
```

## 后端技术选型与理由

| 技术 | 版本 | 选择理由 / 在本项目中的作用 |
|------|------|------------------------------|
| Spring Boot | 3.5（Java 21） | 快速开发、自动装配、内嵌容器，秒杀链路的业务编排框架 |
| MyBatis-Plus | 3.5.7 | 单表 CRUD 免写 XML，`LambdaQueryWrapper` 类型安全；负责活动、订单、商品、用户持久化 |
| MySQL 8 | — | 订单、活动、商品、用户数据的最终落库存储 |
| Redis + Lua 脚本 | — | 抢购核心：**库存预减原子扣减** 防止超卖；**令牌桶限流** 防止洪峰；抢购记录与结果缓存 |
| Redisson | 3.27 | 提供开箱即用的分布式锁 `RLock`，保证同一用户在同一个活动中只能抢购一次 |
| RabbitMQ | — | **异步下单解耦削峰**：抢购成功立即返回，订单由消费者异步创建；消息持久化 + TTL 死信队列保障可靠性 |
| Hutool | 5.8.25 | 雪花算法生成订单号（`SK` 前缀）及常用工具 |
| Lombok | — | 简化 POJO 样板代码 |

> 各技术栈在秒杀场景中的详细选型理由见 [开发文档 1.5 技术选型与理由](./限时抢购系统开发文档.md)

## 核心实现流程

整体链路：**接口限流 → 活动校验 → 分布式锁 → Redis 预减库存 → 发送 MQ → 异步下单**。

```
用户请求(/api/seckill/{activityId})
   │
   ▼
① 库存预热         SeckillStockInitializer 启动时将"进行中"活动的库存加载进 Redis
   ▼
② IP 限流          RateLimitInterceptor → RateLimiterService（Redis+Lua 令牌桶），
                   超过阈值直接返回 HTTP 429
   ▼
③ 活动校验         SeckillService 校验 status=1 且在 [开始时间, 结束时间) 内
   ▼
④ 分布式锁         SeckillLockService（Redisson RLock，key=userId:activityId），
                   获取失败返回"系统繁忙"
   ▼
⑤ 防重复抢购       锁内检查 seckill:record:{activityId}:{userId}，已抢购过则拒绝
   ▼
⑥ 预减库存         SeckillStockService 执行 Lua 脚本原子扣减：
                   返回 -1 未初始化 / 0 已售罄 / 1 扣减成功
   ▼
⑦ 发送 MQ          SeckillMessageProducer 发送持久化消息到 seckill.queue
   ▼  （同时缓存"抢购成功"结果，返回 200，前端可轮询查询）
⑧ 异步下单         SeckillMessageConsumer 消费消息 → OrderService：
                     · 幂等查重（存在未取消订单则跳过，支持取消后重新抢购）
                     · 乐观扣减 MySQL stock_count（限制 stock>0，杜绝库存为负）
                     · 雪花算法生成订单号，写入 t_order
                     · 更新抢购结果缓存（带订单号，前端轮询命中后停止）
                     · 库存不足异常 → basicNack 不重投；瞬时异常 → requeue 重试
   ▼
⑨ 死信队列兜底     seckill.queue 30s 未消费 → 死信队列 seckill.dlx.queue，
                   SeckillDlxConsumer 记录告警日志并手动确认，避免消息无限堆积
                   （死信消息 = 用户已抢购成功但订单 30s 内未创建成功，需人工介入）
```

**多层防超卖**：Lua 预减库存 + 活动状态校验 + MySQL 乐观扣减 + 订单幂等查重，四道防线保证零超卖。

## 接口清单

统一返回结构 `Result { code, msg, data }`；业务用请求头 `X-User-Id` 标识用户（缺省时默认用户 1）。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/seckill/activity/list` | 活动列表（含剩余库存，按开始时间升序） |
| GET | `/api/seckill/activity/{activityId}` | 单个活动详情 |
| POST | `/api/seckill/{activityId}` | 发起抢购（限流 + 锁 + 预减库存 + 发 MQ） |
| GET | `/api/seckill/result/{activityId}` | 查询抢购结果（轮询接口） |
| GET | `/api/user/list` | 用户列表（供测试前端下拉选择） |

## JMeter 压测

`jmeter/` 下两个文件：

- **`seckill_test.jmx`**：压测计划，包含 3 个线程组：
  - `01-功能验证`：5 线程 × 1 次，验证接口连通与基础逻辑
  - `02-并发压测`：1000 线程 × 1 次，模拟高并发抢购，是主要压测场景
  - `03-重复抢购`：单用户连点 50 次，验证防重复抢购逻辑
  - 已配置断言 `HTTP 200|429`，被限流拒绝视为成功响应
- **`user.csv`**：参数化数据，1000 个用户 ID，通过 CSV Data Set 注入 `X-User-Id` 请求头（`${userId}` 变量），模拟不同用户并发。

### 压测前准备

1. 启动 MySQL / Redis / RabbitMQ，并根据 `application.yml.example` 配置好 `application.yml` 后启动后端
2. 重置数据（保证测的是理想状态）：
   - MySQL：清空 `t_order`，重置活动表 `stock_count`
   - Redis：`FLUSHDB` 后再设置 `SET seckill:stock:{activityId} 10000`（业务中也可让库存预热器自动加载，前提是活动为"进行中"且时间在有效期内）
3. 测核心逻辑时，可临时把 `application.yml` 中限流 `rate`/`burst` 调高（如 10000）以免限流干扰库存逻辑，测完还原
4. 如需换活动，修改测试计划中的 `activityId` 变量（默认 2）；`baseHost`/`basePort` 默认 `localhost:10086`

### 运行方式

**GUI 方式（推荐初学者）**

```
JMeter 安装目录/bin/jmeter.bat
→ File → Open → 选择 jmeter/seckill_test.jmx
→ 点绿色启动按钮
→ 查看聚合报告 / 汇总报告
```

> 大并发时建议右键禁用"察看结果树"，避免界面渲染拖慢压测机。

**命令行方式（建议正式压测）**

```bash
jmeter -n -t seckill_test.jmx -l result.jtl -j jmeter.log
# 参数说明：-n 无界面模式  -t 测试计划  -l 结果文件  -j 日志
```

CLI 模式无界面开销，压测结果更准确；结束后可用 GUI 打开 `result.jtl` 查看聚合报告。

## 前端简易demo

> **重要**：该前端仅用于**接口测试与效果演示**，不承担任何业务职责，也不是正式的用户端。

功能：从 `/api/user/list` 选择当前用户 → 查看活动列表（含秒杀价与实时剩余库存）→ 一键发起抢购 → 轮询 `/api/seckill/result/{id}` 展示下单结果。实现上通过 Vite 代理把 `/api` 转发到 `localhost:10086`。

运行方式：

```bash
cd 前端简易demo
npm install
npm run dev
# 浏览器访问 http://localhost:5173
```

技术栈：Vue 3 + Vite + naive-ui + axios。

## 快速开始

前置环境：JDK 21、Maven、MySQL 8、Redis、RabbitMQ（默认 guest/guest）。

1. 复制配置文件并填写环境信息

   ```bash
   copy demo_flash_sale\src\main\resources\application.yml.example demo_flash_sale\src\main\resources\application.yml
   # 填写 MySQL 地址/账号/密码、Redis 地址
   ```

2. 导入数据库表结构并初始化活动/商品/用户数据（建表 SQL 见开发文档「数据库设计」章节）

3. 启动后端

   ```bash
   cd demo_flash_sale
   mvn spring-boot:run
   ```

4. （可选）启动测试前端按上文操作，或直接用 Postman/JMeter 调用接口

## 已知事项与注意事项

- **演示性质的无鉴权**：当前通过请求头 `X-User-Id` 标识用户，未做登录鉴权，可被伪造。这是为了简化接口测试，生产环境必须引入真实的用户认证与鉴权体系。
- **限流粒度**：令牌桶按调用方 IP 限流（默认 100 QPS），经 Nginx 反向代理时需透传 `X-Forwarded-For`，否则全部流量都算作网关 IP。
- **配置脱敏**：真实 `application.yml` 已加入 `.gitignore`（含数据库密码），仓库中只保留 `application.yml.example` 模板，克隆后需自行创建。
- **幂等性说明**：幂等与防超卖由四层防线保障（入口分布式锁 + Redis 已抢购标记 / Redis Lua 预减库存 / 消费者 `selectCount` 查重 / MySQL 条件扣减），满足演示级需求；未引入独立幂等表，详细分析见 [开发文档 4.7 幂等性设计分析](./限时抢购系统开发文档.md)。
- **JMeter 注意**：`user.csv` 通过 `${__testPlanFileDir()}` 引用与脚本同目录的文件，请保证两个文件放在同一目录；测试计划中的 `baseHost`/`basePort` 与 `activityId` 按需修改。
- **改进建议**：后续可将数据库密码等敏感配置迁移到环境变量或 Spring Profile 管理；消息发送失败可增加本地补偿表兜底；秒杀结果缓存与数据库的最终一致性建议结合对账任务定期核对。