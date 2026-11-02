# 短视频创作者素材与收益结算管理系统

软件项目管理课程实验的 Java 21 核心演示。当前是控制台程序，不是完整网站。无需 Maven、数据库服务或第三方依赖。

## 已实现功能

素材分类登记及按创作者查询；按创作者和月份汇总收益；示例服务费规则；结算单草稿、确认、登记付款状态；重复收入、重复结算和非法状态流转检查。

RevenuePolicy 是接口，AbstractRevenuePolicy 是抽象类，ServiceFeePolicy 是具体子类。金额采用 BigDecimal，示例规则先将服务费四舍五入到分，再以总额减去服务费。示例费率10%，不是实际平台或税务规则。

## 在 IDEA 中运行

打开本目录，项目 SDK 选择 JDK 21。将 src 标记为 Sources Root。打开 svms/Main.java，点击 main 方法旁绿色运行按钮。打开 CoreTests.java 同样运行，检查业务测试结果。

如果 IDEA 没有自动识别，可通过“新建项目→Java→JDK 21”建立空项目，然后把本目录 src 下的 svms 文件夹复制到该项目 src。

## 命令行运行

在本目录运行（javac/java 未配置 PATH 时使用电脑 JDK 21 的完整路径）：

```powershell
javac -encoding UTF-8 -d out src/svms/Main.java src/svms/CoreTests.java
java -cp out svms.Main
java -cp out svms.CoreTests
```

演示总收入1250.50元，服务费125.05元，应结算1125.45元。PAID只是演示状态，不产生真实付款。

## 实现边界

数据只在内存，重启不保留。没有登录鉴权、网页、数据库、实际素材文件存储、第三方收益采集及支付接入；不支持结算后补录或冲销。所有这些属于后续计划，不能写成已实现功能。本程序没有验证1万并发，也不能作为该性能目标已经达成的证明。

## 仓库与协作

目标仓库：https://github.com/a3030765513-oss/SoftwareProjectManagment 。实验中的三次提交应对应真实的分步修改；分支、Pull Request、审查及共享代码应以平台实际记录为准。
