# 朝厚云服务JAVA调用示例

本份代码提供在 JAVA (JAVA 11或以上) 下的编程参考。

**生产环境上不应该使用示例代码中的轮询**判断任务完成部分，
而**应该通过回调**获取任务完成信息（即配置启动任务信息中的 `notification` 字段）

想要快速开始或者查看更多算法调用示例？建议先使用我们的Python样例了解http请求方法以及请求参数： https://gitee.com/chohotech/api_python_sample (Github: https://github.com/choho-tech/api_python_sample)


## 使用步骤

在`Seg.java`中的常量部分（16-22行）填写对应信息后在命令行中运行

```bash
javac -cp ".:json-20220320.jar" Seg.java && java -cp ".:json-20220320.jar" Seg
```

在`Seg.java`同目录下将生成分牙结果`processed_mesh.stl`和`seg_labels.txt`

## 样例

- 本样例展示了
  1. 如何新建任务JSON
  2. 如何向服务器新建任务
  3. 如何向服务器查询任务状态并等待任务完成
  4. 如何获取任务结果
  5. 如何解析任务结果
- 请注意，这里我们展示了如何进行分牙任务，但是其他任务大同小异，用户经过简单的修改即可使用
- 本样例的main函数展示的是如何将一个STL半颌文件进行切分并将结果写入磁盘

## 代码许可

本仓库基于AGPL v3.0许可开源，如果您在项目中使用本仓库的代码，则您的项目必须向用户（包括SaaS用户）提供源代码。如果您是朝厚的付费用户，此份代码将根据我们的订阅用户协议向您授权，您没有遵守AGPL v3.0开源协议的义务。