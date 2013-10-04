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

public class QBuffer<E> {

    public enum CommitMode {
        LAZY_SET_MIX, SET, LAZY_SET;
    }

    private final int capacity;
    private final int batchSize;

    private final QBufferProducer<E> producer;
    private final QBufferConsumer<E> consumer;

    @SuppressWarnings("unchecked")
    public QBuffer(final int capacity, final int batchSize) {
        this.capacity = 1 << (32 - Integer.numberOfLeadingZeros(capacity - 1));
        this.batchSize = Math.min(batchSize, this.capacity);

        final E[] data = (E[]) new Object[this.capacity];
        final AtomicLong head = new AtomicLong();
        final AtomicLong tail = new AtomicLong();

        producer = new QBufferProducer(this.capacity, this.batchSize, data, head, tail);
        consumer = new QBufferConsumer(this.capacity, this.batchSize, data, tail, head);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public QBufferProducer<E> producer() {
        return producer;
    }

    public QBufferConsumer<E> consumer() {
        return consumer;
    }
}
