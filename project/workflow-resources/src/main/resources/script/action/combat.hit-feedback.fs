// 示例：战斗命中反馈脚本
// 脚本 ID: action.combat.hit-feedback
// 被 workflow/action/combat-hit.yml 引用

dmg = &?damage ?? 0
print("[战斗] 命中反馈: 伤害=${&dmg}")
