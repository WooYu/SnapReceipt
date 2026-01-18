package com.snapreceipt.io.data.base

interface BaseMapper<I, O> {
    fun map(input: I): O

    fun mapList(input: List<I>): List<O> = input.map { map(it) }
}
