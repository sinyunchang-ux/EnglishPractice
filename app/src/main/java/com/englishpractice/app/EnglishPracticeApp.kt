package com.englishpractice.app

import android.app.Application
import com.englishpractice.app.data.AppDatabase

class EnglishPracticeApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
