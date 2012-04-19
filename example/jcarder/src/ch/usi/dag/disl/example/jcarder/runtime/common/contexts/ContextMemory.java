/*
 * JCarder -- cards Java programs to keep threads disentangled
 *
 * Copyright (C) 2006-2007 Enea AB
 * Copyright (C) 2007 Ulrik Svensson
 * Copyright (C) 2007 Joel Rosdahl
 *
 * This program is made available under the GNU GPL version 2, with a special
 * exception for linking with JUnit. See the accompanying file LICENSE.txt for
 * details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 */

package ch.usi.dag.disl.example.jcarder.runtime.common.contexts;

import java.util.LinkedList;

import ch.usi.dag.disl.example.jcarder.runtime.common.Lock;
import ch.usi.dag.disl.example.jcarder.runtime.common.LockingContext;


public final class ContextMemory
implements ContextWriterIfc, ContextReaderIfc {

    private final LinkedList<Lock> mLocks = new LinkedList<Lock>();
    private final LinkedList<LockingContext> mLockingContexts =
        new LinkedList<LockingContext>();

    public int writeLock(Lock lock) {
        mLocks.addLast(lock);
        return mLocks.size() - 1;
    }

    public int writeContext(LockingContext context) {
        mLockingContexts.addLast(context);
        return mLockingContexts.size() - 1;
    }

    public Lock readLock(int id) {
        return mLocks.get(id);
    }

    public LockingContext readContext(int id) {
        return mLockingContexts.get(id);
    }
}
