package cc.bkhk.monoceros.dispatcher.probe

import cc.bkhk.monoceros.api.dispatcher.StateProbe
import cc.bkhk.monoceros.dispatcher.DispatcherListener
import cc.bkhk.monoceros.dispatcher.event.PlayerArmorChangeEvent
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * StateProbe 运行时服务
 */
object StateProbeService {

    private const val MODULE = "StateProbe"

    private val probes = CopyOnWriteArrayList<StateProbe>()
    private val started = AtomicBoolean(false)

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        register(PlayerArmorChangeStateProbe())
    }

    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        if (started.compareAndSet(false, true)) {
            submit(period = 5L, async = false) {
                runOnce()
            }
            DiagnosticLogger.info(MODULE, "状态探针轮询已启动")
        }
    }

    fun register(probe: StateProbe) {
        probes.removeIf { it.id == probe.id }
        probes += probe
        DiagnosticLogger.info(MODULE, "注册状态探针: ${probe.id}")
    }

    fun runOnce() {
        probes.forEach { probe ->
            probe.poll().forEach { event ->
                DispatcherListener.dispatchVirtual(event)
                if (probe is PlayerArmorChangeStateProbe && event is PlayerArmorChangeEvent) {
                    val equipment = event.player.equipment ?: return@forEach
                    if (event.isCancelled) {
                        when (event.slot) {
                            org.bukkit.inventory.EquipmentSlot.HEAD -> equipment.helmet = event.oldItem.clone()
                            org.bukkit.inventory.EquipmentSlot.CHEST -> equipment.chestplate = event.oldItem.clone()
                            org.bukkit.inventory.EquipmentSlot.LEGS -> equipment.leggings = event.oldItem.clone()
                            org.bukkit.inventory.EquipmentSlot.FEET -> equipment.boots = event.oldItem.clone()
                            else -> Unit
                        }
                        probe.rollback(event)
                    } else {
                        when (event.slot) {
                            org.bukkit.inventory.EquipmentSlot.HEAD -> equipment.helmet = event.newItem.clone()
                            org.bukkit.inventory.EquipmentSlot.CHEST -> equipment.chestplate = event.newItem.clone()
                            org.bukkit.inventory.EquipmentSlot.LEGS -> equipment.leggings = event.newItem.clone()
                            org.bukkit.inventory.EquipmentSlot.FEET -> equipment.boots = event.newItem.clone()
                            else -> Unit
                        }
                        probe.updateSnapshot(event)
                    }
                }
            }
        }
    }
}
