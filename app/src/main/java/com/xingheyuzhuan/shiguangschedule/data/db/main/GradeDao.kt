package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作成绩 (GradeEntity) 数据表。
 * 支持全量刷新（replaceAll），适用于从教务系统完整拉取后覆盖本地缓存的场景。
 */
@Dao
interface GradeDao {

    /**
     * 获取所有成绩记录，按学年、学期降序排列（最新的在前）。
     */
    @Query("SELECT * FROM grades ORDER BY xnmmc DESC, xqmmc DESC")
    fun getAll(): Flow<List<GradeEntity>>

    /**
     * 清空成绩表。
     */
    @Query("DELETE FROM grades")
    suspend fun deleteAll()

    /**
     * 批量插入成绩记录。冲突时替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GradeEntity>)

    /**
     * 全量刷新成绩数据：在事务中先删除全部旧记录，再插入新记录。
     * 确保数据一致性，避免重复或残留的旧数据。
     */
    @Transaction
    suspend fun replaceAll(items: List<GradeEntity>) {
        deleteAll()
        insertAll(items)
    }
}
