/*
 * Copyright (c) 2013 Mark D. Horton
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABIL-
 * ITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nostromo.qbuffer;

import java.util.concurrent.atomic.AtomicLong;

public class QBuffer5<E> {

    private final int capacity;
    private final int commitSize;
    private final int mask;
    private final E[] data;

    private final AtomicLong head = new AtomicLong();
    private final AtomicLong tail = new AtomicLong();

    private long adds;
    private long removes;

    private long addsCapacity;
    private long removesCapacity;

    private long addsLastCommit;
    private long removesLastCommit;

    @SuppressWarnings("unchecked")
    public QBuffer5(final int capacity, final int commitSize) {
        this.capacity = 1 << (32 - Integer.numberOfLeadingZeros(capacity - 1));
        this.commitSize = Math.min(commitSize, this.capacity);
        mask = this.capacity - 1;
        data = (E[]) new Object[this.capacity];
    }

    public long beginAdds() {
        if (addsCapacity == 0) addsCapacity = capacity - (adds - head.get());
        return Math.min(commitSize, addsCapacity);
    }

    public void add(final E e) {
        data[(int) (adds++ & mask)] = e;
    }

    public void commitAdds2() {
        tail.set(adds);
    }

    public long commitAdds() {
        if (addsLastCommit == adds) return 0;
        final long delta = adds - tail.get();
        addsLastCommit = adds;
        addsCapacity -= delta;
        tail.set(adds);
        return delta;
    }

    public long lazyCommitAdds() {
        final long delta = adds - tail.get();
        addsCapacity -= delta;
        if (addsCapacity == 0) tail.set(adds);
        else tail.lazySet(adds);
        return delta;
    }

    public long lazyLazyCommitAdds() {
        final long delta = adds - tail.get();
        addsCapacity -= delta;
        tail.lazySet(adds);
        return delta;
    }

    public long beginRemoves() {
        if (removesCapacity == 0) removesCapacity = tail.get() - removes;
        return Math.min(commitSize, removesCapacity);
    }

    public E remove() {
        return data[(int) (removes++ & mask)];
    }

    public void commitRemoves2() {
        head.set(removes);
    }

    public long commitRemoves() {
        if (removesLastCommit == removes) return 0;
        final long delta = removes - head.get();
        removesLastCommit = removes;
        removesCapacity -= delta;
        head.set(removes);
        return delta;
    }

    public long lazyCommitRemoves() {
        final long delta = removes - head.get();
        removesCapacity -= delta;
        if (removesCapacity == 0) head.set(removes);
        else head.lazySet(removes);
        return delta;
    }

    public long lazyLazyCommitRemoves() {
        final long delta = removes - head.get();
        removesCapacity -= delta;
        head.lazySet(removes);
        return delta;
    }
}
