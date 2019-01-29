/**
 *   Copyright 2019, Dapps Incorporated.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.adappt.policy

import net.corda.core.serialization.CordaSerializable

/**
 * @param firstName
 * @param middleName
 * @param lastName
 * @param sex male, female or intersex
 * @param age in years
 * @param height in centimeters
 * @param weight in kilograms
 * @param heartRate average rate in beats per minute
 */
@CordaSerializable
data class Basics(val firstName: String,
                  val middleName: String,
                  val lastName: String,
                  val sex: Sex,
                  val age: Int,
                  val height: Int = 0,
                  val weight: Int = 0,
                  val heartRate: Int = 0,
                  val title: String,
                  val minimumLives: Int,
                  val maximumAge: Int)