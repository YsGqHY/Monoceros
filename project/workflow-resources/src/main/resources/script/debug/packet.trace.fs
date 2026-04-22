// 示例：数据包追踪脚本
// 文件路径: script/debug/packet.trace.fs
// 脚本 ID: debug.packet.trace
// 被 wireshark/example.yml 引用

val name = packetName ?: "unknown"
val cls = packetClass ?: ""
println("[Wireshark] 追踪: " + name + " (" + cls + ")")
