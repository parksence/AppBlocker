package com.hjpark.appblocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    /** 실시간 활성 차단 목록(스위치 ON인 패키지). 서비스·접근성은 매 평가 시 이 쿼리로 동기화한다. */
    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    fun observeBlockedApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    suspend fun getBlockedApps(): List<BlockedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: BlockedApp)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
