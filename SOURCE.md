# 构建与包体说明

本仓库主要提供冬瓜音乐 1.0.7 的多 CPU 架构安装包与使用说明。

## 应用信息

- 应用名称：冬瓜音乐
- 包名：`com.xifeng.music`
- 版本：1.0.7
- versionCode：26
- 最低系统：Android 7.0（API 24）
- 目标系统：Android 15（API 35）
- 原生架构：`arm64-v8a`、`armeabi-v7a`、`x86_64`

## 关于源码

应用包含第三方组件、原生音乐库与加固/保护模块，源码不在本仓库中。

本仓库是二创版本的多架构分发页：

- 原版：冬瓜音乐
- 二创作者：不许人间见白头
- 项目地址：<https://github.com/BaiX-sudo/tonguamsic>

如需源码或二次开发，请参考原项目/二创作者发布的内容，并遵守其许可证与非商业使用声明。

## 包体拆分方式

1. 从原始 APK 中读取全部条目。
2. 去除 `META-INF` 中的旧签名文件。
3. 按目标 ABI 仅保留 `lib/<abi>/` 目录，删除其他架构的 native 库。
4. 使用 `zipalign -p -f 4` 进行 4 字节/页对齐。
5. 使用 `apksigner` 统一签名。
6. 使用 `aapt2 dump badging` 校验 `native-code`、包名与版本号。

## 校验命令

```bash
# 查看包信息与支持架构
aapt2 dump badging DongguaMusic-1.0.7-arm64-v8a.apk

# 校验对齐
zipalign -c -p 4 DongguaMusic-1.0.7-arm64-v8a.apk

# 校验签名
apksigner verify --print-certs DongguaMusic-1.0.7-arm64-v8a.apk
```

## 架构选择

| 架构 | 适用场景 |
| --- | --- |
| `universal` | 不确定设备架构，或希望一个包通用 |
| `arm64-v8a` | 大多数现代 Android 手机、平板 |
| `armeabi-v7a` | 32 位 ARM 老设备 |
| `x86_64` | x86_64 模拟器、部分 x86 平板 |

> x86_64 包无法在 ARM 手机上运行，这是正常的架构差异。  
> Android 14 对 32 位应用可能显示兼容性提示，确认后可继续使用；64 位设备建议使用 `arm64-v8a`。

## 许可证

本仓库沿用原项目的 GNU General Public License v3.0，详见 [LICENSE](LICENSE)。

应用内公告同时要求：仅供学习交流，请勿用于商业用途。音乐、封面、歌词、榜单等内容的版权归各自权利人所有。
