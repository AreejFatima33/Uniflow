package com.students.uniflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.students.uniflow.data.local.dao.*
import com.students.uniflow.data.local.entity.*

@Database(
    entities = [
        NoteEntity::class,
        TimetableEntity::class,
        ExamTopicEntity::class,
        StudyPlanEntity::class,
        StudyLogEntity::class,
        ConceptEntity::class,
        GeminiCacheEntity::class,
        GradeEntity::class,
        ReminderEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun timetableDao(): TimetableDao
    abstract fun examTopicDao(): ExamTopicDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun studyLogDao(): StudyLogDao
    abstract fun conceptDao(): ConceptDao
    abstract fun geminiCacheDao(): GeminiCacheDao
    abstract fun gradeDao(): GradeDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uniflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}