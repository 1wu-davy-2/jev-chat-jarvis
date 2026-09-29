package com.jev.probe.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 中转地址归一。断言跟 PC 版 `core/relay.py` 的自测是同一批：两边拼出来的口必须一致，
 * 不然同一个中转在手机上能通、在电脑上 404（或者反过来）。
 */
class RelayUrlTest {

    /** 中转商给的几种 base 写法都得归到同一个口。 */
    @Test fun everyBaseShapeLandsOnTheSameEndpoint() {
        val shapes = listOf(
            "https://api.x.com",
            "https://api.x.com/",
            "https://api.x.com/v1",
            "https://api.x.com/v1/chat/completions",
        )
        for (b in shapes) {
            assertEquals(b, "https://api.x.com/v1/chat/completions", RelayUrl.chatUrl(b))
            assertEquals(b, "https://api.x.com/api/alpha/decisions", RelayUrl.judgeUrl(b))
        }
    }

    /** 地址自带 /api 前缀时不能被吃掉（通义兼容那种 /compatible-mode/v1 同理）。 */
    @Test fun apiPrefixSurvivesChatButNotTheDefaultJudgePath() {
        assertEquals("https://x.com/api/v1/chat/completions", RelayUrl.chatUrl("https://x.com/api/v1"))
        // 默认路径自带 /api，按站点根拼，不能叠成 /api/api
        assertEquals("https://x.com/api/alpha/decisions", RelayUrl.judgeUrl("https://x.com/api/v1"))
        assertEquals("https://x.com/api/alpha/decisions", RelayUrl.judgeUrl("https://x.com/api"))
        assertEquals("https://x.com/api/alpha/decisions", RelayUrl.judgeUrl("https://x.com"))
    }

    /** 用户自己填的判断路径按 API 根拼，保留 base 里那个 /api。 */
    @Test fun customJudgePathKeepsTheApiRoot() {
        assertEquals("https://x.com/api/v1/systemone", RelayUrl.judgeUrl("https://x.com/api/v1", "/v1/systemone"))
        assertEquals("https://x.com/v1/systemone", RelayUrl.judgeUrl("https://x.com", "v1/systemone")) // 少个斜杠也认
        assertEquals("https://x.com/v1/systemone", RelayUrl.judgeUrl("https://x.com", "  /v1/systemone  "))
    }

    /** 内置预设的 base 原样喂进来，拼出来的口不能变（改归一不能动到既有三条官方路）。 */
    @Test fun presetBasesStillProduceTheirOriginalEndpoints() {
        assertEquals("https://openrouter.ai/api/v1/chat/completions",
            RelayUrl.chatUrl("https://openrouter.ai/api/v1"))
        assertEquals("https://openrouter.ai/api/alpha/decisions",
            RelayUrl.root("https://openrouter.ai/api") + "/alpha/decisions")
        assertEquals("https://api.deepseek.com/v1/chat/completions",
            RelayUrl.chatUrl("https://api.deepseek.com/v1"))
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            RelayUrl.chatUrl("https://dashscope.aliyuncs.com/compatible-mode/v1"))
        assertEquals("https://api.typesafe.ai/v1/systemone",
            RelayUrl.root("https://api.typesafe.ai") + "/v1/systemone")
        assertEquals("https://opencode.ai/zen/v1/systemone",
            RelayUrl.root("https://opencode.ai/zen") + "/v1/systemone")
    }

    /** 归一得能反复跑：已经拼好的完整地址再喂一遍，结果不变。 */
    @Test fun normalizingTwiceChangesNothing() {
        val once = RelayUrl.chatUrl("https://api.x.com/v1")
        assertEquals(once, RelayUrl.chatUrl(once))
    }
}
