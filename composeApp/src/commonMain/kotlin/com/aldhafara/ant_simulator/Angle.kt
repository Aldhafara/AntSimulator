package com.aldhafara.ant_simulator

class Angle(private val degrees: Float) {
    val normalizedValue: Float get() = degrees.mod(360.0).toFloat()

    operator fun plus(other: Float) = Angle(this.degrees + other)
    operator fun plus(other: Angle) = Angle(this.degrees + other.degrees)
    operator fun minus(other: Float) = Angle(this.degrees - other)
    operator fun minus(other: Angle) = Angle(this.degrees - other.degrees)
    fun getValue() = this.degrees

    override fun toString() = "${normalizedValue}°"
}