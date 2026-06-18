package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作考试安排 (ExamEntity) 数据表。
 * 支持全量刷新（replaceAll），适用于从教务系统完整拉取后覆盖本地缓存的场景。
 */
@Dao
interface ExamDao {

    /**
     * 获取所有考试记录，按考试时间降序排列（最新的在前）。
     */
    @Query("SELECT * FROM exams ORDER BY kssj DESC")
    fun getAll(): Flow<List<ExamEntity>>

    /**
     * 清空考试表。
     */
    @Query("DELETE FROM exams")
    suspend fun deleteAll()

    /**
     * 批量插入考试记录。冲突时替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExamEntity>)

    /**
     * 全量刷新考试数据：在事务中先删除全部旧记录，再插入新记录。
     * 确保数据一致性，避免重复或残留的旧数据。
     */
    @Transaction
    suspend fun replaceAll(items: List<ExamEntity>) {
        deleteAll()
        insertAll(items)
    }
}
