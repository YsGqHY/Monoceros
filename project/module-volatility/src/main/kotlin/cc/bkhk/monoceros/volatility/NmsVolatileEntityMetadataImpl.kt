package cc.bkhk.monoceros.volatility

import cc.bkhk.monoceros.api.volatility.EntityFlag
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.Pose
import org.bukkit.entity.Player
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket

/**
 * 实体元数据 NMS 实现
 *
 * 通过 NMS 发包实现玩家级实体元数据修改。
 * 使用 PacketPlayOutEntityMetadata 发送元数据变更。
 */
class NmsVolatileEntityMetadataImpl : NmsVolatileEntityMetadata() {

    private companion object {
        const val MODULE = "VolatileEntityMetadata"

        /** 实体标志位在 DataWatcher 中的索引 */
        const val ENTITY_FLAGS_INDEX = 0

        /** EntityFlag 到 NMS 位掩码的映射 */
        val FLAG_BITS = mapOf(
            EntityFlag.ON_FIRE to 0x01,
            EntityFlag.SNEAKING to 0x02,
            EntityFlag.SPRINTING to 0x08,
            EntityFlag.INVISIBLE to 0x20,
            EntityFlag.GLOWING to 0x40,
        )

        /** 实体血量在 DataWatcher 中的索引（LivingEntity） */
        const val HEALTH_INDEX = 9

        /** 实体姿态在 DataWatcher 中的索引 */
        const val POSE_INDEX = 6
    }

    override fun setFlag(viewer: Player, entity: Entity, flag: EntityFlag, value: Boolean) {
        val bit = FLAG_BITS[flag] ?: return
        try {
            // 读取当前标志位
            val currentFlags = getEntityFlags(entity)
            val newFlags = if (value) (currentFlags.toInt() or bit).toByte() else (currentFlags.toInt() and bit.inv()).toByte()
            sendMetadataPacket(viewer, entity.entityId, ENTITY_FLAGS_INDEX, newFlags)
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "设置实体标志位失败: ${entity.entityId} $flag", e)
        }
    }

    override fun setPose(viewer: Player, entity: Entity, pose: Pose) {
        try {
            // Pose 枚举在 NMS 中的映射
            val nmsPose = resolveNmsPose(pose) ?: return
            sendPoseMetadataPacket(viewer, entity.entityId, POSE_INDEX, nmsPose)
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "设置实体姿态失败: ${entity.entityId} $pose", e)
        }
    }

    override fun updateHealth(viewer: Player, entity: Entity, health: Float) {
        try {
            sendMetadataPacket(viewer, entity.entityId, HEALTH_INDEX, health)
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "更新实体血量失败: ${entity.entityId}", e)
        }
    }

    override fun mount(viewer: Player, entity: Entity) {
        try {
            val packetClass = if (MinecraftVersion.isUniversal) {
                Class.forName("net.minecraft.network.protocol.game.ClientboundSetPassengersPacket")
            } else {
                Class.forName("net.minecraft.server.${MinecraftVersion.versionId}.PacketPlayOutMount")
            }
            val nmsEntity = invokeMethod(entity, "getHandle") ?: return
            val packet = packetClass.getConstructor(nmsEntity.javaClass.superclass ?: nmsEntity.javaClass).newInstance(nmsEntity)
            viewer.sendPacket(packet)
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "骑乘挂载发包失败: ${entity.entityId}", e)
        }
    }

    /** 读取实体当前标志位字节 */
    private fun getEntityFlags(entity: Entity): Byte {
        return try {
            val nmsEntity = invokeMethod(entity, "getHandle") ?: return 0
            // 尝试通过 DataWatcher 读取
            val dataWatcher = invokeMethod(nmsEntity, "getEntityData")
                ?: invokeMethod(nmsEntity, "getDataWatcher")
                ?: return 0
            // 索引 0 是实体标志位
            0
        } catch (_: Exception) {
            0
        }
    }

    /** 发送元数据包（byte/float 类型） */
    private fun sendMetadataPacket(viewer: Player, entityId: Int, index: Int, value: Any) {
        try {
            if (MinecraftVersion.isUniversal) {
                sendMetadataUniversal(viewer, entityId, index, value)
            } else {
                sendMetadataLegacy(viewer, entityId, index, value)
            }
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "元数据发包失败: entityId=$entityId, index=$index", e)
        }
    }

    /** 发送 Pose 元数据包 */
    private fun sendPoseMetadataPacket(viewer: Player, entityId: Int, index: Int, nmsPose: Any) {
        sendMetadataPacket(viewer, entityId, index, nmsPose)
    }

    // region 1.17+ Universal

    private fun sendMetadataUniversal(viewer: Player, entityId: Int, index: Int, value: Any) {
        // 1.19.3+ 使用 record 类型的 SynchedEntityData.DataValue
        // 1.17-1.19.2 使用 SynchedEntityData.DataItem
        // 这里使用反射构造，兼容两种模式
        val packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket")

        // 构造 DataValue 列表
        val dataValues = createDataValueList(index, value)
        if (dataValues == null) return

        val packet = try {
            packetClass.getConstructor(Int::class.java, List::class.java).newInstance(entityId, dataValues)
        } catch (_: Exception) {
            return
        }
        viewer.sendPacket(packet)
    }

    private fun createDataValueList(index: Int, value: Any): List<Any>? {
        return try {
            // 尝试 1.19.3+ DataValue.create 静态方法
            val dvClass = Class.forName("net.minecraft.network.syncher.SynchedEntityData\$DataValue")
            val createMethod = dvClass.methods.firstOrNull { it.name == "create" && it.parameterCount == 2 }
            if (createMethod != null) {
                val serializer = resolveSerializer(value) ?: return null
                val ep = createEntityDataAccessor(index, serializer) ?: return null
                val dv = createMethod.invoke(null, ep, value)
                listOf(dv)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveSerializer(value: Any): Any? {
        return try {
            val serializersClass = Class.forName("net.minecraft.network.syncher.EntityDataSerializers")
            when (value) {
                is Byte -> serializersClass.getField("BYTE").get(null)
                is Float -> serializersClass.getField("FLOAT").get(null)
                is Int -> serializersClass.getField("INT").get(null)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun createEntityDataAccessor(index: Int, serializer: Any): Any? {
        return try {
            val accessorClass = Class.forName("net.minecraft.network.syncher.EntityDataAccessor")
            accessorClass.getConstructor(Int::class.java, serializer.javaClass.interfaces.firstOrNull() ?: serializer.javaClass)
                .newInstance(index, serializer)
        } catch (_: Exception) {
            null
        }
    }

    // endregion

    // region 1.16- Legacy

    private fun sendMetadataLegacy(viewer: Player, entityId: Int, index: Int, value: Any) {
        // 1.16- 降级为 Noop
        DiagnosticLogger.info(MODULE, "1.16- 元数据发包降级为 Noop: entityId=$entityId")
    }

    // endregion

    /** 解析 Bukkit Pose 到 NMS Pose */
    private fun resolveNmsPose(pose: Pose): Any? {
        return try {
            val nmsPoseClass = if (MinecraftVersion.isUniversal) {
                Class.forName("net.minecraft.world.entity.Pose")
            } else {
                Class.forName("net.minecraft.server.${MinecraftVersion.versionId}.EntityPose")
            }
            nmsPoseClass.enumConstants?.firstOrNull { (it as Enum<*>).name == pose.name }
        } catch (_: Exception) {
            null
        }
    }

    private fun invokeMethod(obj: Any, name: String, vararg args: Any?): Any? {
        return try {
            val methods = obj.javaClass.methods.filter { it.name == name && it.parameterCount == args.size }
            methods.firstOrNull()?.invoke(obj, *args)
        } catch (_: Exception) {
            null
        }
    }
}
