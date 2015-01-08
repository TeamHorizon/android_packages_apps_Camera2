/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.async;

import javax.annotation.Nonnull;

/**
 * This provides empty, NoOp implementation of generic updateable
 * objects.
 */
public final class NoOp<T> implements Updatable<T> {
    private static final NoOp<Object> INSTANCE = new NoOp<>();

    @SuppressWarnings("unchecked")
    public static <T> Updatable<T> ofUpdateable() {
        return (Updatable<T>) INSTANCE;
    }

    private NoOp() { }

    @Override
    public void update(@Nonnull T t) { }
}