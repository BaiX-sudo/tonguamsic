# 构建说明

从原版 APK 出发，完整复现本仓库对应的修改版。

---

## 1. 工具链

| 工具 | 版本 | 用途 |
|---|---|---|
| JDK | Microsoft OpenJDK **21.0.12.1** | 运行 apktool / javac / d8 |
| apktool | **3.0.3** | 解包、回编 smali 与资源 |
| Android SDK Build-Tools | **34.0.0** | `aapt2`、`d8`、`zipalign`、`apksigner` |
| Android SDK Platform | **android-34**（`android.jar`） | `javac` 的编译期 API |
| Android SDK Platform-Tools | 37.0.1 | 仅调试时需要 |
| 调试密钥 | `debug.keystore`，别名 `androiddebugkey`，口令 `android` | 产物签名 |

> 产物用 **Android 调试证书**签名，只能自测，不能覆盖安装任何已用正式证书签名的版本。
> 正式分发请改用你自己的发布密钥，并同步更新 `build.ps1` 中的参数。

环境变量（`build.ps1` 会自动设置）：

```
JAVA_HOME      = <tools>\jdk\jdk-21.0.12.1+1
ANDROID_HOME   = <tools>\android-sdk
PATH           = %JAVA_HOME%\bin;<build-tools>;<platform-tools>;...
```

---

## 2. 解包原版 APK

```bash
java -jar apktool-3.0.3.jar d -f 冬瓜音乐_1.0.7.apk -o decoded
```

基线版本 SHA-256 应为：

```
A4C2DE503A0C98635B86E4DF1CA89BCD71F3F63BCFAA55739D12C2A8378A485E
```

不匹配说明你手上的原版不同，补丁大概率无法直接应用，请先核对
`patches/README.md` 中的校验命令。

---

## 3. 应用 smali 补丁

在 `decoded/` 的**上一级**目录执行：

```bash
patch -p1 -d decoded < patches/com/xifeng/music/App.smali.patch
patch -p1 -d decoded < patches/com/xifeng/music/ui/screens/as.smali.patch
```

详见 [../patches/README.md](../patches/README.md)。

---

## 4. 注册 AboutActivity

编辑 `decoded/AndroidManifest.xml`，在 `<application>` 内加入：

```xml
<activity
    android:name="com.xifeng.music.xifeng.AboutActivity"
    android:exported="false"
    android:label="关于我们"
    android:theme="@style/Theme.InnerTune"
    android:configChanges="orientation|screenSize|keyboardHidden|uiMode" />
```

---

## 5. 复制头像资源

三名开发者的头像已随仓库提供，直接复制到解包目录的资源目录即可：

```powershell
Copy-Item "<repo>\res\drawable\avatar_*.png" "decoded\res\drawable\" -Force
```

圆形裁切由代码侧的遮罩完成，不需要预先做成圆形图片。

---

## 6. 把三个 Java 文件编成 classes3.dex

原包已含 `classes.dex`、`classes2.dex`，新增代码作为第三个 dex 注入。

```powershell
$BT    = "<tools>\android-sdk\build-tools\34.0.0"
$JAR   = "<tools>\android-34\android.jar"
$JAVA  = "<tools>\jdk\jdk-21.0.12.1+1\bin"
$SRC   = "<repo>\src"
$WORK  = "<workdir>\inject"

New-Item -ItemType Directory -Force -Path $WORK | Out-Null

# 5.1 编译
& "$JAVA\javac.exe" -source 8 -target 8 `
    -bootclasspath $JAR -cp $JAR `
    -d $WORK `
    "$SRC\java\com\xifeng\music\xifeng\AboutActivity.java" `
    "$SRC\java\com\xifeng\music\xifeng\Disclaimer.java" `
    "$SRC\java\com\xifeng\music\xifeng\Guard.java"

# 5.2 打成 jar
& "$JAVA\jar.exe" cf "$WORK\inject.jar" -C $WORK com

# 5.3 转 dex
& java -cp "$BT\lib\d8.jar" com.android.tools.r8.D8 `
    --release --min-api 24 --lib $JAR `
    --output $WORK "$WORK\inject.jar"

# 5.4 放到 unknown/ 下，apktool 会把它作为 classes3.dex 打进包
Copy-Item "$WORK\classes.dex" "decoded\unknown\classes3.dex" -Force
```

> **必须使用具名静态嵌套类。** D8 8.2.2-dev 处理非 `static` 方法内的匿名内部类时会抛
> `NullPointerException: Cannot invoke "String.length()" because "<parameter1>" is null`。
> 本仓库的 `Guard.TouchHandler`、`Guard.CopyListener`、`Disclaimer.CenterImageSpan` 等均为具名静态类，
> 修改时请保持这一写法。

> 三个源文件之间存在相互引用（`Guard` 调用 `AboutActivity`），必须**一次性全部编译**，
> 不要分次单独编译。

---

## 7. 回编并签名

```powershell
.\build.ps1 -OutName donggua-music-gpl -DecodedDir <path>\decoded -OutDir <输出目录>
```

`build.ps1` 依次执行：

1. `apktool b` —— smali → dex，资源重编译
2. `zipalign -f -p 4`
3. `apksigner sign`，开启 **v2 + v3**（关闭 v1）
4. `apksigner verify` 校验

产物为 `冬瓜音乐_二创版_<时间戳>.apk`。

---

## 8. 实测基线

修改版 APK 在下列环境下完成回归，确认可用：

| 项 | 值 |
|---|---|
| 系统 | Android 14（SDK 34） |
| 架构 | `arm64-v8a` |
| 分辨率 / 密度 | 720 × 1612 / 272 dpi |
| 设备 | 荣耀 JDY-AN00 |

> `Guard` 的命中区域以该设备的实际布局标定。**换设备或改系统字号后必须重新标定**，
> 否则接管区域会整体错位，表现为点不中或误触发。相关常量集中在 `Guard.attach()` 顶部。