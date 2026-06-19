package com.xingheyuzhuan.shiguangschedule.data.network

import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════
// CourseItem → Course / CourseWeek 的数据转换扩展函数
//
// 承担以下职责:
// 1. 解析 jc (节次) → startSection / endSection
// 2. 解析 zcd (周次) → List<Int> 周数列表（防线 2: 鲁棒正则 + 逐段兜底）
// 3. 构造 Course Room 实体
// ═══════════════════════════════════════════════════════════════════════════

// ── 正则: 提取数字区间 + 可选单双标识 ──
// 匹配 "1-16(单)"、"16"、"3-5" 等（周字和括号已由调用方预处理）
private val WEEK_SEGMENT_REGEX = Regex(
    """(\d+)\s*(?:-\s*(\d+))?[^(（]*(?:[(（]\s*([单双])\s*[)）])?"""
)

/** 解析节次 "1-2" → Pair(1, 2)，单节 "5" → Pair(5, 5)，非法 → (null, null) */
private fun CourseItem.parseJc(): Pair<Int?, Int?> {
    if (jc.isBlank()) return null to null
    val parts = jc.split("-")
    return if (parts.size == 2) {
        (parts[0].trim().toIntOrNull()) to (parts[1].trim().toIntOrNull())
    } else {
        val single = jc.trim().toIntOrNull()
        single to single
    }
}

/**
 * 解析周次字符串，支持以下格式（含混合逗号拼接）:
 *
 * | 输入                           | 输出                   |
 * |-------------------------------|-----------------------|
 * | "1-16周"                      | [1..16]               |
 * | "1-16周(单)"                  | [1,3,5,...,15]        |
 * | "2-16周(双)"                  | [2,4,6,...,16]        |
 * | "1,3,5周"                     | [1,3,5]               |
 * | "5周"                         | [5]                   |
 * | "1-15周(单),16周"             | [1,3,5,...,15,16]     |
 * | "" / "gibberish"              | []                    |
 *
 * ## 防线 2 设计: 逐段拆分 + 逐段兜底
 *
 * 1. 整串用 ',' 拆分为段（处理混合格式）
 * 2. 每段用正则提取: 起始数字 [ - 结束数字 ] + (单)/(双) 标识
 * 3. 每段独立 try-catch 兜底 —— 单段失败仅丢弃该段，不阻断整体
 * 4. 因此即使教务系统返回 "1-15周(单), gibberish, 3-5周"，也能
 *    导出 [1,3,5,...,15, 3,4,5]
 */
fun CourseItem.parseWeeks(): List<Int> {
    if (zcd.isBlank()) return emptyList()

    val result = mutableListOf<Int>()

    // Step A: 整串用逗号/分号拆分
    val segments = zcd.split(",", ";", "，", "；")

    for (segment in segments) {
        val trimmed = segment.trim().replace("周", "")
        if (trimmed.isBlank()) continue

        try {
            val match = WEEK_SEGMENT_REGEX.find(trimmed)
            if (match == null) continue  // 该段无法识别，丢弃

            val start = match.groupValues[1].toIntOrNull() ?: continue
            val end = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: start
            val modifier = match.groupValues[3] // "单" / "双" / ""

            val range = if (start <= end) start..end else end..start
            val filtered = when (modifier) {
                "单" -> range.filter { it % 2 != 0 }
                "双" -> range.filter { it % 2 == 0 }
                else -> range.toList()
            }
            result.addAll(filtered)
        } catch (_: Exception) {
            // 防线 2: 单段失败静默丢弃，继续处理下一段
            continue
        }
    }

    return result.distinct().sorted()
}

/**
 * 将 API 返回的 [CourseItem] 转换为 Room 实体 [Course]。
 *
 * ## 防线 1: 主键类型安全
 *
 * `Course.@PrimaryKey val id: String` 接受手动 UUID 分配。
 * 此模式与 `CourseConversionRepository.importCoursesFromList()` 第 86 行一致。
 *
 * @param courseTableId 目标课表 ID
 * @param colorInt 课程颜色索引（由调用方按名称分组计算）
 */
fun CourseItem.toCourseEntity(courseTableId: String, colorInt: Int): Course {
    val (startSection, endSection) = parseJc()
    return Course(
        id = UUID.randomUUID().toString(),
        courseTableId = courseTableId,
        name = kcmc.trim(),
        teacher = jsxm.trim(),
        position = cdmc.trim(),
        day = xqj.toIntOrNull() ?: 1,
        startSection = startSection,
        endSection = endSection,
        isCustomTime = false,
        customStartTime = null,
        customEndTime = null,
        colorInt = colorInt,
        remark = null
    )
}
