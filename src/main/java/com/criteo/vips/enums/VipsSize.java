/*
  Copyright (c) 2020 Criteo
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
      http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package com.criteo.vips.enums;

import java.util.HashMap;
import java.util.Map;

public enum VipsSize {
    // size both up and down
    Both(0),
    // only upsize
    Up(1),
    // only downsize
    Down(2),
    // force size, that is, break aspect ratio
    Force(3),
    Last(4);

    private int value;
    private static Map map = new HashMap<VipsSize, Integer>();

    private VipsSize(int i) {
      value = i;
    }

    static {
        for (VipsSize e : VipsSize.values()) {
            map.put(e.value, e);
        }
    }

    public static VipsSize valueOf(int i) {
        return (VipsSize) map.get(i);
    }

    public int getValue() {
      return value;
    }
}
