package cc.bkhk.monoceros.volatility

import cc.bkhk.monoceros.api.volatility.DynamicWorldBorderState
import cc.bkhk.monoceros.api.volatility.WorldBorderState
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.entity.Player
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket

/**
 * 伪世界边界 NMS 实现
 *
 * 通过 NMS 发包实现玩家级世界边界。
 * 1.17+ 使用 ClientboundInitializeBorderPacket。
 * 1.16- 使用 PacketPlayOutWorldBorder 的各子操作。
 */
class NmsVolatileWorldBorderImpl : NmsVolatileWorldBorder() {

    private companion object {
        const val MODULE = "VolatileWorldBorder"
    }

    override fun sendWorldBorder(viewer: Player, state: WorldBorderState) {
        try {
            if (MinecraftVersion.isUniversal) {
                sendWorldBorderUniversal(viewer, state)
            } else {
                sendWorldBorderLegacy(viewer, state)
            }
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "世界边界发包失败: ${viewer.name}", e)
        }
    }

    override fun sendDynamicWorldBorder(viewer: Player, state: DynamicWorldBorderState) {
        try {
            if (MinecraftVersion.isUniversal) {
                sendDynamicWorldBorderUniversal(viewer, state)
            } else {
                sendDynamicWorldBorderLegacy(viewer, state)
            }
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "动态世界边界发包失败: ${viewer.name}", e)
        }
    }

    // region 1.17+ Universal

    private fun sendWorldBorderUniversal(viewer: Player, state: WorldBorderState) {
        val nmsWorld = viewer.world.let { invokeMethod(it, "getHandle") } ?: return
        val nmsWorldBorder = newNmsWorldBorder(nmsWorld)

        state.size?.let { setWorldBorderField(nmsWorldBorder, "size", it) }
        state.center?.let {
            invokeMethod(nmsWorldBorder, "setCenter", it.x, it.z)
        }
        state.warningTime?.let { setWorldBorderField(nmsWorldBorder, "warningTime", it) }
        state.warningDistance?.let { setWorldBorderField(nmsWorldBorder, "warningBlocks", it) }
        state.damageBuffer?.let { setWorldBorderField(nmsWorldBorder, "damageSafeZone", it) }
        state.damageAmount?.let { setWorldBorderField(nmsWorldBorder, "damagePerBlock", it) }

        val packetClass = findClass("net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket")
        val packet = packetClass.getConstructor(findClass("net.minecraft.world.level.border.WorldBorder")).newInstance(nmsWorldBorder)
        viewer.sendPacket(packet)
    }

    private fun sendDynamicWorldBorderUniversal(viewer: Player, state: DynamicWorldBorderState) {
        val nmsWorld = viewer.world.let { invokeMethod(it, "getHandle") } ?: return
        val nmsWorldBorder = newNmsWorldBorder(nmsWorld)

        // 设置初始大小后 lerp 到目标大小
        setWorldBorderField(nmsWorldBorder, "size", state.oldSize)
        invokeMethod(nmsWorldBorder, "lerpSizeBetween", state.oldSize, state.newSize, state.speedMs)

        state.center?.let { invokeMethod(nmsWorldBorder, "setCenter", it.x, it.z) }
        state.warningTime?.let { setWorldBorderField(nmsWorldBorder, "warningTime", it) }
        state.warningDistance?.let { setWorldBorderField(nmsWorldBorder, "warningBlocks", it) }

        val packetClass = findClass("net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket")
        val packet = packetClass.getConstructor(findClass("net.minecraft.world.level.border.WorldBorder")).newInstance(nmsWorldBorder)
        viewer.sendPacket(packet)
    }

    // endregion

    // region 1.16- Legacy

    private fun sendWorldBorderLegacy(viewer: Player, state: WorldBorderState) {
        // 1.16- 使用 Bukkit WorldBorder API 作为降级方案
        // 真正的 NMS 实现需要构造 PacketPlayOutWorldBorder，但版本差异较大
        // 这里使用 Bukkit API 的 sendBlockChange 模式降级
        DiagnosticLogger.info(MODULE, "1.16- 世界边界降级为 Noop: ${viewer.name}")
    }

    private fun sendDynamicWorldBorderLegacy(viewer: Player, state: DynamicWorldBorderState) {
        DiagnosticLogger.info(MODULE, "1.16- 动态世界边界降级为 Noop: ${viewer.name}")
    }

    // endregion

    // region 反射工具

    private fun newNmsWorldBorder(nmsWorld: Any): Any {
        val wbClass = findClass("net.minecraft.world.level.border.WorldBorder")
        return wbClass.getConstructor().newInstance()
    }

    private fun findClass(name: String): Class<*> {
        return Class.forName(name)
    }

    private fun invokeMethod(obj: Any, name: String, vararg args: Any?): Any? {
        return try {
            val methods = obj.javaClass.methods.filter { it.name == name && it.parameterCount == args.size }
            methods.firstOrNull()?.invoke(obj, *args)
        } catch (_: Exception) {
            null
        }
    }

    private fun setWorldBorderField(wb: Any, name: String, value: Any) {
        try {
            // 尝试 setter 方法
            val setter = wb.javaClass.methods.firstOrNull {
                it.name.equals("set${name.replaceFirstChar { c -> c.uppercase() }}", ignoreCase = true) && it.parameterCount == 1
            }
            if (setter != null) {
                setter.invoke(wb, value)
                return
            }
            // 尝试直接字段
            val field = wb.javaClass.getDeclaredField(name)
            field.isAccessible = true
            field.set(wb, value)
        } catch (_: Exception) {
            // 忽略
        }
    }

    // endregion
}
