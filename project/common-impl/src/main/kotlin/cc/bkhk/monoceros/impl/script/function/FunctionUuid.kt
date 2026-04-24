package cc.bkhk.monoceros.impl.script.function

import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.util.UUID

/**
 * UUID 工具函数，封装 java.util.UUID 为 Fluxon 脚本函数。
 *
 * ```fluxon
 * // 随机生成
 * id = uuid()
 * print(&id)  // "550e8400-e29b-41d4-a716-446655440000"
 *
 * // 从字符串解析
 * id = uuidFromString("550e8400-e29b-41d4-a716-446655440000")
 *
 * // 基于名称生成（v3）
 * id = uuidFromName("Steve")
 *
 * // 扩展函数
 * &id :: version()
 * &id :: mostBits()
 * &id :: leastBits()
 * ```
 */
@FluxonRelocate
object FunctionUuid {

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        if (!FluxonChecker.isReady()) return
        with(FluxonRuntime.getInstance()) {
            exportRegistry.registerClass(UuidApi::class.java)

            // uuid() -> UUID
            registerFunction("uuid", FunctionSignature.returnsObject().noParams()) { ctx ->
                ctx.setReturnRef(UuidApi.random())
            }
            exportRegisteredFunction("uuid")

            // uuidFromString(str) -> UUID?
            registerFunction("uuidFromString", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                ctx.setReturnRef(UuidApi.fromString(ctx.getString(0)))
            }
            exportRegisteredFunction("uuidFromString")

            // uuidFromName(name) -> UUID
            registerFunction("uuidFromName", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                ctx.setReturnRef(UuidApi.fromName(ctx.getString(0)))
            }
            exportRegisteredFunction("uuidFromName")

            // UUID 扩展函数
            registerExtension(UUID::class.java)
                .sharedFunction("version", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                    ctx.setReturnInt(ctx.target!!.version())
                }
                .sharedFunction("mostBits", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.mostSignificantBits)
                }
                .sharedFunction("leastBits", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.leastSignificantBits)
                }
        }
    }

    object UuidApi {

        /** 随机生成 UUID（v4） */
        @Export(shared = true)
        fun random(): UUID = UUID.randomUUID()

        /** 从标准格式字符串解析 UUID，格式错误返回 null */
        @Export(shared = true)
        fun fromString(value: String): UUID? {
            return try {
                UUID.fromString(value)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        /** 基于名称生成 UUID（v3，使用 MD5） */
        @Export(shared = true)
        fun fromName(name: String): UUID {
            return UUID.nameUUIDFromBytes(name.toByteArray(Charsets.UTF_8))
        }
    }
}
