/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.agent.status;

/**
 * STATUS
 * <p/>
 * <p/>
 * <p/>
 * Author: gary
 * Date: Jul 7, 2008
 * Time: 1:42:48 PM
 */
public enum StatusType
{
	UNINITIALIZED,
	INITIALIZATION_SUCCESS,
	INITIALIZATION_FAILURE,
	READ_SUCCESS,
	READ_FAILURE,
	READ_NULL,
	TIMEOUT
}
