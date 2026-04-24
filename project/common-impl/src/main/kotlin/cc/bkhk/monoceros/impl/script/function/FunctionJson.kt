package cc.bkhk.monoceros.impl.script.function

import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import com.google.gson.*
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 将 Gson JSON 操作封装为 Fluxon 脚本函数。
 *
 * Gson 由 Minecraft 服务端运行时提供，无需额外依赖。
 *
 * 脚本中通过 `json()` 获取工具对象，或直接使用顶层便捷函数：
 *
 * ```fluxon
 * // 顶层函数
 * obj = jsonParse('{"name":"test","value":42}')
 * name = &obj :: get("name")
 * text = jsonStringify(&obj)
 *
 * // 工具对象
 * j = json()
 * obj = &j :: parse('{"a":1}')
 * &j :: getString(&obj, "a")
 *
 * // 构建 JSON
 * obj = jsonObject()
 * &obj :: set("name", "Monoceros")
 * &obj :: set("version", 1)
 * text = jsonStringify(&obj)
 * ```
 */
@FluxonRelocate
object FunctionJson {

    private val gson = Gson()
    private val gsonPretty = GsonBuilder().setPrettyPrinting().create()

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        if (!FluxonChecker.isReady()) return
        with(FluxonRuntime.getInstance()) {
            // 注册 JsonApi 的 @Export(shared = true) 方法
            exportRegistry.registerClass(JsonApi::class.java)

            // 顶层便捷函数
            registerFunction("json", FunctionSignature.returnsObject().noParams()) { it.setReturnRef(JsonApi) }
            exportRegisteredFunction("json")

            registerFunction("jsonParse", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                ctx.setReturnRef(JsonApi.parse(ctx.getString(0)))
            }
            exportRegisteredFunction("jsonParse")

            registerFunction("jsonObject", FunctionSignature.returnsObject().noParams()) { ctx ->
                ctx.setReturnRef(JsonApi.newObject())
            }
            exportRegisteredFunction("jsonObject")

            registerFunction("jsonArray", FunctionSignature.returnsObject().noParams()) { ctx ->
                ctx.setReturnRef(JsonApi.newArray())
            }
            exportRegisteredFunction("jsonArray")

            registerFunction("jsonStringify", FunctionSignature.returns(Type.STRING).params(Type.OBJECT)) { ctx ->
                ctx.setReturnRef(JsonApi.stringify(ctx.getRef(0)))
            }
            exportRegisteredFunction("jsonStringify")

            registerFunction("jsonPretty", FunctionSignature.returns(Type.STRING).params(Type.OBJECT)) { ctx ->
                ctx.setReturnRef(JsonApi.pretty(ctx.getRef(0)))
            }
            exportRegisteredFunction("jsonPretty")

            // JsonObject 扩展函数
            registerExtension(JsonObject::class.java)
                .sharedFunction("get", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(JsonApi.get(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("getString", FunctionSignature.returns(Type.STRING).params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(JsonApi.getString(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("getInt", FunctionSignature.returns(Type.I).params(Type.STRING)) { ctx ->
                    ctx.setReturnInt(JsonApi.getInt(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("getLong", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(JsonApi.getLong(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("getDouble", FunctionSignature.returns(Type.D).params(Type.STRING)) { ctx ->
                    ctx.setReturnDouble(JsonApi.getDouble(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("getBoolean", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(JsonApi.getBoolean(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("getObject", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(JsonApi.getObject(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("getArray", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(JsonApi.getArray(ctx.target!!, ctx.getString(0)))
                }
                .sharedFunction("has", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.has(ctx.getString(0)))
                }
                .sharedFunction("set", FunctionSignature.returnsVoid().params(Type.STRING, Type.OBJECT)) { ctx ->
                    JsonApi.set(ctx.target!!, ctx.getString(0), ctx.getRef(1))
                }
                .sharedFunction("remove", FunctionSignature.returnsVoid().params(Type.STRING)) { ctx ->
                    ctx.target!!.remove(ctx.getString(0))
                }
                .sharedFunction("keys", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.keySet().toList())
                }
                .sharedFunction("size", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                    ctx.setReturnInt(ctx.target!!.size())
                }
                .sharedFunction("toMap", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(JsonApi.toMap(ctx.target!!))
                }

            // JsonArray 扩展函数
            registerExtension(JsonArray::class.java)
                .sharedFunction("get", FunctionSignature.returnsObject().params(Type.I)) { ctx ->
                    ctx.setReturnRef(JsonApi.unwrap(ctx.target!![ctx.getInt(0)]))
                }
                .sharedFunction("size", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                    ctx.setReturnInt(ctx.target!!.size())
                }
                .sharedFunction("add", FunctionSignature.returnsVoid().params(Type.OBJECT)) { ctx ->
                    ctx.target!!.add(JsonApi.wrap(ctx.getRef(0)))
                }
                .sharedFunction("toList", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(JsonApi.toList(ctx.target!!))
                }
        }
    }

    /**
     * JSON 工具 API 对象
     *
     * 所有方法标记 @Export(shared = true) 以支持跨插件共享。
     */
    object JsonApi {

        // ── 解析 ──

        /** 将 JSON 字符串解析为对象（JsonObject / JsonArray / 基本类型） */
        @Export(shared = true)
        fun parse(text: String): Any? {
            return try {
                unwrap(JsonParser.parseString(text))
            } catch (_: JsonSyntaxException) {
                null
            }
        }

        // ── 序列化 ──

        /** 将对象序列化为紧凑 JSON 字符串 */
        @Export(shared = true)
        fun stringify(obj: Any?): String {
            return when (obj) {
                is JsonElement -> gson.toJson(obj)
                else -> gson.toJson(obj)
            }
        }

        /** 将对象序列化为格式化 JSON 字符串 */
        @Export(shared = true)
        fun pretty(obj: Any?): String {
            return when (obj) {
                is JsonElement -> gsonPretty.toJson(obj)
                else -> gsonPretty.toJson(obj)
            }
        }

        // ── 构建 ──

        /** 创建空 JsonObject */
        @Export(shared = true)
        fun newObject(): JsonObject = JsonObject()

        /** 创建空 JsonArray */
        @Export(shared = true)
        fun newArray(): JsonArray = JsonArray()

        // ── JsonObject 字段访问 ──

        /** 获取字段值，自动解包为 Kotlin/Java 基本类型 */
        @Export(shared = true)
        fun get(obj: JsonObject, key: String): Any? {
            return unwrap(obj.get(key))
        }

        @Export(shared = true)
        fun getString(obj: JsonObject, key: String): String? {
            val el = obj.get(key) ?: return null
            return if (el.isJsonPrimitive) el.asString else null
        }

        @Export(shared = true)
        fun getInt(obj: JsonObject, key: String): Int {
            val el = obj.get(key) ?: return 0
            return if (el.isJsonPrimitive) el.asInt else 0
        }

        @Export(shared = true)
        fun getLong(obj: JsonObject, key: String): Long {
            val el = obj.get(key) ?: return 0L
            return if (el.isJsonPrimitive) el.asLong else 0L
        }

        @Export(shared = true)
        fun getDouble(obj: JsonObject, key: String): Double {
            val el = obj.get(key) ?: return 0.0
            return if (el.isJsonPrimitive) el.asDouble else 0.0
        }

        @Export(shared = true)
        fun getBoolean(obj: JsonObject, key: String): Boolean {
            val el = obj.get(key) ?: return false
            return if (el.isJsonPrimitive) el.asBoolean else false
        }

        @Export(shared = true)
        fun getObject(obj: JsonObject, key: String): JsonObject? {
            val el = obj.get(key) ?: return null
            return if (el.isJsonObject) el.asJsonObject else null
        }

        @Export(shared = true)
        fun getArray(obj: JsonObject, key: String): JsonArray? {
            val el = obj.get(key) ?: return null
            return if (el.isJsonArray) el.asJsonArray else null
        }

        /** 设置字段值，自动将 Kotlin/Java 基本类型包装为 JsonElement */
        @Export(shared = true)
        fun set(obj: JsonObject, key: String, value: Any?) {
            obj.add(key, wrap(value))
        }

        // ── 转换 ──

        /** JsonObject 转为 Map<String, Any?> */
        @Export(shared = true)
        fun toMap(obj: JsonObject): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>(obj.size())
            for (key in obj.keySet()) {
                map[key] = unwrap(obj.get(key))
            }
            return map
        }

        /** JsonArray 转为 List<Any?> */
        @Export(shared = true)
        fun toList(arr: JsonArray): List<Any?> {
            return arr.map { unwrap(it) }
        }

        // ── 内部工具 ──

        /** 将 JsonElement 解包为 Kotlin/Java 友好类型 */
        internal fun unwrap(element: JsonElement?): Any? {
            if (element == null || element.isJsonNull) return null
            if (element.isJsonPrimitive) {
                val p = element.asJsonPrimitive
                return when {
                    p.isBoolean -> p.asBoolean
                    p.isNumber -> {
                        val num = p.asNumber
                        // 优先保持整数类型
                        val d = num.toDouble()
                        if (d == d.toLong().toDouble() && d in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
                            val l = num.toLong()
                            if (l in Int.MIN_VALUE..Int.MAX_VALUE) l.toInt() else l
                        } else {
                            d
                        }
                    }
                    else -> p.asString
                }
            }
            // JsonObject / JsonArray 保持原样，脚本通过扩展函数操作
            return element
        }

        /** 将 Kotlin/Java 值包装为 JsonElement */
        internal fun wrap(value: Any?): JsonElement {
            return when (value) {
                null -> JsonNull.INSTANCE
                is JsonElement -> value
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is Map<*, *> -> {
                    val obj = JsonObject()
                    value.forEach { (k, v) -> obj.add(k.toString(), wrap(v)) }
                    obj
                }
                is Iterable<*> -> {
                    val arr = JsonArray()
                    value.forEach { arr.add(wrap(it)) }
                    arr
                }
                else -> JsonPrimitive(value.toString())
            }
        }
    }
}
