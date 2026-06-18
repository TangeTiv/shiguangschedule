package com.xingheyuzhuan.shiguangschedule.ui.campus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.ExamDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.ExamEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 考试安排 ViewModel。
 *
 * 纯响应式设计——所有状态均由 Room Flow 驱动派生，无 init {} 副作用。
 *
 * 核心推导链：
 * 1. examDao.getAll() 是唯一的 Room 订阅（热流）
 * 2. availableTerms 由 allExams 派生（从 kssj 反推学年学期，去重 + 降序）
 * 3. _selectedTerm 初始为 null，通过 combine 优雅回退到第一个可用学期
 * 4. displayedExams 通过 combine(allExams, availableTerms, _selectedTerm) 派生，
 *    并按 kssj 正序排列，空时间沉底
 */
@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examDao: ExamDao
) : ViewModel() {

    // --- 唯一 Room 订阅（共享热流） ---

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _allExams: StateFlow<List<ExamEntity>> = examDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExams: StateFlow<List<ExamEntity>> = _allExams

    // --- 学期列表（从 kssj 反推学年学期，去重 + 降序，无副作用） ---

    val availableTerms: StateFlow<List<String>> = _allExams.map { exams ->
        exams.map { resolveTerm(it.kssj) }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 用户选中学期（null = 尚未选择，自动回退到首个可用学期） ---

    private val _selectedTerm = MutableStateFlow<String?>(null)

    /**
     * 当前生效的学期。
     * 若用户尚未手动选择（null），则自动取 availableTerms 的首项；
     * 若 availableTerms 也为空，则返回空字符串。
     */
    val selectedTerm: StateFlow<String> = combine(availableTerms, _selectedTerm) { terms, selected ->
        selected ?: terms.firstOrNull() ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // --- 当前学期考试列表（过滤 + 按时间正序，空时间沉底） ---

    @OptIn(ExperimentalCoroutinesApi::class)
    val displayedExams: StateFlow<List<ExamEntity>> = combine(
        _allExams, availableTerms, _selectedTerm
    ) { exams, terms, selected ->
        val effectiveTerm = selected ?: terms.firstOrNull() ?: ""
        val filtered = if (effectiveTerm.isBlank()) exams
        else exams.filter { resolveTerm(it.kssj) == effectiveTerm }

        // 按考试时间正序排列，空时间（待定）沉底
        filtered.sortedWith(
            compareBy<ExamEntity> { it.kssj.isBlank() }
                .thenBy { it.kssj }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 用户操作 ---

    /**
     * 用户手动切换学期过滤器。
     * 传入 null 即恢复默认（首个可用学期）。
     */
    fun selectTerm(term: String?) {
        _selectedTerm.value = term
    }

    // --- 纯函数：从考试时间反推学年学期 ---

    /**
     * 从 kssj（格式 "YYYY-MM-DD HH:MM"）反推可读的学年学期。
     *
     * 规则（中国高校学年）：
     * - 9月～次年2月 → 第一学期，学年 = Y-1-Y（如 2024-2025）
     * - 3月～7月       → 第二学期，学年 = Y-1-Y（如 2024-2025）
     * - 其他月份或解析失败 → 返回 ""
     */
    private fun resolveTerm(kssj: String): String {
        val parts = kssj.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return ""
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return ""
        val semester = when (month) {
            in 9..12, in 1..2 -> "第一学期"
            in 3..7 -> "第二学期"
            else -> return ""
        }
        val academicYear = if (month in 1..7) "${year - 1}-$year" else "$year-${year + 1}"
        return "$academicYear $semester"
    }
}
