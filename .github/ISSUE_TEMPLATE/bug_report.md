---
name: 🐛 Bug 反馈 (Bug Report)
about: 提交集成或运行原生 SDK 时遇到的崩溃或异常 (Report crashes or exceptions)
title: '[Bug] 请用一句话简述遇到的问题'
labels: bug
assignees: ''
---

## 📝 问题描述 (Describe the bug)
请简明扼要地描述你遇到了什么问题。例如：在初始化引擎时崩溃、JNI 找不到 `so` 库、或者相机预览黑屏。

## 🔄 复现步骤 (To Reproduce)
请提供能够复现该问题的具体步骤和核心代码：
1. 调用的 API：[例如：`HiFaceSDKEngine.getInstance(context).init(...)`]
2. 传入的参数：[例如：你的模型路径、特定的配置对象]
3. 发生错误的时机：[例如：App 刚启动、调用相机权限时、连续识别 10 分钟后]

## 💻 运行环境 (Environment)
**为了快速定位问题，请务必填写完整的环境信息！**

- **SDK 版本**: [例如：v1.0.5]
- **测试设备型号**: [例如：小米 14 Pro / 某品牌定制开发板]
- **Android 系统版本**: [例如：Android 14 / API 34]
- **摄像头类型**: [例如：手机自带前置 / USB 外接单目摄像头]
- **CPU 架构**: [例如：arm64-v8a / armeabi-v7a]

## 📋 Logcat 日志堆栈 (Crash Logs)
如果是崩溃（Crash/ANR）或底层 C++ 报错，**请务必提供 Logcat 中的完整堆栈信息**。

<details>
<summary>点击展开粘贴 Logcat 日志</summary>

```log
在这里粘贴你的完整崩溃日志 / Paste your Logcat output here