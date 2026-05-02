package com.students.uniflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.students.uniflow.data.local.dao.ConceptDao
import com.students.uniflow.data.local.dao.ExamTopicDao
import com.students.uniflow.data.local.dao.NoteDao
import com.students.uniflow.data.local.dao.StudyLogDao
import com.students.uniflow.data.local.dao.StudyPlanDao
import com.students.uniflow.data.local.dao.TimetableDao
import com.students.uniflow.data.local.entity.ConceptEntity
import com.students.uniflow.data.local.entity.ExamTopicEntity
import com.students.uniflow.data.local.entity.NoteEntity
import com.students.uniflow.data.local.entity.StudyLogEntity
import com.students.uniflow.data.local.entity.StudyPlanEntity
import com.students.uniflow.data.local.entity.TimetableEntity

@Database(
    entities = [
        NoteEntity::class,
        TimetableEntity::class,
        ExamTopicEntity::class,
        StudyPlanEntity::class,
        StudyLogEntity::class,
        ConceptEntity::class      // NEW in version 6
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun timetableDao(): TimetableDao
    abstract fun examTopicDao(): ExamTopicDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun studyLogDao(): StudyLogDao
    abstract fun conceptDao(): ConceptDao    // NEW

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uniflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}