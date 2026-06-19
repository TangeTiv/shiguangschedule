package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 首次启动快速入门引导弹窗
 */
@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 禁止点击外部关闭，确保用户看到内容 */ },
        title = {
            Text(
                text = "🚀 快速入门",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                StepText("1", "点击底部【我的】→ 设置开学日期")
                StepText("2", "点击底部【校园】→ 教务同步")
                StepText("3", "输入教务系统账号密码 → 全选 → 安全同步")

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "⚠️ 注意",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )

                NoteItem("现阶段导入的是下学期课表；如需本学期，请到【我的】→ 课表导入/导出 → 教务导入 中抓取。")
                NoteItem("【校园】页面当前提供：教务同步、考试安排、成绩查询，其他功能后续开发。")
                NoteItem("有问题请到【我的】→ 更多 → 反馈与建议 中告诉我们。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
}

@Composable
private fun StepText(step: String, text: String) {
    Text(
        text = "• 步骤 $step：$text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun NoteItem(text: String) {
    Text(
        text = "  · $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}
