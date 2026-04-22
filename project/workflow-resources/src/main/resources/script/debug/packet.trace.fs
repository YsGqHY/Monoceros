// 示例：数据包追踪脚本
// 脚本 ID: debug.packet.trace
// 被 wireshark/example.yml 引用

name = &?packetName ?? "unknown"
cls = &?packetClass ?? ""
print("[Wireshark] 追踪: ${&name} (${&cls})")
