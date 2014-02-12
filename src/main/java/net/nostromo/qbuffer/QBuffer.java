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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QBuffer is a lock-free high performance single producer single consumer queue.
 * <p>
 * It's backed by an array that acts as a circular buffer.  Two AtomicLong variables act as memory barriers for the head
 * and tail of the queue.
 * <p>
 * Items are typically added in batches.  This along with the lock-free algorithm provides extremely high throughput.
 *
 * @param <E> the type of items held in this queue
 */
public class QBuffer<E> {

    private final QBufferProducer<E> producer;
    private final QBufferConsumer<E> consumer;

    /**
     * Constructs a QBuffer with the given (fixed) capacity and batch size.
     *
     * @param capacity the maximum capacity of the queue
     * @param batchSize the maximum number of items that can be added or removed from the queue at one time
     */
    @SuppressWarnings("unchecked")
    public QBuffer(final int capacity, final int batchSize) {
        // data.length must be a power of 2
        final E[] data = (E[]) new Object[nextPowerOf2(capacity)];

        final AtomicLong head = new AtomicLong();
        final AtomicLong tail = new AtomicLong();
        final AtomicBoolean active = new AtomicBoolean(true);

        // batchSize can't be greater than data.length
        final int actualBatchSize = Math.min(batchSize, data.length);

        producer = new QBufferProducer<>(data, head, tail, active, actualBatchSize);
        consumer = new QBufferConsumer<>(data, tail, head, active, actualBatchSize);
    }

    /**
     * Returns the Producer side object of the queue.
     *
     * @return the QBufferProducer for this queue
     */
    public QBufferProducer<E> producer() {
        return producer;
    }

    /**
     * Returns the Consumer side object of the queue.
     *
     * @return the QBufferConsumer object or this queue
     */
    public QBufferConsumer<E> consumer() {
        return consumer;
    }

    /**
     * Computes and returns the next power of 2 for the given integer.  If the given integer is a power of 2 then that
     * value is returned.
     * <p>
     * This is a simple performance enhancement to allow the use of a bitmask instead of using modulo to determine an
     * array index.
     *
     * @param value the integer value used to compute the next power of 2
     *
     * @return the next power of 2
     */
    private static int nextPowerOf2(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}
