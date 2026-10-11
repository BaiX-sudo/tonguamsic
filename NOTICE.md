# NOTICE

本文件说明「冬瓜音乐 · 二创开源版」的来源构成、权利归属与许可边界。发布本仓库前请完整阅读。

---

## 1. 上游项目

| 项 | 内容 |
|---|---|
| 应用名称 | 冬瓜音乐（Donggua Music） |
| 包名 | `com.xifeng.music` |
| 版本 | `1.0.7`（versionCode 26） |
| 原作者 | 见原版应用内「关于我们」页面的署名信息 |
| 二次分发者 | [BaiX-sudo](https://github.com/BaiX-sudo) |

原版客户端的著作权归原作者所有。本仓库是该客户端的**二次分发版本**，不是原作者发布的官方开源项目。

### 关于 GPL 声明的边界

GPL-3.0 允许任何人自由使用、修改和再分发**已经以 GPL 授权**的作品。以 GPL 名义再分发他人作品，前提是原始权利人已经作出该授权。

因此本仓库的准确表述是：

- 本仓库中**由二次分发者新增和修改的代码**（`src/` 下的 3 个 Java 文件、`patches/` 下的 2 处补丁、`build/` 下的构建脚本）以 GPL-3.0 授权；
- **原版客户端的代码本身**（`com.xifeng.music` 包的 smali、资源、内置第三方库与原生库）**未因本仓库的发布而改变其原有的权利归属**，仍由原作者及各自组件的著作权人主张权利；
- 如果原作者后续以其他条款授权或重新发布原版代码，二次分发者应同步调整本仓库的表述。

本仓库未包含原版的完整解包结果、原生 `.so` 库与成品 APK，正是为了避免上述权利边界被模糊。

## 2. 原版客户端的技术构成（据解包分析）

原版客户端并非从零开发，而是在以下开源项目的基础上构建：

| 组件 | 说明 | 许可 |
|---|---|---|
| **InnerTune** | 应用主体骨架、YouTube Music / PipeTube 播放与界面逻辑 | GPL-3.0 |
| **Kotlin / kotlinx.serialization / Ktor** | 语言与序列化、网络框架 | Apache-2.0 |
| **AndroidX / Jetpack Compose** | UI 框架 | Apache-2.0 |
| **OkHttp / Okio** | HTTP 客户端 | Apache-2.0 |
| **Coil** | 图片加载 | Apache-2.0 |
| **Timber** | 日志 | Apache-2.0 |
| **com.github.houbb** | 分词等工具库（nlp / heaven） | 见上游 |
| **Kuwo 服务** | 酷我音乐分类、榜单、搜索、播放、歌单接口 | 接口方条款 |
| **Kugou 接口** | 歌词搜索与下载 `lyrics.kugou.com`、`mobileservice.kugou.com` | 接口方条款 |
| **lrclib** | 逐字歌词 `lrclib.net` | 公共领域 / CC0 |
| **ApkVmProtect** | 原版使用的加固壳（`assets/protected_by_np/`），界面逻辑被下沉至 `libgushi.so`、`libmod_gushi.so` | 闭源，商业产品 |

由于原版使用加固壳，`com.xifeng.music` 中大量界面与交互逻辑实际存在于原生库内，无法还原为 smali 或 Java。这也意味着**本仓库无法提供该应用的可编译完整源码**，只能提供改动层。

## 3. 本仓库新增的内容

| 文件 | 作者 | 许可 |
|---|---|---|
| `src/java/com/xifeng/music/xifeng/Disclaimer.java` | 二次分发者 | GPL-3.0 |
| `src/java/com/xifeng/music/xifeng/AboutActivity.java` | 二次分发者 | GPL-3.0 |
| `src/java/com/xifeng/music/xifeng/Guard.java` | 二次分发者 | GPL-3.0 |
| `patches/**/*.patch` | 二次分发者 | GPL-3.0 |
| `res/drawable/avatar_*.png` | 二次分发者 | GPL-3.0 |
| `build/build.ps1`、`build/README.md` | 二次分发者 | GPL-3.0 |
| `docs/MODIFICATIONS.md`、`README.md`、`NOTICE.md` | 二次分发者 | GPL-3.0 |

## 4. 未包含的内容

以下内容**有意不纳入**本仓库：

- 原版客户端的完整 smali 与资源解包结果；
- 原生库 `lib/arm64-v8a/libgushi.so`、`libgushi.so`、`libmod_gushi.so` 等；
- 成品 APK 文件。

如需复现修改版，请自行获取原版 APK 并按 [build/README.md](build/README.md) 构建。

## 5. 商标与名称

「冬瓜音乐」及相关名称、标识归其权利人所有。本仓库使用该名称仅为说明修改对象，不表示与原作者存在任何隶属、授权或合作关系。

## 6. 致谢

- 感谢原版客户端作者的开发与分享；
- 感谢 InnerTune 及其依赖项目的开源贡献者；
- 说明：原版使用的 ApkVmProtect 加固壳是闭源商业产品，其存在正是本仓库只能发布改动层、无法提供可编译完整源码的原因。

## 7. 联系

二次分发者主页：<https://github.com/BaiX-sudo>
问题反馈邮箱：`hjl1357275420@hotmail`