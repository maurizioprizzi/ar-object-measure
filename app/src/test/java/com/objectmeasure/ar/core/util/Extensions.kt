package com.objectmeasure.ar.core.util

/**
 * Extensões utilitárias básicas - DIA 1
 * Começamos simples para validar nossa estrutura
 */

/**
 * Verifica se um texto não é nulo nem vazio
 */
fun String?.isNotNullOrEmpty(): Boolean {
    return !this.isNullOrEmpty()
}

/**
 * Valida se um número está dentro de um range
 */
fun Float.isInRange(min: Float, max: Float): Boolean {
    return this in min..max
}