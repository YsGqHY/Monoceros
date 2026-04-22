package cc.bkhk.monoceros.api.mechanic.visual

import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import java.util.UUID

/** BossBar 定义 */
data class ManagedBossBar(
    val id: String,
    var title: String,
    var progress: Double = 1.0,
    var color: BarColor = BarColor.WHITE,
    var style: BarStyle = BarStyle.SOLID,
)

/** 消息队列条目 */
data class QueuedMessage(
    val content: String,
    val priority: Int = 0,
    val durationTicks: Long = 40,
    val type: MessageType = MessageType.ACTION_BAR,
)

enum class MessageType { ACTION_BAR, TITLE, SUBTITLE }

/** 视觉服务 */
interface VisualService {
    fun createBossBar(id: String, title: String, color: BarColor = BarColor.WHITE, style: BarStyle = BarStyle.SOLID): ManagedBossBar
    fun getBossBar(id: String): ManagedBossBar?
    fun removeBossBar(id: String)
    fun showBossBar(id: String, playerId: UUID)
    fun hideBossBar(id: String, playerId: UUID)
    fun updateBossBar(id: String, title: String? = null, progress: Double? = null, color: BarColor? = null)
    fun sendActionBar(playerId: UUID, message: String, durationTicks: Long = 40)
    fun sendTitle(playerId: UUID, title: String, subtitle: String = "", fadeIn: Int = 10, stay: Int = 40, fadeOut: Int = 10)
    fun queueMessage(playerId: UUID, message: QueuedMessage)
}
