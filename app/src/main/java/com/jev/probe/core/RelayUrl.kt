package com.jev.probe.core

/**
 * 第三方中转（OpenAI 兼容）的地址归一。
 *
 * 中转商给的 base 写法很不统一：带不带 `/v1`、连 `/chat/completions` 一起粘进来、
 * 有没有 `/api` 前缀，都见过。这里统一归到站点根再拼，用户填哪种写法都认——
 * 跟 PC 版 `core/relay.py` 是同一套规则，两边的自测也是同一批断言。
 *
 * 判断口各家叫法不同（OpenRouter 官方是 `/api/alpha/decisions`，PackyCode 的 typesafe
 * 通道是 `/v1/systemone`，拿前者打后者会 404），所以路径单独填，见 [judgeUrl]。
 *
 * 纯字符串处理，不碰 Android API，可以直接跑 JVM 单测（RelayUrlTest）。
 */
object RelayUrl {

    /** OpenRouter 官方的判断路径。用户没填自定义路径时，按站点根拼这一条。 */
    const val DEFAULT_JUDGE_PATH = "/api/alpha/decisions"

    /** base → 站点根：把 `/v1`、`/chat/completions` 这些尾巴去掉。 */
    fun root(base: String): String {
        var b = base.trim().trimEnd('/')
        for (suffix in listOf("/v1/chat/completions", "/chat/completions")) {
            if (b.endsWith(suffix)) {
                b = b.dropLast(suffix.length)
                break
            }
        }
        return if (b.endsWith("/v1")) b.dropLast(3) else b
    }

    /**
     * 站点根：在 [root] 基础上再去掉尾部的 `/api`。默认判断路径本身就带 `/api`，
     * base 里也有的话会叠成 `/api/api`。
     */
    fun siteRoot(base: String): String {
        val r = root(base)
        return if (r.endsWith("/api")) r.dropLast(4) else r
    }

    /** 起草 / 视觉的口：`{站点根}/v1/chat/completions`。 */
    fun chatUrl(base: String): String = root(base) + "/v1/chat/completions"

    /**
     * 判断 / 排序的口。
     *
     * @param path 留空 = OpenRouter 那个默认路径，按**站点根**拼（它自带 `/api`）；
     *        填了 = 按 **API 根**（[root] 的结果）拼，保留 base 里可能有的 `/api` 前缀——
     *        用户多半是照着报错的原文抄的（"typesafe channel only supports POST
     *        /v1/systemone"），照抄就得能用。少写开头的斜杠也认。
     */
    fun judgeUrl(base: String, path: String = ""): String {
        val p = path.trim()
        if (p.isEmpty()) return siteRoot(base) + DEFAULT_JUDGE_PATH
        return root(base) + if (p.startsWith("/")) p else "/$p"
    }
}
