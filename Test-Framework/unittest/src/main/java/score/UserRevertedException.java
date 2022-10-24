/*
 * Copyright 2022 ICONLOOP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package score;

public class UserRevertedException extends RevertedException {
    // private static final int Start = 32;
    // private static final int End = 1000 - Start;

    private int statusCode;

    public UserRevertedException() {
        super();
    }

    public UserRevertedException(String message) {
        super(message);
    }

    public UserRevertedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserRevertedException(Throwable cause) {
        super(cause);
    }

    public UserRevertedException(int code) {
        super();
        statusCode = code;
    }

    public UserRevertedException(int code, String message) {
        super(message);
        statusCode = code;
    }

    public UserRevertedException(int code, String message, Throwable cause) {
        super(message, cause);
        statusCode = code;
    }

    public UserRevertedException(int code, Throwable cause) {
        super(cause);
        statusCode = code;
    }

    public int getCode() {
        return statusCode;
    }
}
