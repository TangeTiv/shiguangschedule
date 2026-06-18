package com.xingheyuzhuan.shiguangschedule.data.network

import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class ScnuScraperTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ═══════════════════════════════════════════════════════════════
    // 数据模型反序列化测试
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `grade item deserialization from JSON`() {
        val jsonStr = """
            {
                "xnmmc": "2024-2025",
                "xqmmc": "第1学期",
                "kcmc": "高等数学（上）",
                "kch": "MATH1001",
                "kcxzmc": "必修",
                "xf": "4.0",
                "cj": "85",
                "jd": "3.5",
                "khfsmc": "考试",
                "jsxm": "张三",
                "kkbmmc": "数学科学学院"
            }
        """.trimIndent()

        val item = json.decodeFromString<GradeItem>(jsonStr)
        assertEquals("2024-2025", item.xnmmc)
        assertEquals("第1学期", item.xqmmc)
        assertEquals("高等数学（上）", item.kcmc)
        assertEquals("MATH1001", item.kch)
        assertEquals("必修", item.kcxzmc)
        assertEquals("4.0", item.xf)
        assertEquals("85", item.cj)
        assertEquals("3.5", item.jd)
        assertEquals("考试", item.khfsmc)
        assertEquals("张三", item.jsxm)
        assertEquals("数学科学学院", item.kkbmmc)
    }

    @Test
    fun `grade item handles missing fields with defaults`() {
        val jsonStr = """{ "kcmc": "仅有课程名" }"""
        val item = json.decodeFromString<GradeItem>(jsonStr)
        assertEquals("仅有课程名", item.kcmc)
        assertEquals("", item.xnmmc)
        assertEquals("", item.cj)
        assertEquals("", item.jd)
    }

    @Test
    fun `exam item deserialization from JSON`() {
        val jsonStr = """
            {
                "xqm": "2024-2025-1",
                "ksmc": "期末考试",
                "kcmc": "线性代数",
                "kssj": "2025-01-15 09:00-11:00",
                "cdmc": "第一教学楼",
                "cdbh": "301",
                "cdxqmc": "石牌校区",
                "ksfs": "闭卷",
                "khfs": "考试",
                "kkxy": "数学科学学院"
            }
        """.trimIndent()

        val item = json.decodeFromString<ExamItem>(jsonStr)
        assertEquals("2024-2025-1", item.xqm)
        assertEquals("期末考试", item.ksmc)
        assertEquals("线性代数", item.kcmc)
        assertEquals("2025-01-15 09:00-11:00", item.kssj)
        assertEquals("第一教学楼", item.cdmc)
        assertEquals("301", item.cdbh)
        assertEquals("石牌校区", item.cdxqmc)
        assertEquals("闭卷", item.ksfs)
        assertEquals("考试", item.khfs)
        assertEquals("数学科学学院", item.kkxy)
    }

    @Test
    fun `grade response parses items array`() {
        val jsonStr = """
            {
                "items": [
                    { "kcmc": "课程A", "cj": "90" },
                    { "kcmc": "课程B", "cj": "85" }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<GradeResponse>(jsonStr)
        assertEquals(2, response.items.size)
        assertEquals("课程A", response.items[0].kcmc)
        assertEquals("课程B", response.items[1].kcmc)
    }

    @Test
    fun `exam response handles empty items`() {
        val jsonStr = """{ "items": [] }"""
        val response = json.decodeFromString<ExamResponse>(jsonStr)
        assertTrue(response.items.isEmpty())
    }

    @Test
    fun `grade item handles text-based scores`() {
        // 教务系统可能返回 "优秀"、"良好" 等文字评分
        val jsonStr = """{ "kcmc": "体育", "cj": "优秀", "jd": "4.0" }"""
        val item = json.decodeFromString<GradeItem>(jsonStr)
        assertEquals("优秀", item.cj)
        assertEquals("4.0", item.jd)
    }

    // ═══════════════════════════════════════════════════════════════
    // 登录表单 action 正则测试
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `form action regex extracts absolute URL`() {
        val html = """<form id="loginForm" action="https://sso.scnu.edu.cn/AccountService/user/login.html" method="post">"""
        val re = Regex(
            """<form[^>]*action\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        val match = re.find(html)
        assertNotNull(match)
        assertEquals(
            "https://sso.scnu.edu.cn/AccountService/user/login.html",
            match!!.groupValues[1]
        )
    }

    @Test
    fun `form action regex extracts relative URL`() {
        val html = """<form action="/AccountService/user/login.html" method="post">"""
        val re = Regex(
            """<form[^>]*action\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        val match = re.find(html)
        assertNotNull(match)
        assertEquals("/AccountService/user/login.html", match!!.groupValues[1])
    }

    @Test
    fun `form action regex returns null on no match`() {
        val html = """<p>No form here</p>"""
        val re = Regex(
            """<form[^>]*action\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        val match = re.find(html)
        assertNull(match)
    }

    @Test
    fun `error message regex extracts error from login page`() {
        val html = """<div class="alert alert-danger">账号或密码错误</div>"""
        val re = Regex(
            """(?:alert|error|msgtext)[^>]*>\s*([^<]+?)\s*<""",
            RegexOption.IGNORE_CASE
        )
        val match = re.find(html)
        assertNotNull(match)
        assertEquals("账号或密码错误", match!!.groupValues[1].trim())
    }

    // ═══════════════════════════════════════════════════════════════
    // CookieJar 基本功能测试
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `cookie jar saves and loads cookies by host`() {
        val jar = ScnuCookieJar()
        val url = "https://sso.scnu.edu.cn".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("JSESSIONID")
            .value("abc123")
            .domain("sso.scnu.edu.cn")
            .path("/")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(cookie))
        val loaded = jar.loadForRequest(url)

        assertEquals(1, loaded.size)
        assertEquals("JSESSIONID", loaded[0].name)
        assertEquals("abc123", loaded[0].value)
    }

    @Test
    fun `cookie jar returns empty for unknown host`() {
        val jar = ScnuCookieJar()
        val url = "https://unknown.example.com".toHttpUrl()
        assertTrue(jar.loadForRequest(url).isEmpty())
    }

    @Test
    fun `cookie jar filters expired cookies`() {
        val jar = ScnuCookieJar()
        val url = "https://sso.scnu.edu.cn".toHttpUrl()
        val expiredCookie = Cookie.Builder()
            .name("OLD")
            .value("expired")
            .domain("sso.scnu.edu.cn")
            .path("/")
            .expiresAt(System.currentTimeMillis() - 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(expiredCookie))
        assertTrue(jar.loadForRequest(url).isEmpty())
    }

    @Test
    fun `cookie jar clear removes all cookies`() {
        val jar = ScnuCookieJar()
        val url = "https://sso.scnu.edu.cn".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("TEST")
            .value("val")
            .domain("sso.scnu.edu.cn")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(cookie))
        assertEquals(1, jar.loadForRequest(url).size)

        jar.clear()
        assertTrue(jar.loadForRequest(url).isEmpty())
    }
}
