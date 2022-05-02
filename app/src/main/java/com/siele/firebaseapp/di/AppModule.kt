package com.siele.firebaseapp.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.siele.firebaseapp.utils.Constants.UPLOAD_REF
import com.siele.firebaseapp.utils.Constants.UPVOTES_REF
import com.siele.firebaseapp.utils.Constants.VIEWS_REF
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase() = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage() = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideUploadStorageRef(firebaseStorage: FirebaseStorage) = firebaseStorage.reference


    @Provides
    @Singleton
    @UploadsQualifier
    fun provideUploadDatabaseRef(firebaseDatabase: FirebaseDatabase): DatabaseReference {
        return firebaseDatabase.getReference(UPLOAD_REF)
    }
    @Provides
    @Singleton
    @ViewsQualifier
    fun provideViewsDatabaseRef(): DatabaseReference {
        return FirebaseDatabase.getInstance().getReference(VIEWS_REF)
    }



    @Provides
    @Singleton
    @UpvotesQualifier
    fun provideUpvotesDatabaseRef(): DatabaseReference {
        return FirebaseDatabase.getInstance().getReference(UPVOTES_REF)
    }

    @Qualifier
    annotation class ViewsQualifier

    @Qualifier
    annotation class UploadsQualifier

    @Qualifier
    annotation class UpvotesQualifier

}