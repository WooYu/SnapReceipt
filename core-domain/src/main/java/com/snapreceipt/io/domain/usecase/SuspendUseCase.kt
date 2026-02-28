package com.snapreceipt.io.domain.usecase

/**
 * Base contract for a suspend use case with input parameters.
 *
 * @param P Input parameter type. Use [Unit] for parameterless use cases.
 * @param R Return type.
 */
fun interface SuspendUseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
}

/**
 * Convenience alias for use cases that take no parameters.
 */
fun interface NoParamSuspendUseCase<out R> {
    suspend operator fun invoke(): R
}