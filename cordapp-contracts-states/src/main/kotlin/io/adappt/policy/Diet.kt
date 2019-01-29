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
 * Basic measures of regular diet.
 * @param vegetables in cups per day
 * @param fruits in cups per day
 * @param meat in grams per day
 */
@CordaSerializable
data class Diet(val vegetables: Int = 0,
                val fruits: Int = 0,
                val meat: Int = 0,
                val carbohydrates: Int = 0,
                val protein: Int = 0,
                val fat: Int = 0,
                val alcohol: Int = 0,
                val caffeine: Int = 0,
                val wellness: Int = 0,
                val fish: Int = 0)