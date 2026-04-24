package cc.bkhk.monoceros.impl.script.function

import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture

/**
 * HTTP 客户端函数，封装 JDK HttpURLConnection 为 Fluxon 脚本函数。
 *
 * 所有请求强制异步执行（返回 CompletableFuture），脚本侧通过 `await` 消费：
 *
 * ```fluxon
 * // GET
 * resp = await httpGet("https://v1.hitokoto.cn")
 * data = jsonParse(&resp :: body())
 *
 * // POST
 * resp = await httpPost("https://example.com/api", '{"key":"value"}')
 *
 * // 通用请求
 * headers = ["Authorization": "Bearer token123", "Accept": "application/json"]
 * resp = await httpRequest("https://example.com/api", "PUT", &headers, '{"data":1}')
 * if &resp :: isOk() {
 *     print(&resp :: body())
 * }
 * ```
 */
@FluxonRelocate
object FunctionHttp {

    private const val DEFAULT_CONNECT_TIMEOUT = 5000
    private const val DEFAULT_READ_TIMEOUT = 10000

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        if (!FluxonChecker.isReady()) return
        with(FluxonRuntime.getInstance()) {
            exportRegistry.registerClass(HttpApi::class.java)

            // httpGet(url) -> CompletableFuture<HttpResponse>
            registerFunction("httpGet", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                ctx.setReturnRef(HttpApi.get(ctx.getString(0)))
            }
            exportRegisteredFunction("httpGet")

            // httpPost(url, body) -> CompletableFuture<HttpResponse>
            registerFunction("httpPost", FunctionSignature.returnsObject().params(Type.STRING, Type.STRING)) { ctx ->
                ctx.setReturnRef(HttpApi.post(ctx.getString(0), ctx.getString(1)))
            }
            exportRegisteredFunction("httpPost")

            // httpRequest(url, method, headers, body) -> CompletableFuture<HttpResponse>
            registerFunction("httpRequest", FunctionSignature.returnsObject().params(Type.STRING, Type.STRING, Type.OBJECT, Type.OBJECT)) { ctx ->
                val url = ctx.getString(0)
                val method = ctx.getString(1)
                @Suppress("UNCHECKED_CAST")
                val headers = ctx.getRef(2) as? Map<String, String> ?: emptyMap()
                val body = ctx.getRef(3)?.toString()
                ctx.setReturnRef(HttpApi.request(url, method, headers, body))
            }
            exportRegisteredFunction("httpRequest")

            // HttpResponse 扩展函数
            registerExtension(HttpResponse::class.java)
                .sharedFunction("statusCode", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                    ctx.setReturnInt(ctx.target!!.statusCode)
                }
                .sharedFunction("body", FunctionSignature.returns(Type.STRING).noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.body)
                }
                .sharedFunction("header", FunctionSignature.returns(Type.STRING).params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.headers[ctx.getString(0).lowercase()])
                }
                .sharedFunction("headers", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.headers)
                }
                .sharedFunction("isOk", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.statusCode in 200..299)
                }
        }
    }

    /** HTTP 响应数据类 */
    class HttpResponse(
        val statusCode: Int,
        val body: String,
        val headers: Map<String, String>
    )

    object HttpApi {

        /** GET 请求 */
        @Export(shared = true)
        fun get(url: String): CompletableFuture<HttpResponse> {
            return request(url, "GET", emptyMap(), null)
        }

        /** POST 请求（Content-Type: application/json） */
        @Export(shared = true)
        fun post(url: String, body: String): CompletableFuture<HttpResponse> {
            return request(url, "POST", mapOf("Content-Type" to "application/json"), body)
        }

        /** 通用请求 */
        @Export(shared = true)
        fun request(
            url: String,
            method: String,
            headers: Map<String, String>,
            body: String?
        ): CompletableFuture<HttpResponse> {
            return CompletableFuture.supplyAsync {
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = method.uppercase()
                    conn.connectTimeout = DEFAULT_CONNECT_TIMEOUT
                    conn.readTimeout = DEFAULT_READ_TIMEOUT
                    conn.setRequestProperty("User-Agent", "Monoceros/1.0")
                    headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

                    // 写入请求体
                    if (body != null && method.uppercase() in listOf("POST", "PUT", "PATCH")) {
                        conn.doOutput = true
                        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
                    }

                    val statusCode = conn.responseCode
                    val responseBody = try {
                        (if (statusCode in 200..399) conn.inputStream else conn.errorStream)
                            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    } catch (_: Throwable) {
                        ""
                    }

                    // 收集响应头（取每个 header 的第一个值）
                    val responseHeaders = LinkedHashMap<String, String>()
                    var i = 0
                    while (true) {
                        val key = conn.getHeaderFieldKey(i) ?: if (i == 0) { i++; continue } else break
                        val value = conn.getHeaderField(i) ?: break
                        responseHeaders[key.lowercase()] = value
                        i++
                    }

                    HttpResponse(statusCode, responseBody, responseHeaders)
                } finally {
                    conn.disconnect()
                }
            }
        }
    }
}
