// 示例：战斗命中反馈脚本
// 文件路径: script/action/combat.hit-feedback.fs
// 脚本 ID: action.combat.hit-feedback
// 被 workflow/action/combat-hit.yml 引用

val dmg = damage ?: 0
println("[战斗] 命中反馈: 伤害=" + dmg)
