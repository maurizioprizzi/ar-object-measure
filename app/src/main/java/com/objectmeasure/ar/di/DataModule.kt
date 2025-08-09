package com.objectmeasure.ar.di

import com.objectmeasure.ar.data.repository.ObjectRepositoryImpl
import com.objectmeasure.ar.domain.repository.ObjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para configurar injeção de dependência
 * DIA 3: Conecta domain interfaces com data implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * Conecta interface ObjectRepository com implementação ObjectRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindObjectRepository(
        objectRepositoryImpl: ObjectRepositoryImpl
    ): ObjectRepository
}