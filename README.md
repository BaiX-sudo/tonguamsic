# 冬瓜音乐 · 二创开源版（Donggua Music GPL）

本仓库收录**「冬瓜音乐」Android 客户端二次分发版**中由二次分发者新增与修改的全部代码，以 [GNU GPL v3.0](LICENSE) 开源。

- 应用名称：冬瓜音乐（Donggua Music）
- 应用版本：`1.0.7`（versionCode `26`）
- 包名：`com.xifeng.music`
- 最低系统：Android 7.0（minSdk 24）／目标：Android 15（targetSdk 35）
- 二次分发者主页：<https://github.com/BaiX-sudo>

> **本仓库不是官方仓库。** 它只包含在原版 APK 之上新增/修改的代码层。原版客户端由其原作者独立开发，版权归原作者所有，详见 [NOTICE.md](NOTICE.md)。

---

## 这个仓库里有什么

原版 APK 使用 **ApkVmProtect** 加固，界面与大部分交互逻辑被下沉到 `libgushi.so` / `libmod_gushi.so` 这个原生壳里，无法还原为可读的 Java/Kotlin 源码。因此**不存在一份「原始完整源码」可以开源**。

本仓库采取的方案是：**公开改动层**——即本次二次分发新增的 3 个 Java 源文件、2 处 smali 补丁，以及完整可复现的构建脚本。任何人拿到原版 APK 后，都可以依据本仓库的文件重新产出与本仓库作者完全一致的修改版 APK。

```
donggua-music-gpl/
├── LICENSE                    GPL-3.0 全文
├── NOTICE                     来源、致谢与许可边界说明
├── README.md                  本文件
├── .gitignore
├── src/
│   └── java/com/xifeng/music/xifeng/
│       ├── AboutActivity.java   关于我们页面（重写）
│       ├── Disclaimer.java      首启免责声明弹窗（新增）
│       └── Guard.java           原生页面的触摸/状态接管层（新增）
├── patches/
│   ├── README.md               补丁说明与使用方法
│   └── com/xifeng/music/
│       ├── App.smali.patch             注入两个新类的安装入口
│       └── ui/screens/as.smali.patch   短路公告/强制更新请求
├── res/
│   └── drawable/               关于我们页面用的三张开发者头像
├── build/
│   ├── README.md               构建步骤
│   └── build.ps1               一键构建脚本
└── docs/
    └── MODIFICATIONS.md        逐项改动清单
```

---

## 改动清单

| # | 改动 | 文件 |
|---|------|------|
| 1 | 移除远程公告与强制更新弹窗（直接短路网络请求，不再发起调用及其副作用） | `patches/.../as.smali.patch` |
| 2 | 新增首启免责声明弹窗：10 秒阅读倒计时、「不再显示 / 关闭」双按钮、二次分发声明与开发者头像 | `src/.../Disclaimer.java` |
| 3 | 重写「关于我们」页面：真实应用 Logo、三名开发者头像与署名；移除原生联系方式、小程序入口、打赏与侵权说明 | `src/.../AboutActivity.java` |
| 4 | 设置页「反馈BUG」改为唤起系统邮箱，收件人 `hjl1357275420@hotmail`，自动带上版本号与设备信息 | `src/.../Guard.java` |
| 5 | 首页折叠态顶部按钮改为唤起系统浏览器打开 <https://github.com/BaiX-sudo> | `src/.../Guard.java` |
| 6 | 修正被接管控件的手势判定：滑动与长按不再误触发跳转，恢复正常的按压高亮与列表滚动 | `src/.../Guard.java` |
| 7 | 注入入口：在 `App.onCreate` 末尾安装上述两个新类 | `patches/.../App.smali.patch` |
| 8 | 注册新 Activity：`com.xifeng.music.xifeng.AboutActivity` | `docs/MODIFICATIONS.md` |
| 9 | 提供三名开发者的头像资源 | `res/drawable/avatar_*.png` |

每一处的具体位置、实现原理和注意事项见 [docs/MODIFICATIONS.md](docs/MODIFICATIONS.md)。

## 实现思路

原版的设置页与首页均运行在加固后的原生壳中，无法通过 smali 直接修改交互逻辑。`Guard` 的做法是：

1. 在 `MainActivity` 的 `DecorView` 上挂一层全屏透明 `View`；
2. 每 350ms 用 `PixelCopy` 抓取一帧，识别当前处于「设置页」还是「首页折叠态」，据此决定当前触摸是否需要接管；
3. 接管时在 `ACTION_DOWN` 消费事件并绘制自定义按压高亮，移动超过系统 `touchSlop` 时合成 `DOWN/MOVE/UP` 事件序列转交底层列表以恢复滚动，未滑动且抬手在 600ms 内才执行跳转；
4. 需要执行的动作（跳转 Activity、唤起邮箱、打开浏览器）全部由 Java 层直接发起，不依赖原生分发链路。

各控件的命中区域以设备实际布局标定，**不同分辨率或系统字号下需要重新标定**，相关常量集中在 `Guard.attach()` 顶部。

## 复现构建

完整步骤见 [build/README.md](build/README.md)。简要版：

```powershell
# 1. 用 apktool 解包原版 APK
java -jar apktool-3.0.3.jar d -f 冬瓜音乐_1.0.7.apk -o decoded

# 2. 按 patches/ 下的补丁修改 smali，并把 src/ 下的 3 个 Java 文件编成 classes3.dex
#    放入 decoded/unknown/classes3.dex，并在 AndroidManifest.xml 注册 AboutActivity

# 3. 构建并签名
.\build.ps1
```

## 实测环境

修改版 APK 在以下环境完成回归验证：

| 项 | 值 |
|---|---|
| 系统 | Android 14（SDK 34） |
| 架构 | `arm64-v8a` |
| 分辨率 / 密度 | 720 × 1612 / 272 dpi |
| 设备 | 荣耀 JDY-AN00 |

验证项包括：两个接管控件的点击、滑动、长按三种手势；反馈邮箱唤起；关于我们页面跳转；折叠态按钮唤起浏览器并正确加载 GitHub 页面。

## 已知限制

- 原版界面逻辑位于 ApkVmProtect 加固的原生库内，**无法编译为本仓库的源码**，本仓库也未包含这些 `.so` 文件。
- 命中区域为固定 dp 坐标，仅在上述实测设备上验证过；换设备或改系统字号可能需要调整 `Guard.java` 中的区域常量。
- 注入代码在 d8 8.2.2 上存在已知缺陷：非 `static` 方法内的匿名内部类会触发 `NullPointerException`，因此本仓库的注入代码一律使用**具名静态嵌套类**。若要升级构建工具链，请保留这一写法。
- 成品 APK 使用 Android 调试证书签名，仅供测试分发，**不具备正式发布所需的身份签名**。

## 免责声明

- 本仓库及其构建产物仅供学习、研究与技术交流使用。
- 音乐内容来自各第三方开放接口，版权归属原权利人，请勿用于商业用途或传播受版权保护的音频。
- 使用者需自行遵守所在地法律法规及第三方服务的使用条款。

## 许可证

本仓库发布的**二次分发者新增与修改的代码**以 [GNU General Public License v3.0](LICENSE) 授权。
原版客户端及其内置第三方组件不因本仓库的发布而改变其原有权利归属，详见 [NOTICE.md](NOTICE.md)。