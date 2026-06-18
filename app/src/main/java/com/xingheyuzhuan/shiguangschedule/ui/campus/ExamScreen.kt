package com.xingheyuzhuan.shiguangschedule.ui.campus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.db.main.ExamEntity

/**
 * 考试安排主页面。
 *
 * 布局结构（LazyColumn）：
 * 1. 学期过滤器（ScrollableTabRow）
 * 2. 考试列表（每场考试一张卡片，含时间/地点图标行）
 * 3. 空数据时展示友好空状态提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    onBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val allExams by viewModel.allExams.collectAsState()
    val availableTerms by viewModel.availableTerms.collectAsState()
    val selectedTerm by viewModel.selectedTerm.collectAsState()
    val displayedExams by viewModel.displayedExams.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.exam_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFCF9F8)
                )
            )
        },
        containerColor = Color(0xFFFCF9F8)
    ) { innerPadding ->
        if (allExams.isEmpty()) {
            EmptyExamState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 学期过滤器
                item {
                    ExamTermFilterRow(
                        terms = availableTerms,
                        selectedTerm = selectedTerm,
                        onTermSelected = { viewModel.selectTerm(it) }
                    )
                }

                // 2. 考试列表
                items(
                    items = displayedExams,
                    key = { it.id }
                ) { exam ->
                    ExamCard(exam = exam)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 空状态
// ──────────────────────────────────────────────────────────────────────────────

/**
 * 数据库中无考试数据时展示的友好空状态。
 */
@Composable
private fun EmptyExamState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color(0xFFBDBDBD)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.exam_no_data),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF757575)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.exam_no_data_hint),
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 学期过滤器
// ──────────────────────────────────────────────────────────────────────────────

/**
 * 横向可滚动的学期 Tab 行。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamTermFilterRow(
    terms: List<String>,
    selectedTerm: String,
    onTermSelected: (String) -> Unit
) {
    if (terms.isEmpty()) return

    val selectedIndex = terms.indexOf(selectedTerm).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {}
    ) {
        terms.forEachIndexed { index, term ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTermSelected(term) },
                text = {
                    Text(
                        text = term,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 单场考试卡片
// ──────────────────────────────────────────────────────────────────────────────

/**
 * 单场考试的日程卡片。
 *
 * 布局：
 * - 头部行：课程名称
 * - 时间行：Schedule 图标 + 考试时间
 * - 地点行：LocationOn 图标 + 校区 + 教学楼
 */
@Composable
private fun ExamCard(exam: ExamEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // ── 头部行：课程名称 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exam.kcmc.ifBlank { "—" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 时间行 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF757575)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = exam.kssj.ifBlank { stringResource(R.string.exam_time_pending) },
                    fontSize = 14.sp,
                    color = if (exam.kssj.isBlank()) Color(0xFFF97316) else Color(0xFF555555)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── 地点行 ──
            val venue = listOf(exam.cdxqmc, exam.cdmc)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { stringResource(R.string.exam_venue_pending) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF757575)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = venue,
                    fontSize = 14.sp,
                    color = if (venue == stringResource(R.string.exam_venue_pending))
                        Color(0xFFF97316) else Color(0xFF555555),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
