package com.jin.subwayalarm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jin.subwayalarm.data.local.dao.FavoriteRouteDao
import com.jin.subwayalarm.data.local.dao.JourneyPlanDao
import com.jin.subwayalarm.data.local.dao.LineMetaDao
import com.jin.subwayalarm.data.local.dao.StationDao
import com.jin.subwayalarm.data.local.dao.TripSessionDao
import com.jin.subwayalarm.data.local.entity.FavoriteRouteEntity
import com.jin.subwayalarm.data.local.entity.JourneyPlanEntity
import com.jin.subwayalarm.data.local.entity.LineMetaEntity
import com.jin.subwayalarm.data.local.entity.StationEntity
import com.jin.subwayalarm.data.local.entity.TripSessionEntity

@Database(
    entities = [
        StationEntity::class,
        LineMetaEntity::class,
        TripSessionEntity::class,
        JourneyPlanEntity::class,
        FavoriteRouteEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class SubwayDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun lineMetaDao(): LineMetaDao
    abstract fun tripSessionDao(): TripSessionDao
    abstract fun journeyPlanDao(): JourneyPlanDao
    abstract fun favoriteRouteDao(): FavoriteRouteDao

    companion object {
        const val NAME = "subway.db"

        /**
         * 즐겨찾기 테이블 추가.
         *
         * ⚠️ 여기서부터는 파괴적 마이그레이션을 쓰면 **사용자가 저장한 즐겨찾기가 날아간다.**
         * 역·노선 시드는 지워져도 재시드되지만 즐겨찾기는 복구할 방법이 없으므로,
         * 앞으로 스키마를 바꿀 때도 이렇게 실제 마이그레이션을 붙일 것.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorite_route` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`originName` TEXT NOT NULL, " +
                        "`destinationName` TEXT NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL, " +
                        "`lastUsedAtEpochMillis` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_favorite_route_originName_destinationName` " +
                        "ON `favorite_route` (`originName`, `destinationName`)",
                )
            }
        }
    }
}
